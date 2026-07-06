package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.AcceptAgreementRequest;
import com.krunity.HostelManagment.dto.CreateFlatAgreementRequest;
import com.krunity.HostelManagment.dto.QrActivationResponse;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.enums.AgreementStatus;
import com.krunity.HostelManagment.enums.AgreementType;
import com.krunity.HostelManagment.enums.RoomType;
import com.krunity.HostelManagment.repository.AgreementRepository;
import com.krunity.HostelManagment.enums.PaymentFrequency;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.enums.TransactionMode;
import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.exception.ConflictException;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AgreementService {
    
    @Autowired
    private AgreementRepository agreementRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private TenantPaymentPlanRepository paymentPlanRepository;

    @Autowired
    private PaymentTypeRepository paymentTypeRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RoomAllotmentRepository roomAllotmentRepository;
    
    @Autowired
    private PaymentCalculationService paymentCalculationService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PaymentScheduleService paymentScheduleService;
    
    @Autowired
    private CashPaymentOtpService cashPaymentOtpService;

    @Autowired
    private com.krunity.HostelManagment.service.PaymentService paymentService;

    @Autowired
    private RoomAgreementPlanService roomAgreementPlanService;

    @Autowired
    private RoomAvailabilityService roomAvailabilityService;
    
    private static final int QR_TOKEN_EXPIRY_HOURS = 72; // 3 days
    
    @Transactional
    public Agreement createAgreement(Agreement agreement) {
        // Validate user exists
        User user = userRepository.findById(agreement.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + agreement.getUserId()));
        
        // Validate room exists and has availability for the agreement start/end dates
        if (agreement.getRoomId() != null
                && (agreement.getType() == AgreementType.ROOM || agreement.getType() == AgreementType.FLAT)) {
            roomRepository.findById(agreement.getRoomId())
                    .orElseThrow(() -> new NotFoundException("Room not found with ID: " + agreement.getRoomId()));
            LocalDate startDate = agreement.getStartDate() != null ? agreement.getStartDate() : LocalDate.now();
            LocalDate endDate = agreement.getEndDate() != null ? agreement.getEndDate() : startDate.plusYears(1);
            roomAvailabilityService.validateRoomHasBeds(agreement.getRoomId(), startDate, endDate, 1);
        }

        // Stamp the currently logged-in owner on the agreement
        User owner = com.krunity.HostelManagment.Utils.ApplicationContext.getUser();
        if (owner != null) {
            agreement.setOwnerId(owner.getUserId());
        }

        // Set status and timestamps
        agreement.setStatus(AgreementStatus.PENDING_TENANT_ACTION);
        agreement.setCreatedAt(Instant.now());
        agreement.setQrUsed(false);
        
        // Generate QR token
        String qrToken = generateQrToken();
        agreement.setQrToken(qrToken);
        agreement.setQrExpiry(Instant.now().plus(QR_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS));
        
        Agreement savedAgreement = agreementRepository.save(agreement);

        // Resolve hostel and room info for SMS context
        String hostelName = "N/A";
        String roomNumber = "N/A";
        if (agreement.getType() == AgreementType.ROOM && agreement.getRoomId() != null) {
            try {
                Room room = roomRepository.findById(agreement.getRoomId()).orElse(null);
                if (room != null) {
                    roomNumber = room.getRoomNumber();
                    if (room.getHostel() != null) {
                        hostelName = room.getHostel().getHostelName();
                    }
                }
            } catch (Exception ignored) { }
        }

        // Send notifications to tenant (email + SMS with activation link)
        notificationService.sendQrActivationEmail(savedAgreement, user);
        notificationService.sendQrActivationSms(savedAgreement, user, hostelName, roomNumber);
        
        return savedAgreement;
    }

    /**
     * Creates a new Flat agreement.
     * <p>
     * Validates that the referenced room has {@code roomType=FLAT} and the referenced plan has
     * {@code planType=FLAT}, then persists an {@link Agreement} document with
     * {@code type=FLAT} and the supplied co-tenant names. Returns a {@link QrActivationResponse}
     * containing the new agreement ID and QR token so the owner can share the activation link.
     *
     * @param request validated flat-agreement creation request
     * @return QR activation response with agreementId and qrToken
     * @throws IllegalArgumentException if the room is not a FLAT room or the plan is not a FLAT plan
     * @throws NotFoundException        if the room, plan, or user cannot be found
     */
    @Transactional
    public QrActivationResponse createFlatAgreement(CreateFlatAgreementRequest request) {
        // --- Validate room exists and is of type FLAT ---
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new NotFoundException("Room not found with ID: " + request.getRoomId()));
        if (room.getRoomType() != RoomType.FLAT) {
            throw new IllegalArgumentException(
                    "Room " + request.getRoomId() + " is not a FLAT room. Only FLAT rooms can be used for flat agreements.");
        }

        // --- Validate co-tenant count based on date-aware availability ---
        List<String> coTenantNames = request.getCoTenantNames() != null
                ? request.getCoTenantNames()
                : new ArrayList<>();

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : startDate.plusYears(1);
        int bedsRequired = 1 + coTenantNames.size();
        int availableBeds = roomAvailabilityService.getAvailableBeds(request.getRoomId(), startDate, endDate);

        if (availableBeds < bedsRequired) {
            throw new IllegalArgumentException(String.format(
                    "Room capacity exceeded from %s to %s. Available beds: %d, required: %d (1 primary + %d co-tenants).",
                    startDate, endDate, availableBeds, bedsRequired, coTenantNames.size()));
        }

        int maxCoTenants = availableBeds - 1;
        if (coTenantNames.size() > maxCoTenants) {
            throw new IllegalArgumentException(String.format(
                    "Maximum %d co-tenants allowed for this room from %s to %s (%d beds available, 1 reserved for primary tenant). Provided: %d co-tenants.",
                    maxCoTenants, startDate, endDate, availableBeds, coTenantNames.size()));
        }

        // --- Validate plan exists and is of type FLAT ---
        RoomAgreementPlan plan = roomAgreementPlanService.getPlanById(request.getPlanId());
        if (!"FLAT".equalsIgnoreCase(plan.getPlanType())) {
            throw new IllegalArgumentException(
                    "Plan " + request.getPlanId() + " is not a FLAT plan. Only FLAT plans can be used for flat agreements.");
        }

        // --- Validate user (primary tenant) exists ---
        User tenant = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + request.getUserId()));

        // --- Resolve currently logged-in owner ---
        User owner = com.krunity.HostelManagment.Utils.ApplicationContext.getUser();

        // --- Build the Agreement document ---
        String qrToken = generateQrToken();
        Instant now = Instant.now();

        Agreement agreement = Agreement.builder()
                .type(AgreementType.FLAT)
                .userId(request.getUserId())
                .roomId(request.getRoomId())
                .planId(request.getPlanId())
                .planSnapshot(plan)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .coTenantNames(coTenantNames)
                .ownerId(owner != null ? owner.getUserId() : null)
                .status(AgreementStatus.PENDING_TENANT_ACTION)
                .qrToken(qrToken)
                .qrExpiry(now.plus(QR_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS))
                .qrUsed(false)
                .createdAt(now)
                .build();

        Agreement savedAgreement = agreementRepository.save(agreement);

        // Mark plan as in use
        roomAgreementPlanService.markPlanAsInUse(request.getPlanId());

        // --- Resolve room info for notification context ---
        String hostelName = "N/A";
        String roomNumber = room.getRoomNumber() != null ? room.getRoomNumber() : "N/A";
        if (room.getHostel() != null && room.getHostel().getHostelName() != null) {
            hostelName = room.getHostel().getHostelName();
        }

        // --- Send notifications to primary tenant ---
        notificationService.sendQrActivationEmail(savedAgreement, tenant);
        notificationService.sendQrActivationSms(savedAgreement, tenant, hostelName, roomNumber);

        // --- Build and return QR activation response ---
        QrActivationResponse response = new QrActivationResponse();
        response.setAgreementId(savedAgreement.getId());
        response.setQrToken(savedAgreement.getQrToken());
        response.setExpiry(savedAgreement.getQrExpiry());
        return response;
    }

    public Optional<Agreement> validateQrToken(String token) {
        // Use repository method that checks expiry automatically
        return agreementRepository.findByQrTokenAndQrUsedFalseAndQrExpiryAfter(token, Instant.now());
    }
    
    @Transactional
    public Agreement activateAgreement(String agreementId, AcceptAgreementRequest request) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new NotFoundException("Agreement not found with ID: " + agreementId));
        
        // Validate QR token is not used
        if (agreement.getQrUsed() != null && agreement.getQrUsed()) {
            throw new IllegalStateException("Agreement QR token has already been used");
        }
        
        // Validate token is not expired
        if (agreement.getQrExpiry() != null && agreement.getQrExpiry().isBefore(Instant.now())) {
            throw new IllegalStateException("Agreement QR token has expired");
        }
        
        // Validate status
        if (agreement.getStatus() != AgreementStatus.PENDING_TENANT_ACTION) {
            throw new IllegalStateException("Agreement is not in PENDING_TENANT_ACTION status");
        }
        
        // Verify OTP for cash payment
        if (request.getPaymentMode() == TransactionMode.CASH) {
            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                throw new IllegalStateException("OTP is required for cash payment");
            }
            boolean isOtpValid = cashPaymentOtpService.verifyOtp(agreementId, request.getOtp());
            if (!isOtpValid) {
                throw new IllegalStateException("Invalid or expired OTP");
            }
        }

        // Verify Razorpay signature for online payment
        if (request.getPaymentMode() == TransactionMode.ONLINE) {
            if (request.getRazorpayOrderId() == null || request.getRazorpayPaymentId() == null || request.getRazorpaySignature() == null) {
                throw new IllegalStateException("Razorpay payment details (orderId, paymentId, signature) are required for online payment");
            }
            com.krunity.HostelManagment.dto.VerifyPaymentRequest verifyRequest =
                    new com.krunity.HostelManagment.dto.VerifyPaymentRequest();
            verifyRequest.setOrderId(request.getRazorpayOrderId());
            verifyRequest.setPaymentId(request.getRazorpayPaymentId());
            verifyRequest.setSignature(request.getRazorpaySignature());
            verifyRequest.setAgreementId(agreementId);

            com.krunity.HostelManagment.dto.VerifyPaymentResponse verifyResponse =
                    paymentService.verifyAgreementPayment(verifyRequest);

            if (!verifyResponse.isVerified()) {
                throw new IllegalStateException("Online payment verification failed: " + verifyResponse.getMessage());
            }
        }
        
        // Get user
        User tenant = userRepository.findById(agreement.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        
        // Create payment artifacts
        TenantPaymentPlan paymentPlan = createPaymentPlan(agreement, tenant);
        Transaction transaction = processPayment(agreement, tenant, paymentPlan, request);
        createRoomAllotment(agreement, tenant, paymentPlan, transaction);

        // Generate installment schedule with activation transaction link
        paymentScheduleService.generateSchedule(paymentPlan, agreement.getPlanSnapshot(), transaction);

        // Update agreement
        agreement.setStatus(AgreementStatus.ACTIVE);
        agreement.setQrUsed(true);
        agreement.setActivatedAt(Instant.now());
        
        // Activate user account
        tenant.setActive(true);
        userRepository.save(tenant);
        
        // Generate password reset token for tenant to set password — returned to frontend
        String resetToken = passwordResetService.generatePasswordResetToken(tenant.getUserId());
        
        Agreement savedAgreement = agreementRepository.save(agreement);
        
        // Resolve hostel and room info for SMS context
        String hostelName = "N/A";
        String roomNumber = "N/A";
        if (agreement.getType() == AgreementType.ROOM && agreement.getRoomId() != null) {
            try {
                Room room = roomRepository.findById(agreement.getRoomId()).orElse(null);
                if (room != null) {
                    roomNumber = room.getRoomNumber();
                    if (room.getHostel() != null) {
                        hostelName = room.getHostel().getHostelName();
                    }
                }
            } catch (Exception ignored) { }
        }

        // Notify owner — SMS with tenant name, hostel, room, plan, rent
        User owner = getOwnerForAgreement(agreement);
        notificationService.sendAgreementAcceptedNotification(
                savedAgreement, owner, tenant.getDisplayName(), hostelName, roomNumber);

        // Notify tenant — SMS confirming their agreement is now active
        notificationService.sendAgreementActivatedSmsToTenant(
                savedAgreement, tenant, hostelName, roomNumber);

        // Attach reset token to agreement transiently so controller can pass it to frontend
        savedAgreement.setPasswordResetToken(resetToken);

        return savedAgreement;
    }
    
    @Transactional
    public Agreement rejectAgreement(String agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new NotFoundException("Agreement not found with ID: " + agreementId));
        
        if (agreement.getStatus() != AgreementStatus.PENDING_TENANT_ACTION) {
            throw new IllegalStateException("Agreement cannot be rejected in current status");
        }
        
        agreement.setStatus(AgreementStatus.REJECTED);
        agreement.setQrUsed(true); // Mark QR as used even if rejected
        
        return agreementRepository.save(agreement);
    }
    
    private String generateQrToken() {
        // Generate a secure random token
        return UUID.randomUUID().toString().replace("-", "") + 
               Instant.now().toEpochMilli();
    }
    
    private Transaction processPayment(
            Agreement agreement,
            User tenant,
            TenantPaymentPlan paymentPlan,
            AcceptAgreementRequest request
    ) {
        // Use new payment calculation service
        PaymentCalculationService.PaymentBreakdown breakdown = 
            paymentCalculationService.calculatePaymentBreakdown(agreement.getPlanSnapshot());
        
        // Total activation payment = AT_AGREEMENT charges (deposit + one-time) + first installment
        BigDecimal firstInstallment = BigDecimal.valueOf(paymentPlan.getInstallmentAmount());
        BigDecimal totalAmount = breakdown.getTotalAgreementTime().add(firstInstallment);

        // Fallback to legacy calculation if no plan snapshot
        if (totalAmount.equals(firstInstallment)) {
            totalAmount = resolveBaseRent(agreement)
                    .add(defaultAmount(agreement.getDeposit()))
                    .add(defaultAmount(agreement.getCleaningCharges()))
                    .add(firstInstallment);
        }
        
        // Get owner (assuming first user with OWNER role or from room's hostel owner)
        User owner = getOwnerForAgreement(agreement);

        
        // Create transaction
        Transaction transaction = Transaction.builder()
                .planId(paymentPlan)
                .fromUser(tenant)
                .toUser(owner)
                .amount(totalAmount.longValue())
                .mode(request.getPaymentMode())
                .status(request.getPaymentMode() == TransactionMode.ONLINE 
                        ? TransactionStatus.COMPLETED 
                        : TransactionStatus.COMPLETED)
                .reason("Agreement activation payment")
                .otpVerified(request.getPaymentMode() == TransactionMode.CASH && request.getOtp() != null)
                .build();
        
        return transactionRepository.save(transaction);
    }
    
    private TenantPaymentPlan createPaymentPlan(Agreement agreement, User tenant) {
        // Use new payment calculation service
        PaymentCalculationService.PaymentBreakdown breakdown = 
            paymentCalculationService.calculatePaymentBreakdown(agreement.getPlanSnapshot());
        
        // Create payment plan
        TenantPaymentPlan paymentPlan = new TenantPaymentPlan();
        paymentPlan.setTenant(tenant);
        paymentPlan.setPaymentType(resolvePaymentType(agreement));
        paymentPlan.setTenantPlanId(buildTenantPlanId(agreement, tenant));
        paymentPlan.setAgreementId(agreement.getId());
        paymentPlan.setPaymentFrequency(resolvePaymentFrequency(agreement));
        
        // Use new calculation for deposit amount (refundable amount at agreement time)
        paymentPlan.setDepositAmount(breakdown.getAgreementTimeRefundable().longValue());
        
        // Use new calculation for installment amount
        paymentPlan.setInstallmentAmount(resolveInstallmentAmount(agreement));
        paymentPlan.setStartDate(agreement.getStartDate());
        paymentPlan.setPendingInstallments(resolvePendingInstallments(agreement));
        paymentPlan.setActive(true);
        
        return paymentPlanRepository.save(paymentPlan);
    }

    private void createRoomAllotment(
            Agreement agreement,
            User tenant,
            TenantPaymentPlan paymentPlan,
            Transaction depositTransaction
    ) {
        // WORKER agreements skip allotment creation entirely
        if (agreement.getType() == AgreementType.WORKER || agreement.getRoomId() == null) {
            return;
        }

        if (agreement.getType() == AgreementType.FLAT) {
            // FLAT path: one allotment for the primary tenant only; reject if room already occupied
            Room room = roomRepository.findById(agreement.getRoomId())
                    .orElseThrow(() -> new NotFoundException("Room not found with ID: " + agreement.getRoomId()));

            LocalDate startDate = agreement.getStartDate() != null ? agreement.getStartDate() : LocalDate.now();
            LocalDate endDate = agreement.getEndDate() != null ? agreement.getEndDate() : startDate.plusYears(1);
            roomAvailabilityService.validateRoomHasBeds(room.getRoomId(), startDate, endDate, 1);

            RoomAllotment allotment = RoomAllotment.builder()
                    .room(room)
                    .tenant(tenant)
                    .agreementId(agreement.getId())
                    .paymentPlanId(paymentPlan)
                    .depositTransactionId(depositTransaction)
                    .startDate(startDate)
                    .endDate(agreement.getEndDate())
                    .roomAllotmentStatus(resolveAllotmentStatus(startDate))
                    .noticePeriodMonths(resolveNoticePeriodMonths(agreement.getPlanSnapshot()))
                    .lastStatusChangedBy("SYSTEM")
                    .lastStatusChangedAt(LocalDateTime.now())
                    .build();

            roomAllotmentRepository.save(allotment);
            return;
        }

        // ROOM path (unchanged)
        if (agreement.getType() != AgreementType.ROOM) {
            return;
        }

        if (roomAllotmentRepository.existsByTenant_UserId(tenant.getUserId())) {
            return;
        }

        Room room = roomRepository.findById(agreement.getRoomId())
                .orElseThrow(() -> new NotFoundException("Room not found with ID: " + agreement.getRoomId()));

        LocalDate startDate = agreement.getStartDate() != null ? agreement.getStartDate() : LocalDate.now();
        LocalDate endDate = agreement.getEndDate() != null ? agreement.getEndDate() : startDate.plusYears(1);
        roomAvailabilityService.validateRoomHasBeds(room.getRoomId(), startDate, endDate, 1);

        RoomAllotment allotment = RoomAllotment.builder()
                .room(room)
                .tenant(tenant)
                .agreementId(agreement.getId())
                .paymentPlanId(paymentPlan)
                .depositTransactionId(depositTransaction)
                .startDate(startDate)
                .endDate(agreement.getEndDate())
                .roomAllotmentStatus(resolveAllotmentStatus(startDate))
                .noticePeriodMonths(resolveNoticePeriodMonths(agreement.getPlanSnapshot()))
                .lastStatusChangedBy("SYSTEM")
                .lastStatusChangedAt(LocalDateTime.now())
                .build();

        roomAllotmentRepository.save(allotment);
    }

    /**
     * Extracts the notice period in months from the plan snapshot.
     * The plan stores notice period in days; we convert to whole months (ceil).
     * Returns null if the plan has no cancellation rules.
     */
    private Integer resolveNoticePeriodMonths(com.krunity.HostelManagment.model.RoomAgreementPlan plan) {
        if (plan == null) return null;
        var rules = plan.getAgreementCancellationRules();
        if (rules == null || rules.getTenantCancellation() == null) return null;
        Integer days = rules.getTenantCancellation().getNoticePeriodDays();
        if (days == null || days <= 0) return null;
        return (int) Math.ceil(days / 30.0);
    }

    private RoomAllotmentStatus resolveAllotmentStatus(LocalDate startDate) {
        return startDate.isAfter(LocalDate.now())
                ? RoomAllotmentStatus.UPCOMING
                : RoomAllotmentStatus.ACTIVE;
    }

    private String buildTenantPlanId(Agreement agreement, User tenant) {
        String agreementReference = agreement.getId() != null
                ? agreement.getId()
                : UUID.randomUUID().toString();

        return "TPP-" + tenant.getUserId() + "-" + agreementReference;
    }

    private int resolvePendingInstallments(Agreement agreement) {
        com.krunity.HostelManagment.model.RoomAgreementPlan snap = agreement.getPlanSnapshot();
        if (isNotFixedDuration(snap)) {
            // For NOT_FIXED: generate installments equal to the minimum stay months upfront
            int minStay = (snap.getDuration().getMinimumStayMonths() != null)
                    ? snap.getDuration().getMinimumStayMonths() : 1;
            return Math.max(minStay, 1);
        }
        if (agreement.getPlanSnapshot() != null
                && agreement.getPlanSnapshot().getPaymentModel() != null
                && agreement.getPlanSnapshot().getPaymentModel().getInstallments() != null) {
            return Math.max(agreement.getPlanSnapshot().getPaymentModel().getInstallments(), 1);
        }

        if (agreement.getPlanSnapshot() != null
                && agreement.getPlanSnapshot().getDuration() != null
                && agreement.getPlanSnapshot().getDuration().getValue() != null) {
            Integer durationValue = agreement.getPlanSnapshot().getDuration().getValue();
            return Math.max(durationValue, 1);
        }

        return 12;
    }

    private long resolveInstallmentAmount(Agreement agreement) {
        // Use new payment calculation service
        if (agreement.getPlanSnapshot() != null) {
            PaymentCalculationService.PaymentBreakdown breakdown =
                paymentCalculationService.calculatePaymentBreakdown(agreement.getPlanSnapshot());

            if (breakdown.getInstallmentAmount().compareTo(BigDecimal.ZERO) > 0) {
                int monthsPerInstallment = resolveMonthsPerInstallment(agreement.getPlanSnapshot());
                return breakdown.getInstallmentAmount()
                        .multiply(BigDecimal.valueOf(monthsPerInstallment))
                        .longValue();
            }
        }

        // Fallback to legacy calculation
        if (agreement.getPlanSnapshot() != null
                && agreement.getPlanSnapshot().getRentDetails() != null
                && agreement.getPlanSnapshot().getRentDetails().getMonthlyRent() != null) {
            int monthsPerInstallment = resolveMonthsPerInstallment(agreement.getPlanSnapshot());
            return agreement.getPlanSnapshot().getRentDetails().getMonthlyRent()
                    .multiply(BigDecimal.valueOf(monthsPerInstallment))
                    .longValue();
        }

        if (agreement.getRent() != null) {
            return agreement.getRent().longValue();
        }

        throw new IllegalStateException("Agreement rent is required to create a tenant payment plan");
    }

    private int resolveMonthsPerInstallment(com.krunity.HostelManagment.model.RoomAgreementPlan planSnapshot) {
        if (isNotFixedDuration(planSnapshot)) {
            return 1; // Monthly rolling — always 1 month per installment
        }
        if (planSnapshot.getDuration() != null && planSnapshot.getPaymentModel() != null
                && planSnapshot.getDuration().getValue() != null
                && planSnapshot.getPaymentModel().getInstallments() != null
                && planSnapshot.getPaymentModel().getInstallments() > 0) {
            int totalMonths = planSnapshot.getDuration().getValue();
            int installments = planSnapshot.getPaymentModel().getInstallments();
            return Math.max(1, (int) Math.ceil((double) totalMonths / installments));
        }
        return 1;
    }

    public static boolean isNotFixedDuration(com.krunity.HostelManagment.model.RoomAgreementPlan planSnapshot) {
        if (planSnapshot == null || planSnapshot.getDuration() == null) return false;
        return "NOT_FIXED".equalsIgnoreCase(planSnapshot.getDuration().getDurationType());
    }

    private PaymentFrequency resolvePaymentFrequency(Agreement agreement) {
        if (agreement.getPlanSnapshot() != null
                && agreement.getPlanSnapshot().getPaymentModel() != null
                && agreement.getPlanSnapshot().getPaymentModel().getMode() != null) {
            return PaymentFrequency.valueOf(
                    agreement.getPlanSnapshot().getPaymentModel().getMode().trim().toUpperCase()
            );
        }

        return PaymentFrequency.MONTHLY;
    }

    private PaymentType resolvePaymentType(Agreement agreement) {
        String typeName = resolvePaymentFrequency(agreement).name();
        return paymentTypeRepository.findByTypeNameIgnoreCase(typeName)
                .orElseGet(() -> paymentTypeRepository.save(
                        PaymentType.builder().typeName(typeName).build()
                ));
    }

    private BigDecimal resolveBaseRent(Agreement agreement) {
        if (agreement.getRent() != null) {
            return agreement.getRent();
        }

        if (agreement.getPlanSnapshot() != null
                && agreement.getPlanSnapshot().getRentDetails() != null
                && agreement.getPlanSnapshot().getRentDetails().getMonthlyRent() != null) {
            return agreement.getPlanSnapshot().getRentDetails().getMonthlyRent();
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
    
    public List<Agreement> getAllAgreements() {
        User owner = com.krunity.HostelManagment.Utils.ApplicationContext.getUser();
        if (owner != null) {
            return agreementRepository.findByOwnerId(owner.getUserId());
        }
        return agreementRepository.findAll();
    }
    
    public Optional<Agreement> getAgreementById(String agreementId) {
        User owner = com.krunity.HostelManagment.Utils.ApplicationContext.getUser();
        if (owner != null) {
            // Only return agreement if it belongs to the current owner
            return agreementRepository.findByIdAndOwnerId(agreementId, owner.getUserId());
        }
        return agreementRepository.findById(agreementId);
    }
    
    private User getOwnerForAgreement(Agreement agreement) {
        // If room agreement, get owner from room's hostel
        if (agreement.getType() == AgreementType.ROOM && agreement.getRoomId() != null) {
            Room room = roomRepository.findById(agreement.getRoomId())
                    .orElseThrow(() -> new NotFoundException("Room not found"));
            return room.getHostel().getOwner();
        }
        
        // Otherwise, find first user with OWNER role
        return userRepository.findAll().stream()
                .filter(user -> "OWNER".equalsIgnoreCase(user.getRole().getName()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Owner not found"));
    }
}
