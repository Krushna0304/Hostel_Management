package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.SettlementApprovalDto;
import com.krunity.HostelManagment.dto.SettlementCalculationDto;
import com.krunity.HostelManagment.dto.SettlementRequestDto;
import com.krunity.HostelManagment.dto.SettlementResponseDto;
import com.krunity.HostelManagment.enums.AgreementStatus;
import com.krunity.HostelManagment.enums.PaymentStatus;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.enums.SettlementStatus;
import com.krunity.HostelManagment.exception.ConflictException;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class SettlementService {

    @Autowired
    private SettlementRequestRepository settlementRepository;

    @Autowired
    private ExtendAllotmentRequestRepository extendAllotmentRequestRepository;

    @Autowired
    private AgreementRepository agreementRepository;

    @Autowired
    private OtherChargePaymentService otherChargePaymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomAllotmentRepository roomAllotmentRepository;


    @Autowired
    private TenantPaymentPlanRepository paymentPlanRepository;

    @Autowired
    private PaymentRequestScheduleRepository paymentRequestScheduleRepository;

    @Autowired
    private OtherChargeRepository otherChargeRepository;

    @Autowired
    private OtherChargePaymentRepository otherChargePaymentRepository;

    @Autowired
    private ElectricityPaymentRepository electricityPaymentRepository;

    @Autowired
    private PaymentCalculationService paymentCalculationService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RoomAllotmentStatusTransitionValidator transitionValidator;


    @Autowired
    private AllotmentService allotmentService;

    @Transactional
    public SettlementRequest initiateSettlement(SettlementRequestDto requestDto, UUID tenantId) {
        log.info("Initiating settlement request for agreement: {} by tenant: {}", requestDto.getAgreementId(), tenantId);

        // Add debugging to see what agreements exist for this tenant
        List<Agreement> tenantAgreements = agreementRepository.findByUserId(tenantId);
        log.info("Found {} agreements for tenant {}: {}", 
                tenantAgreements.size(), 
                tenantId, 
                tenantAgreements.stream().map(Agreement::getId).toList());

        // Validate agreement exists and is active
        Agreement agreement = agreementRepository.findById(requestDto.getAgreementId())
                .orElseThrow(() -> {
                    log.error("Agreement not found with ID: {}. Available agreements for tenant {}: {}", 
                            requestDto.getAgreementId(), 
                            tenantId, 
                            tenantAgreements.stream().map(Agreement::getId).toList());
                    return new NotFoundException("Agreement not found with ID: " + requestDto.getAgreementId());
                });

        if (agreement.getStatus() != AgreementStatus.ACTIVE) {
            throw new ConflictException("Settlement can only be requested for active agreements. Current status: " + agreement.getStatus());
        }

        if (!agreement.getUserId().equals(tenantId)) {
            throw new ConflictException("You can only request settlement for your own agreement");
        }

        // Check if settlement already exists
        if (settlementRepository.existsByAgreementIdAndStatusNotIn(
                requestDto.getAgreementId(), 
                Arrays.asList(SettlementStatus.COMPLETED, SettlementStatus.CANCELLED, SettlementStatus.REJECTED))) {
            throw new ConflictException("Settlement request already exists for this agreement");
        }

        // Get tenant and owner
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found"));
        User owner = userRepository.findById(agreement.getOwnerId())
                .orElseThrow(() -> new NotFoundException("Owner not found"));

        // Get room if applicable
        Room room = null;
        if (agreement.getRoomId() != null) {
            room = roomRepository.findById(agreement.getRoomId()).orElse(null);
        }

        // Create settlement request
        SettlementRequest settlement = SettlementRequest.builder()
                .agreementId(requestDto.getAgreementId())
                .tenant(tenant)
                .owner(owner)
                .room(room)
                .status(SettlementStatus.PENDING_OWNER_REVIEW)
                .securityDeposit(calculateRefundableAmount(agreement))
                .tenantNotes(requestDto.getTenantNotes())
                .requestedEndDate(requestDto.getRequestedEndDate())
                .build();

        settlement = settlementRepository.save(settlement);

        // Update agreement status
        agreement.setStatus(AgreementStatus.SETTLEMENT_REQUESTED);
        agreementRepository.save(agreement);

        // Transition allotment → SETTLEMENT_REQUESTED
        transitionAllotmentOnSettlementRequest(agreement, tenantId);

        // Send notification to owner
        notificationService.sendSettlementRequestNotification(owner, tenant, settlement);

        log.info("Settlement request created with ID: {}", settlement.getSettlementId());
        return settlement;
    }

    @Transactional(readOnly = true)
    public SettlementCalculationDto calculateSettlement(UUID settlementId) {
        log.info("Calculating settlement for ID: {}", settlementId);

        SettlementRequest settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new NotFoundException("Settlement request not found"));

        Agreement agreement = agreementRepository.findById(settlement.getAgreementId())
                .orElseThrow(() -> new NotFoundException("Agreement not found"));

        LocalDate settlementDate = settlement.getRequestedEndDate();
        BigDecimal refundableAmount = calculateRefundableAmount(agreement);
        BigDecimal outstandingRent = calculateOutstandingRent(settlement.getAgreementId(), settlementDate);
        BigDecimal outstandingCharges = calculateOutstandingCharges(agreement.getUserId(), settlementDate);
        BigDecimal outstandingElectricityBills = calculateOutstandingElectricityBills(agreement, settlementDate);

        // Get outstanding items details
        List<SettlementCalculationDto.OutstandingItemDto> outstandingItems =
                getOutstandingItemsDetails(agreement, settlementDate);

        // Calculate total deductions
        BigDecimal totalDeductions = outstandingRent
                .add(outstandingCharges)
                .add(outstandingElectricityBills)
                .add(settlement.getDamageCharges())
                .add(settlement.getCleaningCharges())
                .add(settlement.getOtherDeductions());

        // Calculate final settlement amount
        // The prior agreement's refundable credit may already have reduced the
        // activation charge of a signed extension. Apply that exact snapshot
        // once here so it cannot be refunded or charged twice.
        BigDecimal extensionCredit = extendAllotmentRequestRepository
                .findByCurrentAgreementId(agreement.getId())
                .filter(request -> request.getNewAgreementId() != null)
                .map(ExtendAllotmentRequest::getPreviousAgreementRefundableAmount)
                .orElse(BigDecimal.ZERO);
        settlement.setExtensionCreditApplied(extensionCredit);

        BigDecimal finalAmount = refundableAmount.subtract(totalDeductions).subtract(extensionCredit);
        String settlementType = finalAmount.compareTo(BigDecimal.ZERO) >= 0 ? "OWNER_PAYABLE" : "TENANT_PAYABLE";

        return SettlementCalculationDto.builder()
                .settlementId(settlement.getSettlementId().toString())
                .agreementId(settlement.getAgreementId())
                .tenantName(settlement.getTenant().getDisplayName())
                .roomNumber(settlement.getRoom() != null ? settlement.getRoom().getRoomNumber() : "N/A")
                .securityDeposit(refundableAmount)
                .outstandingRent(outstandingRent)
                .outstandingCharges(outstandingCharges)
                .outstandingElectricityBills(outstandingElectricityBills)
                .damageCharges(settlement.getDamageCharges())
                .cleaningCharges(settlement.getCleaningCharges())
                .otherDeductions(settlement.getOtherDeductions())
                .totalDeductions(totalDeductions)
                .finalSettlementAmount(finalAmount.abs())
                .settlementType(settlementType)
                .status(settlement.getStatus().toString())
                .settledAt(settlement.getSettledAt())
                .paymentReference(settlement.getPaymentReference())
                .outstandingItems(outstandingItems)
                .tenantNotes(settlement.getTenantNotes())
                .ownerNotes(settlement.getOwnerNotes())
                .damageDescription(settlement.getDamageDescription())
                .createdAt(settlement.getCreatedAt())
                .updatedAt(settlement.getUpdatedAt())
                .requestedEndDate(settlement.getRequestedEndDate())
                .build();
    }

    @Transactional
    public SettlementRequest approveSettlement(UUID settlementId, SettlementApprovalDto approvalDto, UUID ownerId) {
        log.info("Processing settlement approval for ID: {} by owner: {}", settlementId, ownerId);

        SettlementRequest settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new NotFoundException("Settlement request not found"));

        if (!settlement.getOwner().getUserId().equals(ownerId)) {
            throw new ConflictException("You can only approve your own settlement requests");
        }

        if (settlement.getStatus() != SettlementStatus.PENDING_OWNER_REVIEW) {
            throw new ConflictException("Settlement is not in pending review status");
        }

        if (!approvalDto.isApproved()) {
            // Reject settlement
            settlement.setStatus(SettlementStatus.REJECTED);
            settlement.setOwnerNotes(approvalDto.getOwnerNotes());
            settlement = settlementRepository.save(settlement);

            // Revert agreement status
            Agreement agreement = agreementRepository.findById(settlement.getAgreementId())
                    .orElseThrow(() -> new NotFoundException("Agreement not found"));
            agreement.setStatus(AgreementStatus.ACTIVE);
            agreementRepository.save(agreement);

            // Notify tenant
            notificationService.sendSettlementRejectionNotification(settlement.getTenant(), settlement);
            return settlement;
        }

        Agreement agreement = agreementRepository.findById(settlement.getAgreementId())
                .orElseThrow(() -> new NotFoundException("Agreement not found"));
        BigDecimal refundableAmount = calculateRefundableAmount(agreement);
        settlement.setSecurityDeposit(refundableAmount);

        // Update settlement with owner's charges
        settlement.setDamageCharges(approvalDto.getDamageCharges());
        settlement.setCleaningCharges(approvalDto.getCleaningCharges());
        settlement.setOtherDeductions(approvalDto.getOtherDeductions());
        settlement.setOwnerNotes(approvalDto.getOwnerNotes());
        settlement.setDamageDescription(approvalDto.getDamageDescription());

        // Calculate final amounts
        LocalDate settlementDate = settlement.getRequestedEndDate();
        BigDecimal outstandingRent = calculateOutstandingRent(settlement.getAgreementId(), settlementDate);
        BigDecimal outstandingCharges = calculateOutstandingCharges(settlement.getTenant().getUserId(), settlementDate);
        BigDecimal outstandingElectricityBills = calculateOutstandingElectricityBills(agreement, settlementDate);

        settlement.setOutstandingRent(outstandingRent);
        settlement.setOutstandingCharges(outstandingCharges);
        settlement.setOutstandingElectricityBills(outstandingElectricityBills);

        BigDecimal totalDeductions = outstandingRent
                .add(outstandingCharges)
                .add(outstandingElectricityBills)
                .add(settlement.getDamageCharges())
                .add(settlement.getCleaningCharges())
                .add(settlement.getOtherDeductions());

        settlement.setTotalDeductions(totalDeductions);

        // Use the same X credit shown during extension activation. The value is
        // read from the linked request rather than recalculated from mutable plan data.
        BigDecimal extensionCredit = extendAllotmentRequestRepository
                .findByCurrentAgreementId(agreement.getId())
                .filter(request -> request.getNewAgreementId() != null)
                .map(ExtendAllotmentRequest::getPreviousAgreementRefundableAmount)
                .orElse(BigDecimal.ZERO);
        settlement.setExtensionCreditApplied(extensionCredit);
        BigDecimal finalAmount = refundableAmount.subtract(totalDeductions).subtract(extensionCredit);
        settlement.setFinalSettlementAmount(finalAmount.abs());

        if (finalAmount.compareTo(BigDecimal.ZERO) >= 0) {
            // Owner needs to pay tenant
            settlement.setSettlementType("OWNER_PAYABLE");
            settlement.setStatus(SettlementStatus.PENDING_OWNER_PAYMENT);
        } else {
            // Tenant needs to pay owner
            settlement.setSettlementType("TENANT_PAYABLE");
            settlement.setStatus(SettlementStatus.PENDING_TENANT_PAYMENT);
        }

        settlement = settlementRepository.save(settlement);

        applySettlementEndDateToAllotment(settlement);

        // Notify tenant about approval
        notificationService.sendSettlementApprovalNotification(settlement.getTenant(), settlement);

        log.info("Settlement approved. Type: {}, Amount: {}", settlement.getSettlementType(), settlement.getFinalSettlementAmount());
        return settlement;
    }

    @Transactional
    public SettlementRequest completeSettlement(UUID settlementId, String paymentReference, UUID userId) {
        log.info("Completing settlement for ID: {} with payment reference: {}", settlementId, paymentReference);

        SettlementRequest settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new NotFoundException("Settlement request not found"));

        // Validate user can complete this settlement
        boolean canComplete = settlement.getOwner().getUserId().equals(userId) || 
                             settlement.getTenant().getUserId().equals(userId);
        
        if (!canComplete) {
            throw new ConflictException("You are not authorized to complete this settlement");
        }

        // Validate status
        if (!Arrays.asList(SettlementStatus.PENDING_OWNER_PAYMENT, SettlementStatus.PENDING_TENANT_PAYMENT)
                .contains(settlement.getStatus())) {
            throw new ConflictException("Settlement is not in a payable status");
        }

        // Complete settlement
        settlement.setStatus(SettlementStatus.COMPLETED);
        settlement.setPaymentReference(paymentReference);
        settlement.setSettledAt(LocalDateTime.now());
        settlement = settlementRepository.save(settlement);

        // Update agreement status to settled and endDate 
        Agreement agreement = agreementRepository.findById(settlement.getAgreementId())
                .orElseThrow(() -> new NotFoundException("Agreement not found"));
        agreement.setStatus(AgreementStatus.SETTLED);
        agreement.setEndDate(settlement.getRequestedEndDate());
        agreementRepository.save(agreement);

        activateLinkedExtension(agreement.getId());

        // Transition allotment → ON_NOTICE_PERIOD now that payment is done
        transitionAllotmentToOnNoticePeriod(settlement);
        allotmentService.markEndDate(settlementId, userId, settlement.getRequestedEndDate());
        // Mark outstanding items as PAID (included in settlement) or CANCELLED (future/excluded)
        finalizeOutstandingPaymentsOnSettlementComplete(settlement, agreement);

        finalizeAllotmentOnSettlementComplete(settlement);

        // Send completion notifications
        notificationService.sendSettlementCompletionNotification(settlement.getOwner(), settlement.getTenant(), settlement);

        log.info("Settlement completed successfully for agreement: {}", settlement.getAgreementId());
        return settlement;
    }

    private void activateLinkedExtension(String previousAgreementId) {
        extendAllotmentRequestRepository.findByCurrentAgreementId(previousAgreementId)
                .filter(request -> request.getNewAgreementId() != null)
                .ifPresent(request -> agreementRepository.findById(request.getNewAgreementId())
                        .ifPresent(extensionAgreement -> {
                            if (extensionAgreement.getStatus() == AgreementStatus.PENDING_PREVIOUS_SETTLEMENT) {
                                extensionAgreement.setStatus(AgreementStatus.ACTIVE);
                                extensionAgreement.setActivatedAt(java.time.Instant.now());
                                agreementRepository.save(extensionAgreement);
                                request.updateStatus(com.krunity.HostelManagment.enums.ExtendAllotmentStatus.ACTIVE);
                                extendAllotmentRequestRepository.save(request);
                                log.info("Activated extension agreement {} after settlement of {}",
                                        extensionAgreement.getId(), previousAgreementId);
                            }
                        }));
    }

    public List<SettlementResponseDto> getOwnerSettlements(UUID ownerId) {
        List<SettlementRequest> settlements = settlementRepository.findByOwnerOrderByCreatedAtDesc(ownerId);
        return settlements.stream()
                .map(s -> enrichWithAllotment(com.krunity.HostelManagment.Mapper.SettlementMapper.toResponseDto(s), s))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<SettlementResponseDto> getTenantSettlements(UUID tenantId) {
        List<SettlementRequest> settlements = settlementRepository.findByTenantOrderByCreatedAtDesc(tenantId);
        return settlements.stream()
                .map(s -> enrichWithAllotment(com.krunity.HostelManagment.Mapper.SettlementMapper.toResponseDto(s), s))
                .collect(java.util.stream.Collectors.toList());
    }

    private SettlementResponseDto enrichWithAllotment(SettlementResponseDto dto, SettlementRequest settlement) {
        if (settlement.getTenant() == null || settlement.getAgreementId() == null) return dto;
        roomAllotmentRepository
                .findByTenant_UserIdAndAgreementId(settlement.getTenant().getUserId(), settlement.getAgreementId())
                .ifPresent(allotment -> {
                    dto.setAllotmentId(allotment.getAllotmentId());
                    dto.setAllotmentStatus(allotment.getRoomAllotmentStatus().name());
                    dto.setTenantMarkedLeft(allotment.isTenantMarkedLeft());
                    dto.setOwnerMarkedLeft(allotment.isOwnerMarkedLeft());
                });
        return dto;
    }

    private BigDecimal calculateOutstandingRent(String agreementId, LocalDate settlementDate) {
        // Find the payment plan for this agreement
        Optional<TenantPaymentPlan> paymentPlanOpt = paymentPlanRepository.findByAgreementId(agreementId);
        
        if (paymentPlanOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        TenantPaymentPlan paymentPlan = paymentPlanOpt.get();
        return paymentRequestScheduleRepository.findByTenantPaymentPlan(paymentPlan).stream()
                .filter(schedule -> schedule.getDueDate() != null && !schedule.getDueDate().isAfter(settlementDate))
                .filter(schedule -> schedule.getPaymentStatus() != com.krunity.HostelManagment.enums.TransactionStatus.COMPLETED)
                .filter(schedule -> schedule.getPaymentStatus() != com.krunity.HostelManagment.enums.TransactionStatus.CANCELLED)
                .filter(schedule -> schedule.getPaymentStatus() != com.krunity.HostelManagment.enums.TransactionStatus.FAILED)
                .map(this::getOutstandingInstallmentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateOutstandingCharges(UUID tenantId, LocalDate settlementDate) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found"));
                
        List<OtherCharge> pendingCharges = otherChargeRepository
                .findByTenantAndPaymentStatusIn(tenant,
                    Arrays.asList(PaymentStatus.PENDING, PaymentStatus.OVERDUE));

        BigDecimal amt1 = otherChargePaymentService.calculateOutstandingCharge(tenantId);
        BigDecimal amt2 =  pendingCharges.stream()
                .filter(charge -> isChargeDueBySettlementDate(charge, settlementDate))
                .map(charge -> charge.getAmount().subtract(charge.getPaidAmount() != null ? charge.getPaidAmount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return amt1.add(amt2);
    }

    private BigDecimal calculateOutstandingElectricityBills(Agreement agreement, LocalDate settlementDate) {
        if (agreement.getRoomId() == null) {
            return BigDecimal.ZERO;
        }

        return electricityPaymentRepository.findByTenantIdWithBillDetails(agreement.getUserId()).stream()
                .filter(payment -> payment.getStatus() != PaymentStatus.COMPLETED)
                .filter(payment -> payment.getElectricityBill() != null)
                .filter(payment -> agreement.getRoomId().equals(payment.getElectricityBill().getRoomId()))
                .map(ElectricityPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<SettlementCalculationDto.OutstandingItemDto> getOutstandingItemsDetails(
            Agreement agreement, LocalDate settlementDate) {
        List<SettlementCalculationDto.OutstandingItemDto> items = new ArrayList<>();

        // Add overdue rent installments
        Optional<TenantPaymentPlan> paymentPlanOpt = paymentPlanRepository.findByAgreementId(agreement.getId());
        
        if (paymentPlanOpt.isPresent()) {
            TenantPaymentPlan paymentPlan = paymentPlanOpt.get();
            List<PaymentRequestSchedule> schedules = paymentRequestScheduleRepository
                    .findByTenantPaymentPlan(paymentPlan);

            for (PaymentRequestSchedule schedule : schedules) {
                if (schedule.getDueDate() == null || schedule.getDueDate().isAfter(settlementDate)
                        || schedule.getPaymentStatus() == com.krunity.HostelManagment.enums.TransactionStatus.COMPLETED
                        || schedule.getPaymentStatus() == com.krunity.HostelManagment.enums.TransactionStatus.CANCELLED
                        || schedule.getPaymentStatus() == com.krunity.HostelManagment.enums.TransactionStatus.FAILED) {
                    continue;
                }
                items.add(SettlementCalculationDto.OutstandingItemDto.builder()
                        .type("INSTALLMENT")
                        .description("Installment #" + schedule.getInstallmentNumber())
                        .amount(getOutstandingInstallmentAmount(schedule))
                        .dueDate(schedule.getDueDate().toString())
                        .status(schedule.getPaymentStatus().toString())
                        .build());
            }
        }

        // Add pending other charges
        User tenant = userRepository.findById(agreement.getUserId())
                .orElse(null);
        
        if (tenant != null) {
            List<OtherCharge> pendingCharges = otherChargeRepository
                    .findByTenantAndPaymentStatusIn(tenant,
                        Arrays.asList(PaymentStatus.PENDING, PaymentStatus.OVERDUE));

            for (OtherCharge charge : pendingCharges) {

                BigDecimal outstandingAmount = charge.getAmount().subtract(
                    charge.getPaidAmount() != null ? charge.getPaidAmount() : BigDecimal.ZERO);
                if (outstandingAmount.compareTo(BigDecimal.ZERO) > 0) {
                    items.add(SettlementCalculationDto.OutstandingItemDto.builder()
                            .type(charge.getCategory().toString())
                            .description(charge.getChargeName())
                            .amount(outstandingAmount)
                            .dueDate(charge.getDueDate() != null ? charge.getDueDate().toString() : "N/A")
                            .status(charge.getPaymentStatus().toString())
                            .build());
                }
            }

            List<OtherChargePayment> pendingCharges2 = otherChargePaymentService.getOutstandingCharge(tenant.getUserId());
//
            for (OtherChargePayment charge : pendingCharges2) {

                BigDecimal outstandingAmount = charge.getAmount();
                if (outstandingAmount.compareTo(BigDecimal.ZERO) > 0) {
                    items.add(SettlementCalculationDto.OutstandingItemDto.builder()
//                            .type(charge.getgetCategory().toString())
                            .description(charge.getChargeName())
                            .amount(outstandingAmount)
//                            .dueDate(charge.getCharge(). != null ? charge.getDueDate().toString() : "N/A")
                            .status(charge.getStatus().toString())
                            .build());
                }
            }
        }

        if (agreement.getRoomId() != null) {
            for (ElectricityPayment payment : electricityPaymentRepository
                    .findByTenantIdWithBillDetails(agreement.getUserId())) {
                ElectricityBill bill = payment.getElectricityBill();
                if (payment.getStatus() == PaymentStatus.COMPLETED || bill == null
                        || !agreement.getRoomId().equals(bill.getRoomId())
                ) {
                    continue;
                }
                items.add(SettlementCalculationDto.OutstandingItemDto.builder()
                        .type("ELECTRICITY")
                        .description("Electricity bill - " + bill.getBillMonth() + "/" + bill.getBillYear())
                        .amount(payment.getAmount())
                        .dueDate(bill.getDueDate() != null ? bill.getDueDate().toLocalDate().toString() : "N/A")
                        .status(payment.getStatus().toString())
                        .build());
            }
        }

        return items;
    }

    private BigDecimal calculateRefundableAmount(Agreement agreement) {
        return paymentCalculationService.calculatePaymentBreakdown(agreement.getPlanSnapshot())
                .getAgreementTimeRefundable();
    }

    private BigDecimal getOutstandingInstallmentAmount(PaymentRequestSchedule schedule) {
        long paidAmount = schedule.getPaidAmount() != null ? schedule.getPaidAmount() : 0L;
        long lateFee = schedule.getLateFeeApplied() != null ? schedule.getLateFeeApplied() : 0L;
        return BigDecimal.valueOf(schedule.getAmount() - paidAmount + lateFee);
    }

    private boolean isChargeDueBySettlementDate(OtherCharge charge, LocalDate settlementDate) {
        LocalDate chargeDate = charge.getDueDate() != null
                ? charge.getDueDate().toLocalDate()
                : charge.getCreatedAt() != null ? charge.getCreatedAt().toLocalDate() : settlementDate;
        return !chargeDate.isAfter(settlementDate);
    }

    private boolean isElectricityBillDueBySettlementDate(ElectricityBill bill, LocalDate settlementDate) {
        if (bill.getDueDate() != null) {
            return !bill.getDueDate().toLocalDate().isAfter(settlementDate);
        }
        LocalDate billMonth = LocalDate.of(bill.getBillYear(), bill.getBillMonth(), 1);
        return !billMonth.isAfter(settlementDate.withDayOfMonth(1));
    }

    private void applySettlementEndDateToAllotment(SettlementRequest settlement) {
        if (settlement.getRequestedEndDate() == null) {
            return;
        }

        int updated = roomAllotmentRepository.updateEndDateByTenantAndAgreement(
                settlement.getTenant().getUserId(),
                settlement.getAgreementId(),
                settlement.getRequestedEndDate());

        if (updated == 0) {
            log.warn("No active allotment found to set end date for agreement {}", settlement.getAgreementId());
        }
    }

    private void finalizeAllotmentOnSettlementComplete(SettlementRequest settlement) {
        if (settlement.getRoom() == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = settlement.getRequestedEndDate();

        if (endDate != null && today.isAfter(endDate)) {
            roomAllotmentRepository.markAllotmentLeft(
                    settlement.getTenant().getUserId(),
                    settlement.getRoom().getRoomId(),
                    settlement.getAgreementId());
        }
    }

    // ─── Allotment status transition helpers ──────────────────────────────────

    /**
     * Transitions the allotment from UPCOMING / ACTIVE / SETTLEMENT_PENDING
     * → SETTLEMENT_REQUESTED and sets earlyExit + audit fields.
     */
    private void transitionAllotmentOnSettlementRequest(Agreement agreement, UUID tenantId) {
        if (agreement.getRoomId() == null) return;

        roomAllotmentRepository
                .findByTenant_UserIdAndAgreementIdAndRoomAllotmentStatusIn(
                        tenantId,
                        agreement.getId(),
                        RoomAllotmentStatus.settlementRequestableStatuses())
                .ifPresent(allotment -> {
                    transitionValidator.validate(
                            allotment.getRoomAllotmentStatus(), RoomAllotmentStatus.SETTLEMENT_REQUESTED);

                    // Early exit: tenant requested before the notice-period window opened
                    if (allotment.getEndDate() != null && allotment.getNoticePeriodMonths() != null) {
                        LocalDate noticeWindowStart =
                                allotment.getEndDate().minusMonths(allotment.getNoticePeriodMonths());
                        allotment.setEarlyExit(LocalDate.now().isBefore(noticeWindowStart));
                    }

                    allotment.setRoomAllotmentStatus(RoomAllotmentStatus.SETTLEMENT_REQUESTED);
                    allotment.setSettlementRequestedAt(LocalDateTime.now());
                    allotment.setLastStatusChangedBy("TENANT");
                    allotment.setLastStatusChangedAt(LocalDateTime.now());
                    roomAllotmentRepository.save(allotment);
                    log.info("Allotment {} transitioned to SETTLEMENT_REQUESTED (earlyExit={})",
                            allotment.getAllotmentId(), allotment.isEarlyExit());
                });
    }

    // ─── Settlement finalisation: mark items PAID or CANCELLED ──────────────

    /**
     * After a settlement is completed, stamps every outstanding payment item that
     * was factored into the settlement calculation as {@code COMPLETED} (i.e. the
     * outstanding balance is absorbed by the settlement). Every item that falls
     * <em>outside</em> the settlement window (future installments, future charges,
     * electricity bills for a different room, etc.) is stamped {@code CANCELLED} so
     * they no longer appear as open debts.
     *
     * <p>The three buckets mirror the three amounts computed in
     * {@code approveSettlement()}:
     * <ol>
     *   <li>Rent installments – {@code calculateOutstandingRent()}</li>
     *   <li>Other charges / charge-payment shares – {@code calculateOutstandingCharges()}</li>
     *   <li>Electricity payment shares – {@code calculateOutstandingElectricityBills()}</li>
     * </ol>
     */
    private void finalizeOutstandingPaymentsOnSettlementComplete(
            SettlementRequest settlement, Agreement agreement) {

        LocalDate settlementDate = settlement.getRequestedEndDate();
        UUID tenantId = settlement.getTenant().getUserId();

        log.info("Finalising outstanding payments for settlement {} (date={})", 
                settlement.getSettlementId(), settlementDate);

        finalizeInstallmentsOnSettlement(settlement.getAgreementId(), settlementDate);
        finalizeOtherChargesOnSettlement(tenantId, settlementDate);
        finalizeElectricityPaymentsOnSettlement(agreement, settlementDate);
    }

    /**
     * Marks rent installments that were <em>included</em> in the settlement
     * (due on or before {@code settlementDate} and not already terminal) as
     * {@code CANCELLED} — the debt is absorbed into the settlement amount.
     * Installments due <em>after</em> the settlement date are also cancelled
     * because the tenancy ends on that date.
     */
    private void finalizeInstallmentsOnSettlement(String agreementId, LocalDate settlementDate) {
        Optional<TenantPaymentPlan> planOpt = paymentPlanRepository.findByAgreementId(agreementId);
        if (planOpt.isEmpty()) {
            log.debug("No payment plan found for agreement {}, skipping installment finalisation", agreementId);
            return;
        }

        List<PaymentRequestSchedule> schedules =
                paymentRequestScheduleRepository.findByTenantPaymentPlan(planOpt.get());

        List<PaymentRequestSchedule> toUpdate = new ArrayList<>();

        for (PaymentRequestSchedule schedule : schedules) {
            // Already in a terminal state – leave as-is
            if (isInstallmentTerminal(schedule.getPaymentStatus())) {
                continue;
            }

            if (schedule.getDueDate() != null && !schedule.getDueDate().isAfter(settlementDate)) {
                // Included in the settlement calculation → mark CANCELLED
                // (the outstanding balance was rolled into the settlement amount)
                schedule.setPaymentStatus(com.krunity.HostelManagment.enums.TransactionStatus.CANCELLED);
                log.debug("Installment #{} (due {}) absorbed into settlement – marked CANCELLED",
                        schedule.getInstallmentNumber(), schedule.getDueDate());
            } else {
                // Future installment that will never become due → CANCELLED
                schedule.setPaymentStatus(com.krunity.HostelManagment.enums.TransactionStatus.CANCELLED);
                log.debug("Future installment #{} (due {}) cancelled as tenancy ends on {}",
                        schedule.getInstallmentNumber(), schedule.getDueDate(), settlementDate);
            }

            toUpdate.add(schedule);
        }

        if (!toUpdate.isEmpty()) {
            paymentRequestScheduleRepository.saveAll(toUpdate);
            log.info("Finalised {} installment(s) for agreement {}", toUpdate.size(), agreementId);
        }
    }

    /**
     * For other charges and their split-payment shares:
     * <ul>
     *   <li>Charges/shares that were <em>included</em> in {@code calculateOutstandingCharges()}
     *       (PENDING or OVERDUE, due on or before settlementDate) → {@code CANCELLED}
     *       (absorbed into settlement).</li>
     *   <li>Charges/shares not yet due or belonging to a different tenant scope →
     *       {@code CANCELLED} (tenancy ended).</li>
     * </ul>
     */
    private void finalizeOtherChargesOnSettlement(UUID tenantId, LocalDate settlementDate) {
        User tenant = userRepository.findById(tenantId).orElse(null);
        if (tenant == null) return;

        // ── OtherCharge rows (tenant-specific, not yet fully paid) ──
        List<OtherCharge> openCharges = otherChargeRepository
                .findByTenantAndPaymentStatusIn(tenant,
                        Arrays.asList(PaymentStatus.PENDING, PaymentStatus.OVERDUE));

        List<OtherCharge> chargesToUpdate = new ArrayList<>();
        for (OtherCharge charge : openCharges) {
            // Only touch non-terminal charges
            if (charge.getPaymentStatus() == PaymentStatus.COMPLETED
                    || charge.getPaymentStatus() == PaymentStatus.CANCELLED) {
                continue;
            }
            // All open charges for this tenant are cancelled when the tenancy settles
            charge.setPaymentStatus(PaymentStatus.CANCELLED);
            chargesToUpdate.add(charge);
            log.debug("OtherCharge '{}' (id={}) cancelled on settlement", 
                    charge.getChargeName(), charge.getChargeId());
        }
        if (!chargesToUpdate.isEmpty()) {
            otherChargeRepository.saveAll(chargesToUpdate);
            log.info("Finalised {} other charge(s) for tenant {}", chargesToUpdate.size(), tenantId);
        }

        // ── OtherChargePayment share rows (for room-split charges) ──
        List<com.krunity.HostelManagment.model.OtherChargePayment> openPaymentShares =
                otherChargePaymentService.getOutstandingCharge(tenantId);

        List<com.krunity.HostelManagment.model.OtherChargePayment> sharesToUpdate = new ArrayList<>();
        for (com.krunity.HostelManagment.model.OtherChargePayment share : openPaymentShares) {
            if (share.getStatus() == PaymentStatus.COMPLETED
                    || share.getStatus() == PaymentStatus.CANCELLED) {
                continue;
            }
            share.setStatus(PaymentStatus.CANCELLED);
            sharesToUpdate.add(share);
            log.debug("OtherChargePayment share (id={}) cancelled on settlement", share.getPaymentId());
        }
        if (!sharesToUpdate.isEmpty()) {
            otherChargePaymentRepository.saveAll(sharesToUpdate);
            log.info("Finalised {} other-charge payment share(s) for tenant {}", 
                    sharesToUpdate.size(), tenantId);
        }
    }

    /**
     * Marks electricity payment shares that were included in
     * {@code calculateOutstandingElectricityBills()} as {@code CANCELLED}
     * (absorbed into settlement). Shares for other rooms are left untouched.
     */
    private void finalizeElectricityPaymentsOnSettlement(Agreement agreement, LocalDate settlementDate) {
        if (agreement.getRoomId() == null) return;

        List<ElectricityPayment> payments =
                electricityPaymentRepository.findByTenantIdWithBillDetails(agreement.getUserId());

        List<ElectricityPayment> toUpdate = new ArrayList<>();
        for (ElectricityPayment payment : payments) {
            // Already paid or already cancelled – skip
            if (payment.getStatus() == PaymentStatus.COMPLETED
                    || payment.getStatus() == PaymentStatus.CANCELLED) {
                continue;
            }

            ElectricityBill bill = payment.getElectricityBill();
            if (bill == null) continue;

            // Only touch bills for this tenant's room (mirrors calculateOutstandingElectricityBills)
            if (!agreement.getRoomId().equals(bill.getRoomId())) continue;

            // Mark cancelled – absorbed into settlement (whether before or after settlementDate)
            payment.setStatus(PaymentStatus.CANCELLED);
            toUpdate.add(payment);
            log.debug("ElectricityPayment (id={}, bill={}/{}) cancelled on settlement",
                    payment.getPaymentId(), bill.getBillMonth(), bill.getBillYear());
        }

        if (!toUpdate.isEmpty()) {
            electricityPaymentRepository.saveAll(toUpdate);
            log.info("Finalised {} electricity payment(s) for agreement {}", 
                    toUpdate.size(), agreement.getId());
        }
    }

    /** Returns true if an installment status is already terminal (no need to touch it). */
    private boolean isInstallmentTerminal(com.krunity.HostelManagment.enums.TransactionStatus status) {
        return status == com.krunity.HostelManagment.enums.TransactionStatus.COMPLETED
                || status == com.krunity.HostelManagment.enums.TransactionStatus.CANCELLED
                || status == com.krunity.HostelManagment.enums.TransactionStatus.FAILED;
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Transitions the allotment from SETTLEMENT_REQUESTED → ON_NOTICE_PERIOD
     * after the owner approves.
     */
    private void transitionAllotmentToOnNoticePeriod(SettlementRequest settlement) {
        if (settlement.getRoom() == null) return;

        roomAllotmentRepository
                .findByTenant_UserIdAndAgreementIdAndRoomAllotmentStatusIn(
                        settlement.getTenant().getUserId(),
                        settlement.getAgreementId(),
                        Set.of(RoomAllotmentStatus.SETTLEMENT_REQUESTED))
                .ifPresent(allotment -> {
                    transitionValidator.validate(
                            allotment.getRoomAllotmentStatus(), RoomAllotmentStatus.ON_NOTICE_PERIOD);

                    allotment.setRoomAllotmentStatus(RoomAllotmentStatus.ON_NOTICE_PERIOD);
                    allotment.setSettlementApprovedAt(LocalDateTime.now());
                    allotment.setLastStatusChangedBy("OWNER");
                    allotment.setLastStatusChangedAt(LocalDateTime.now());
                    roomAllotmentRepository.save(allotment);
                    log.info("Allotment {} transitioned to ON_NOTICE_PERIOD", allotment.getAllotmentId());
                });
    }
}
