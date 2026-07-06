package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.CashPaymentAllowDto;
import com.krunity.HostelManagment.enums.CashPaymentMethod;
import com.krunity.HostelManagment.exception.ConflictException;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cash-payment OTP engine.
 *
 * <p>An OTP ({@link CashPaymentOtp}) is decoupled from payment type. Each request it
 * authorises is recorded as a {@link PaymentOtp} row pointing at the owner+method
 * config ({@link CashPaymentAllow}). This makes the system extensible (new method =
 * new enum value, no schema change) and lets one OTP cover many requests (pay all).</p>
 */
@Slf4j
@Service
public class CashPaymentOtpService {

    @Autowired private CashPaymentOtpRepository otpRepository;
    @Autowired private CashPaymentAllowRepository allowRepository;
    @Autowired private PaymentOtpRepository paymentOtpRepository;

    @Autowired private AgreementRepository agreementRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private PaymentRequestScheduleRepository scheduleRepository;
    @Autowired private ElectricityBillRepository electricityBillRepository;
    @Autowired private OtherChargeRepository otherChargeRepository;
    @Autowired private SettlementRequestRepository settlementRequestRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private NotificationService notificationService;

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    // ── Owner cash-payment settings ────────────────────────────────────────────

    /** Seeds one config row per method for a new owner (configurable ones disabled). */
    @Transactional
    public void seedDefaultsForOwner(UUID ownerId) {
        for (CashPaymentMethod method : CashPaymentMethod.values()) {
            getOrCreateConfig(ownerId, method);
        }
    }

