package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.SettlementApprovalDto;
import com.krunity.HostelManagment.dto.SettlementCalculationDto;
import com.krunity.HostelManagment.dto.SettlementRequestDto;
import com.krunity.HostelManagment.dto.SettlementResponseDto;
import com.krunity.HostelManagment.enums.AgreementStatus;
import com.krunity.HostelManagment.enums.PaymentStatus;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class SettlementService {

    @Autowired
    private SettlementRequestRepository settlementRepository;

    @Autowired
    private AgreementRepository agreementRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomAllotmentRepository roomAllotmentRepository;


    @Autowired
    private TenantPaymentPlanRepository paymentPlanRepository;

    @Autowired
    private InstallmentInvoiceRepository installmentInvoiceRepository;

    @Autowired
    private OtherChargeRepository otherChargeRepository;

    @Autowired
    private NotificationService notificationService;

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
                .securityDeposit(agreement.getDeposit())
                .tenantNotes(requestDto.getTenantNotes())
                .build();

        settlement = settlementRepository.save(settlement);

        // Update agreement status
        agreement.setStatus(AgreementStatus.SETTLEMENT_REQUESTED);
        agreementRepository.save(agreement);

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

        // Calculate outstanding amounts
        BigDecimal outstandingRent = calculateOutstandingRent(settlement.getAgreementId());
        BigDecimal outstandingCharges = calculateOutstandingCharges(agreement.getUserId());

        // Get outstanding items details
        List<SettlementCalculationDto.OutstandingItemDto> outstandingItems = getOutstandingItemsDetails(agreement);

        // Calculate total deductions
        BigDecimal totalDeductions = outstandingRent
                .add(outstandingCharges)
                .add(settlement.getDamageCharges())
                .add(settlement.getCleaningCharges())
                .add(settlement.getOtherDeductions());

        // Calculate final settlement amount
        BigDecimal finalAmount = settlement.getSecurityDeposit().subtract(totalDeductions);
        String settlementType = finalAmount.compareTo(BigDecimal.ZERO) >= 0 ? "OWNER_PAYABLE" : "TENANT_PAYABLE";

        return SettlementCalculationDto.builder()
                .settlementId(settlement.getSettlementId().toString())
                .agreementId(settlement.getAgreementId())
                .tenantName(settlement.getTenant().getDisplayName())
                .roomNumber(settlement.getRoom() != null ? settlement.getRoom().getRoomNumber() : "N/A")
                .securityDeposit(settlement.getSecurityDeposit())
                .outstandingRent(outstandingRent)
                .outstandingCharges(outstandingCharges)
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

        // Update settlement with owner's charges
        settlement.setDamageCharges(approvalDto.getDamageCharges());
        settlement.setCleaningCharges(approvalDto.getCleaningCharges());
        settlement.setOtherDeductions(approvalDto.getOtherDeductions());
        settlement.setOwnerNotes(approvalDto.getOwnerNotes());
        settlement.setDamageDescription(approvalDto.getDamageDescription());

        // Calculate final amounts
        BigDecimal outstandingRent = calculateOutstandingRent(settlement.getAgreementId());
        BigDecimal outstandingCharges = calculateOutstandingCharges(settlement.getTenant().getUserId());

        settlement.setOutstandingRent(outstandingRent);
        settlement.setOutstandingCharges(outstandingCharges);

        BigDecimal totalDeductions = outstandingRent
                .add(outstandingCharges)
                .add(settlement.getDamageCharges())
                .add(settlement.getCleaningCharges())
                .add(settlement.getOtherDeductions());

        settlement.setTotalDeductions(totalDeductions);

        BigDecimal finalAmount = settlement.getSecurityDeposit().subtract(totalDeductions);
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

        // Update agreement status to settled
        Agreement agreement = agreementRepository.findById(settlement.getAgreementId())
                .orElseThrow(() -> new NotFoundException("Agreement not found"));
        agreement.setStatus(AgreementStatus.SETTLED);
        agreementRepository.save(agreement);

        Room room = settlement.getRoom();
        roomAllotmentRepository.removeTenant(settlement.getTenant().getUserId(),room.getRoomId());
        room.setAvailableBeds(room.getAvailableBeds() + 1);
        roomRepository.save(room);
        // Send completion notifications
        notificationService.sendSettlementCompletionNotification(settlement.getOwner(), settlement.getTenant(), settlement);

        log.info("Settlement completed successfully for agreement: {}", settlement.getAgreementId());
        return settlement;
    }

    public List<SettlementResponseDto> getOwnerSettlements(UUID ownerId) {
        List<SettlementRequest> settlements = settlementRepository.findByOwnerOrderByCreatedAtDesc(ownerId);
        return com.krunity.HostelManagment.Mapper.SettlementMapper.toResponseDtoList(settlements);
    }

    public List<SettlementResponseDto> getTenantSettlements(UUID tenantId) {
        List<SettlementRequest> settlements = settlementRepository.findByTenantOrderByCreatedAtDesc(tenantId);
        return com.krunity.HostelManagment.Mapper.SettlementMapper.toResponseDtoList(settlements);
    }

    private BigDecimal calculateOutstandingRent(String agreementId) {
        // Find the payment plan for this agreement
        Optional<TenantPaymentPlan> paymentPlanOpt = paymentPlanRepository.findByAgreementId(agreementId);
        
        if (paymentPlanOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        TenantPaymentPlan paymentPlan = paymentPlanOpt.get();
        List<InstallmentInvoice> overdueInvoices = installmentInvoiceRepository
                .findByPaymentPlanAndStatus(paymentPlan, com.krunity.HostelManagment.enums.InvoiceStatus.OVERDUE);
        
        return overdueInvoices.stream()
                .map(InstallmentInvoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateOutstandingCharges(UUID tenantId) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found"));
                
        List<OtherCharge> pendingCharges = otherChargeRepository
                .findByTenantAndPaymentStatusIn(tenant,
                    Arrays.asList(PaymentStatus.PENDING, PaymentStatus.OVERDUE));

        return pendingCharges.stream()
                .map(charge -> charge.getAmount().subtract(charge.getPaidAmount() != null ? charge.getPaidAmount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<SettlementCalculationDto.OutstandingItemDto> getOutstandingItemsDetails(Agreement agreement) {
        List<SettlementCalculationDto.OutstandingItemDto> items = new ArrayList<>();

        // Add overdue rent installments
        Optional<TenantPaymentPlan> paymentPlanOpt = paymentPlanRepository.findByAgreementId(agreement.getId());
        
        if (paymentPlanOpt.isPresent()) {
            TenantPaymentPlan paymentPlan = paymentPlanOpt.get();
            List<InstallmentInvoice> overdueInvoices = installmentInvoiceRepository
                    .findByPaymentPlanAndStatus(paymentPlan, com.krunity.HostelManagment.enums.InvoiceStatus.OVERDUE);

            for (InstallmentInvoice invoice : overdueInvoices) {
                items.add(SettlementCalculationDto.OutstandingItemDto.builder()
                        .type("RENT")
                        .description("Rent installment #" + invoice.getInstallmentNumber())
                        .amount(invoice.getTotalAmount())
                        .dueDate(invoice.getDueDate().toString())
                        .status(invoice.getStatus().toString())
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
        }

        return items;
    }
}