package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.OtherChargeRequest;
import com.krunity.HostelManagment.dto.OtherChargeResponse;
import com.krunity.HostelManagment.enums.ChargeCategory;
import com.krunity.HostelManagment.enums.PaymentStatus;
import com.krunity.HostelManagment.enums.ReminderType;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtherChargeService {

    private final OtherChargeRepository otherChargeRepository;
    private final OtherChargeInstallmentRepository installmentRepository;
    private final OtherChargePaymentRepository otherChargePaymentRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final HostelRepository hostelRepository;
    private final RoomAllotmentRepository roomAllotmentRepository;
    private final NotificationService notificationService;
    private final SmsTemplateService smsTemplateService;
    private final SubscriptionService subscriptionService;

    // A tenant is included in a room (split) charge only if they have occupied the
    // room for at least this many days, mirroring the electricity bill rule.
    private static final long MIN_OCCUPANCY_DAYS = 15;

    @Transactional
    public OtherChargeResponse createOtherCharge(OtherChargeRequest request, UUID ownerId) {
        log.info("Creating other charge: {} for owner: {}", request.getChargeName(), ownerId);

        // Validate request
        if (!request.isValid()) {
            throw new IllegalArgumentException("Invalid charge request: missing tenant or room ID based on category");
        }

        // Fetch entities
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));

        Hostel hostel = hostelRepository.findById(request.getHostelId())
                .orElseThrow(() -> new IllegalArgumentException("Hostel not found"));

        User tenant = null;
        Room room = null;

        if (request.getCategory() == ChargeCategory.OTHER_CHARGE_TENANT) {
            tenant = userRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        } else if (request.getCategory() == ChargeCategory.OTHER_CHARGE_ROOM) {
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        }

        // Calculate installment amount if enabled
        BigDecimal installmentAmount = null;
        if (request.getInstallmentEnabled() && request.getInstallmentCount() != null && request.getInstallmentCount() > 1) {
            installmentAmount = request.getAmount().divide(
                    BigDecimal.valueOf(request.getInstallmentCount()), 
                    2, 
                    RoundingMode.HALF_UP
            );
        }

        // Create the charge
        OtherCharge charge = OtherCharge.builder()
                .chargeName(request.getChargeName())
                .description(request.getDescription())
                .amount(request.getAmount())
                .category(request.getCategory())
                .owner(owner)
                .tenant(tenant)
                .room(room)
                .hostel(hostel)
                .dueDate(request.getDueDate() != null ? request.getDueDate() : LocalDateTime.now().plusDays(7))
                .installmentEnabled(request.getInstallmentEnabled())
                .installmentCount(request.getInstallmentCount())
                .installmentAmount(installmentAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .active(true)
                .build();

        charge = otherChargeRepository.save(charge);

        // Create installments if enabled; otherwise split a room charge into one
        // PENDING share per eligible tenant (full-payment split-among-tenants).
        if (request.getInstallmentEnabled() && request.getInstallmentCount() != null && request.getInstallmentCount() > 1) {
            createInstallments(charge);
        } else if (charge.isRoomBased()) {
            createRoomChargeShares(charge);
        }

        // Send SMS notification to affected tenants if owner has SMS reminders enabled
        sendOtherChargeSmsNotification(charge);

        log.info("Created other charge with ID: {}", charge.getChargeId());
        return mapToResponse(charge);
    }

    @Transactional
    public void createInstallments(OtherCharge charge) {
        if (!charge.getInstallmentEnabled() || charge.getInstallmentCount() == null) {
            return;
        }

        List<User> tenants = getTenantsForCharge(charge);
        if (tenants.isEmpty()) {
            log.warn("No tenants found for charge: {}", charge.getChargeId());
            return;
        }

        BigDecimal totalAmount = charge.getAmount();
        int installmentCount = charge.getInstallmentCount();
        
        for (User tenant : tenants) {
            BigDecimal tenantAmount = charge.isRoomBased() ? 
                    totalAmount.divide(BigDecimal.valueOf(tenants.size()), 2, RoundingMode.HALF_UP) : 
                    totalAmount;

            BigDecimal installmentAmount = tenantAmount.divide(
                    BigDecimal.valueOf(installmentCount), 
                    2, 
                    RoundingMode.HALF_UP
            );

            for (int i = 1; i <= installmentCount; i++) {
                // Adjust last installment to handle rounding differences
                BigDecimal amount = installmentAmount;
                if (i == installmentCount) {
                    BigDecimal totalPaid = installmentAmount.multiply(BigDecimal.valueOf(installmentCount - 1));
                    amount = tenantAmount.subtract(totalPaid);
                }

                OtherChargeInstallment installment = OtherChargeInstallment.builder()
                        .otherCharge(charge)
                        .tenant(tenant)
                        .installmentNumber(i)
                        .amount(amount)
                        .dueDate(charge.getDueDate().plusMonths(i - 1))
                        .paymentStatus(PaymentStatus.PENDING)
                        .build();

                installmentRepository.save(installment);
            }
        }

        log.info("Created {} installments for charge: {}", installmentCount * tenants.size(), charge.getChargeId());
    }

    private List<User> getTenantsForCharge(OtherCharge charge) {
        if (charge.isTenantSpecific()) {
            return List.of(charge.getTenant());
        } else if (charge.isRoomBased()) {
            // Get current tenants in the room
            List<RoomAllotment> allotments = roomAllotmentRepository.findByRoomAndRoomAllotmentStatus(charge.getRoom(), RoomAllotmentStatus.ACTIVE);
            return allotments.stream()
                    .map(RoomAllotment::getTenant)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * Splits a room-based, full-payment charge equally among the room's eligible
     * tenants and persists one PENDING {@link OtherChargePayment} share per tenant.
     * Eligible = currently occupying the room and allotted for at least
     * {@link #MIN_OCCUPANCY_DAYS} days (same rule as electricity bills).
     */
    private void createRoomChargeShares(OtherCharge charge) {
        LocalDate today = LocalDate.now();
        List<User> eligible = roomAllotmentRepository
                .findByRoomAndRoomAllotmentStatusIn(charge.getRoom(), RoomAllotmentStatus.occupyingStatuses())
                .stream()
                .filter(a -> a.getStartDate() != null
                        && ChronoUnit.DAYS.between(a.getStartDate(), today) >= MIN_OCCUPANCY_DAYS)
                .map(RoomAllotment::getTenant)
                .collect(Collectors.toList());

        if (eligible.isEmpty()) {
            log.warn("No eligible tenants for room charge {}; no shares created", charge.getChargeId());
            return;
        }

        List<BigDecimal> shares = splitAmount(charge.getAmount(), eligible.size());
        for (int i = 0; i < eligible.size(); i++) {
            otherChargePaymentRepository.save(OtherChargePayment.builder()
                    .chargeId(charge.getChargeId())
                    .tenantId(eligible.get(i).getUserId())
                    .amount(shares.get(i))
                    .status(PaymentStatus.PENDING)
                    .build());
        }
        log.info("Room charge {} split into {} share(s)", charge.getChargeId(), eligible.size());
    }

    /**
     * Divides {@code total} into {@code parts} 2-decimal amounts that sum exactly to
     * {@code total}; any rounding remainder is added to the first share.
     */
    private List<BigDecimal> splitAmount(BigDecimal total, int parts) {
        BigDecimal base = total.divide(BigDecimal.valueOf(parts), 2, RoundingMode.DOWN);
        List<BigDecimal> shares = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            shares.add(base);
        }
        BigDecimal remainder = total.subtract(base.multiply(BigDecimal.valueOf(parts)));
        shares.set(0, shares.get(0).add(remainder));
        return shares;
    }

    public List<OtherChargeResponse> getChargesByOwner(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));

        List<OtherCharge> charges = otherChargeRepository.findByOwnerAndActiveTrue(owner);
        return charges.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<OtherChargeResponse> getChargesByOwner(UUID ownerId, Pageable pageable) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));

        Page<OtherCharge> charges = otherChargeRepository.findByOwnerAndActiveTrue(owner, pageable);
        return charges.map(this::mapToResponse);
    }

    public List<OtherChargeResponse> getChargesByTenant(UUID tenantId) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        List<OtherCharge> charges = otherChargeRepository.findByTenantAndActiveTrue(tenant);
        
        // Also get room-based charges for rooms where tenant is currently living
        List<RoomAllotment> allotments = roomAllotmentRepository.findByTenantAndRoomAllotmentStatus(tenant, RoomAllotmentStatus.ACTIVE);
        for (RoomAllotment allotment : allotments) {
            List<OtherCharge> roomCharges = otherChargeRepository.findByRoomAndActiveTrue(allotment.getRoom());
            charges.addAll(roomCharges);
        }

        return charges.stream()
                .distinct()
                .map(this::mapToResponse)
                // A split charge is only relevant to tenants who actually hold a share.
                .filter(resp -> tenantHasShareOrNotSplit(resp, tenantId))
                .map(resp -> applyTenantShareView(resp, tenantId))
                .collect(Collectors.toList());
    }

    /** True unless this is a split charge in which the tenant holds no share. */
    private boolean tenantHasShareOrNotSplit(OtherChargeResponse resp, UUID tenantId) {
        boolean isSplit = resp.getRoomTenants() != null
                && resp.getRoomTenants().stream().anyMatch(t -> t.getPaymentStatus() != null);
        if (!isSplit) {
            return true;
        }
        return resp.getRoomTenants().stream().anyMatch(t -> tenantId.equals(t.getTenantId()));
    }

    /**
     * For a room (split) charge, rewrite the response so its status/amounts reflect
     * the viewing tenant's own share rather than the whole charge. This lets the
     * tenant UI show their share as paid even while other tenants are still pending.
     */
    private OtherChargeResponse applyTenantShareView(OtherChargeResponse resp, UUID tenantId) {
        if (resp.getRoomTenants() == null) {
            return resp;
        }
        resp.getRoomTenants().stream()
                .filter(t -> tenantId.equals(t.getTenantId()) && t.getPaymentStatus() != null)
                .findFirst()
                .ifPresent(share -> {
                    share.setCurrentTenant(true);
                    boolean paid = share.getPaymentStatus() == PaymentStatus.COMPLETED;
                    resp.setPaymentStatus(share.getPaymentStatus());
                    resp.setPaidAmount(paid ? share.getSplitAmount() : BigDecimal.ZERO);
                    resp.setRemainingAmount(paid ? BigDecimal.ZERO : share.getSplitAmount());
                });
        return resp;
    }

    public OtherChargeResponse getChargeById(UUID chargeId) {
        OtherCharge charge = otherChargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found"));

        return mapToResponse(charge);
    }

    @Transactional
    public OtherChargeResponse updateCharge(UUID chargeId, OtherChargeRequest request, UUID ownerId) {
        OtherCharge charge = otherChargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found"));

        // Verify ownership
        if (!charge.getOwner().getUserId().equals(ownerId)) {
            throw new IllegalArgumentException("Not authorized to update this charge");
        }

        // Update basic fields
        charge.setChargeName(request.getChargeName());
        charge.setDescription(request.getDescription());
        charge.setAmount(request.getAmount());
        charge.setDueDate(request.getDueDate());

        charge = otherChargeRepository.save(charge);
        return mapToResponse(charge);
    }

    @Transactional
    public void deleteCharge(UUID chargeId, UUID ownerId) {
        OtherCharge charge = otherChargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found"));

        // Verify ownership
        if (!charge.getOwner().getUserId().equals(ownerId)) {
            throw new IllegalArgumentException("Not authorized to delete this charge");
        }

        // Soft delete
        charge.setActive(false);
        otherChargeRepository.save(charge);

        log.info("Deleted charge: {}", chargeId);
    }

    /**
     * Send SMS notification to all affected tenants when a new other charge is created.
     * Respects the owner's smsRemindersEnabled toggle.
     */
    private void sendOtherChargeSmsNotification(OtherCharge charge) {
        try {
            UUID ownerId = charge.getOwner().getUserId();

            // Check if owner has SMS reminders enabled
            if (!subscriptionService.hasFeature(ownerId, "sms_reminders")) {
                log.info("SMS reminders disabled for owner {}. Skipping other charge SMS.", ownerId);
                return;
            }

            // Resolve tenants to notify
            List<User> tenants = getTenantsForCharge(charge);
            if (tenants.isEmpty()) {
                log.warn("No tenants found for other charge SMS: {}", charge.getChargeId());
                return;
            }

            // Resolve hostel and room info
            String hostelName = charge.getHostel() != null ? charge.getHostel().getHostelName() : "N/A";
            String roomNumber = charge.getRoom() != null ? charge.getRoom().getRoomNumber() : "N/A";

            // Format due date
            String dueDateStr = charge.getDueDate() != null
                    ? charge.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    : "N/A";

            // Get template (custom if available, else default)
            String template = smsTemplateService.getTemplate(ownerId, ReminderType.OTHER_CHARGE);

            for (User tenant : tenants) {
                try {
                    // Build per-tenant amount (split for room-based charges)
                    BigDecimal tenantAmount = charge.isRoomBased() && tenants.size() > 1
                            ? charge.getAmount().divide(BigDecimal.valueOf(tenants.size()), 2, RoundingMode.HALF_UP)
                            : charge.getAmount();

                    Map<String, String> values = new HashMap<>();
                    values.put("tenantName", tenant.getDisplayName());
                    values.put("chargeName", charge.getChargeName());
                    values.put("amount", tenantAmount.toPlainString());
                    values.put("hostelName", hostelName);
                    values.put("roomNumber", roomNumber);
                    values.put("dueDate", dueDateStr);
                    values.put("description", charge.getDescription() != null ? charge.getDescription() : "");

                    String message = smsTemplateService.replacePlaceholders(template, values);
                    notificationService.sendSms(tenant.getPhoneNumber(), message);

                    log.info("Other charge SMS sent to tenant {} for charge {}", tenant.getUserId(), charge.getChargeId());
                } catch (Exception e) {
                    log.error("Failed to send other charge SMS to tenant {}: {}", tenant.getUserId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to send other charge SMS notifications for charge {}: {}", charge.getChargeId(), e.getMessage());
        }
    }

    private OtherChargeResponse mapToResponse(OtherCharge charge) {
        OtherChargeResponse.OtherChargeResponseBuilder builder = OtherChargeResponse.builder()
                .chargeId(charge.getChargeId())
                .chargeName(charge.getChargeName())
                .description(charge.getDescription())
                .amount(charge.getAmount())
                .category(charge.getCategory())
                .paymentStatus(charge.getPaymentStatus())
                .ownerId(charge.getOwner().getUserId())
                .ownerName(charge.getOwner().getDisplayName())
                .hostelId(charge.getHostel().getHostelId())
                .hostelName(charge.getHostel().getHostelName())
                .dueDate(charge.getDueDate())
                .paidDate(charge.getPaidDate())
                .paidAmount(charge.getPaidAmount())
                .remainingAmount(charge.getRemainingAmount())
                .installmentEnabled(charge.getInstallmentEnabled())
                .installmentCount(charge.getInstallmentCount())
                .installmentAmount(charge.getInstallmentAmount())
                .active(charge.getActive())
                .createdAt(charge.getCreatedAt())
                .updatedAt(charge.getUpdatedAt());

        // Add tenant details if tenant-specific
        if (charge.getTenant() != null) {
            builder.tenantId(charge.getTenant().getUserId())
                   .tenantName(charge.getTenant().getDisplayName());
        }

        // Add room details if room-based
        if (charge.getRoom() != null) {
            builder.roomId(charge.getRoom().getRoomId())
                   .roomNumber(charge.getRoom().getRoomNumber());

            List<OtherChargePayment> shares = otherChargePaymentRepository.findByChargeId(charge.getChargeId());
            List<OtherChargeResponse.TenantSummary> roomTenants;

            if (!shares.isEmpty()) {
                // Full-payment split charge: one fixed share per tenant, with status.
                roomTenants = shares.stream()
                        .map(share -> OtherChargeResponse.TenantSummary.builder()
                                .tenantId(share.getTenantId())
                                .tenantName(share.getTenant() != null ? share.getTenant().getDisplayName() : "N/A")
                                .phoneNumber(share.getTenant() != null ? share.getTenant().getPhoneNumber() : null)
                                .splitAmount(share.getAmount())
                                .paymentStatus(share.getStatus())
                                .paidAt(share.getPaidAt())
                                .build())
                        .collect(Collectors.toList());
            } else {
                // Installment / legacy charge: compute the split dynamically from
                // current occupants for display only.
                List<RoomAllotment> allotments = roomAllotmentRepository.findByRoomAndRoomAllotmentStatus(charge.getRoom(), RoomAllotmentStatus.ACTIVE);
                roomTenants = allotments.stream()
                        .map(allotment -> {
                            BigDecimal splitAmount = allotments.isEmpty() ? charge.getAmount()
                                    : charge.getAmount().divide(BigDecimal.valueOf(allotments.size()), 2, RoundingMode.HALF_UP);
                            return OtherChargeResponse.TenantSummary.builder()
                                    .tenantId(allotment.getTenant().getUserId())
                                    .tenantName(allotment.getTenant().getDisplayName())
                                    .phoneNumber(allotment.getTenant().getPhoneNumber())
                                    .splitAmount(splitAmount)
                                    .build();
                        })
                        .collect(Collectors.toList());
            }
            builder.roomTenants(roomTenants);
        }

        // Add installment details if enabled
        if (charge.getInstallmentEnabled()) {
            List<OtherChargeInstallment> installments = installmentRepository.findByOtherChargeOrderByInstallmentNumber(charge);
            List<OtherChargeResponse.InstallmentSummary> installmentSummaries = installments.stream()
                    .map(installment -> OtherChargeResponse.InstallmentSummary.builder()
                            .installmentId(installment.getInstallmentId())
                            .installmentNumber(installment.getInstallmentNumber())
                            .amount(installment.getAmount())
                            .dueDate(installment.getDueDate())
                            .paymentStatus(installment.getPaymentStatus())
                            .paidDate(installment.getPaidDate())
                            .paidAmount(installment.getPaidAmount())
                            .remainingAmount(installment.getRemainingAmount())
                            .isOverdue(installment.isOverdue())
                            .build())
                    .collect(Collectors.toList());
            builder.installments(installmentSummaries);
        }

        return builder.build();
    }
}