    /** Returns the owner's togglable cash-payment methods for the Settings screen. */
    @Transactional
    public List<CashPaymentAllowDto> getOwnerSettings(UUID ownerId) {
        return CashPaymentMethod.configurableMethods().stream()
                .map(method -> {
                    CashPaymentAllow cfg = getOrCreateConfig(ownerId, method);
                    return CashPaymentAllowDto.builder()
                            .method(method.name())
                            .displayName(method.getDisplayName())
                            .isAllowed(Boolean.TRUE.equals(cfg.getIsAllowed()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public CashPaymentAllowDto updateOwnerSetting(UUID ownerId, String methodName, boolean isAllowed) {
        CashPaymentMethod method;
        try {
            method = CashPaymentMethod.valueOf(methodName);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Unknown cash payment method: " + methodName);
        }
        if (!method.isOwnerConfigurable()) {
            throw new ConflictException(method.getDisplayName() + " cannot be configured");
        }
        CashPaymentAllow cfg = getOrCreateConfig(ownerId, method);
        cfg.setIsAllowed(isAllowed);
        allowRepository.save(cfg);
        return CashPaymentAllowDto.builder()
                .method(method.name())
                .displayName(method.getDisplayName())
                .isAllowed(isAllowed)
                .build();
    }

    private CashPaymentAllow getOrCreateConfig(UUID ownerId, CashPaymentMethod method) {
        return allowRepository.findByOwnerIdAndMethodName(ownerId, method.name())
                .orElseGet(() -> allowRepository.save(CashPaymentAllow.builder()
                        .ownerId(ownerId)
                        .methodName(method.name())
                        // Non-configurable (core) flows are allowed by default.
                        .isAllowed(!method.isOwnerConfigurable())
                        .build()));
    }

    // ── Generic OTP engine ─────────────────────────────────────────────────────

    /**
     * Generates one OTP authorising every {@code requestId} for the given owner+method,
     * sends it to the owner, and returns a masked confirmation message. Passing several
     * request ids enables a single-OTP "pay all".
     */
    @Transactional
    public String generateAndSend(User owner, CashPaymentMethod method, List<String> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            throw new ConflictException("No payment requests provided for OTP");
        }

        CashPaymentAllow config = getOrCreateConfig(owner.getUserId(), method);
        if (method.isOwnerConfigurable() && !Boolean.TRUE.equals(config.getIsAllowed())) {
            throw new ConflictException("Owner has not enabled cash payments for " + method.getDisplayName());
        }

        // Invalidate any outstanding authorisations for these requests to avoid stale OTPs.
        for (String requestId : requestIds) {
            List<PaymentOtp> outstanding = paymentOtpRepository.findByRequestIdAndUsedFalseOrderByCreatedAtDesc(requestId);
            for (PaymentOtp po : outstanding) {
                po.setUsed(true);
            }
            paymentOtpRepository.saveAll(outstanding);
        }

        String otp = generateOtp();
        CashPaymentOtp otpRow = otpRepository.save(CashPaymentOtp.builder()
                .otpHash(passwordEncoder.encode(otp))
                .ownerPhone(owner.getPhoneNumber())
                .expiryTime(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .build());

        for (String requestId : requestIds) {
            paymentOtpRepository.save(PaymentOtp.builder()
                    .otpId(otpRow.getOtpId())
                    .cpId(config.getCpId())
                    .requestId(requestId)
                    .used(false)
                    .build());
        }

        notificationService.sendCashPaymentOtp(owner, otp);
        log.info("Sent {} OTP authorising {} request(s) to owner {}", method, requestIds.size(), owner.getUserId());
        return "OTP sent to owner's mobile number ending with ******" + maskPhoneNumber(owner.getPhoneNumber());
    }

    /**
     * Verifies an OTP for a single request and consumes that authorisation. The same
     * OTP can still be used to verify the other requests it was issued for.
     */
    @Transactional
    public boolean verify(CashPaymentMethod method, String requestId, String otp) {
        Instant now = Instant.now();
        List<PaymentOtp> candidates = paymentOtpRepository.findByRequestIdAndUsedFalseOrderByCreatedAtDesc(requestId);

        for (PaymentOtp po : candidates) {
            CashPaymentOtp otpRow = otpRepository.findById(po.getOtpId()).orElse(null);
            if (otpRow == null || otpRow.getExpiryTime().isBefore(now)) {
                continue; // expired or missing
            }
            CashPaymentAllow cfg = allowRepository.findById(po.getCpId()).orElse(null);
            if (cfg == null || !cfg.getMethodName().equals(method.name())) {
                continue; // wrong method
            }
            if (passwordEncoder.matches(otp, otpRow.getOtpHash())) {
                po.setUsed(true);
                paymentOtpRepository.save(po);
                log.info("Verified {} OTP for request {}", method, requestId);
                return true;
            }
        }
        log.warn("No valid {} OTP for request {}", method, requestId);
        return false;
    }

    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteByExpiryTimeBefore(Instant.now());
    }

    // ── Per-method wrappers (preserve existing call sites) ──────────────────────

    @Transactional
    public String generateAndSendOtp(String agreementId) {
        return generateAndSend(ownerForAgreement(agreementId), CashPaymentMethod.AGREEMENT, List.of(agreementId));
    }

    @Transactional
    public boolean verifyOtp(String agreementId, String otp) {
        return verify(CashPaymentMethod.AGREEMENT, agreementId, otp);
    }

    @Transactional
    public String generateAndSendInstallmentOtp(String scheduleIdStr) {
        UUID scheduleId = UUID.fromString(scheduleIdStr);
        PaymentRequestSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Payment schedule not found"));
        String agreementId = schedule.getTenantPaymentPlan().getAgreementId();
        return generateAndSend(ownerForAgreement(agreementId), CashPaymentMethod.INSTALLMENT,
                List.of(scheduleId.toString()));
    }

    @Transactional
    public boolean verifyInstallmentOtp(UUID scheduleId, String otp) {
        return verify(CashPaymentMethod.INSTALLMENT, scheduleId.toString(), otp);
    }

    @Transactional
    public String generateAndSendOtherChargeOtp(String chargeId) {
        OtherCharge charge = otherChargeRepository.findById(UUID.fromString(chargeId))
                .orElseThrow(() -> new NotFoundException("Other charge not found"));
        return generateAndSend(charge.getOwner(), CashPaymentMethod.OTHER_CHARGE, List.of(chargeId));
    }

    @Transactional
    public boolean verifyOtherChargeOtp(String chargeId, String otp) {
        return verify(CashPaymentMethod.OTHER_CHARGE, chargeId, otp);
    }

    @Transactional
    public String generateAndSendElectricityOtp(String billIdStr) {
        UUID billId = UUID.fromString(billIdStr);
        ElectricityBill bill = electricityBillRepository.findById(billId)
                .orElseThrow(() -> new NotFoundException("Electricity bill not found"));
        Room room = roomRepository.findById(bill.getRoomId())
                .orElseThrow(() -> new NotFoundException("Room not found"));
        return generateAndSend(room.getHostel().getOwner(), CashPaymentMethod.ELECTRICITY_BILL,
                List.of(billId.toString()));
    }

    @Transactional
    public boolean verifyElectricityOtp(UUID billId, String otp) {
        return verify(CashPaymentMethod.ELECTRICITY_BILL, billId.toString(), otp);
    }

    @Transactional
    public String generateAndSendSettlementOtp(String settlementId) {
        SettlementRequest settlement = settlementRequestRepository.findById(UUID.fromString(settlementId))
                .orElseThrow(() -> new NotFoundException("Settlement not found"));
        return generateAndSend(settlement.getOwner(), CashPaymentMethod.SETTLEMENT, List.of(settlementId));
    }

    @Transactional
    public boolean verifySettlementOtp(String settlementId, String otp) {
        return verify(CashPaymentMethod.SETTLEMENT, settlementId, otp);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private User ownerForAgreement(String agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new NotFoundException("Agreement not found"));
        Room room = roomRepository.findById(agreement.getRoomId())
                .orElseThrow(() -> new NotFoundException("Room not found"));
        return room.getHostel().getOwner();
    }

    private String generateOtp() {
        return String.valueOf(100000 + RANDOM.nextInt(900000)); // 6-digit
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(phoneNumber.length() - 4);
    }
}
