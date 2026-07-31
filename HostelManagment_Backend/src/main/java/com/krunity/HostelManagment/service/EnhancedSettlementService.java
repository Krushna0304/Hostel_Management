package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.SettlementCalculationResult;
import com.krunity.HostelManagment.dto.SettlementTransaction;
import com.krunity.HostelManagment.enums.NotificationType;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.enums.SettlementStatus;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.exception.SettlementCalculationException;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import com.krunity.HostelManagment.repository.SettlementRequestRepository;
import com.krunity.HostelManagment.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced Settlement Service that extends the existing settlement functionality
 * with new features for settlement transactions, early settlements, and room availability updates
 */
@Slf4j
@Service
public class EnhancedSettlementService {

    @Autowired
    private SettlementService baseSettlementService; // Existing settlement service

    @Autowired
    private SettlementTransactionCalculator settlementCalculator;

    @Autowired
    private SettlementRequestRepository settlementRepository;

    @Autowired
    private RoomAllotmentRepository roomAllotmentRepository;

    @Autowired
    private AgreementService agreementService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;



    // ─── Settlement Transaction Management ────────────────────────────────────

    /**
     * Creates settlement transaction at any time based on current plan
     */
    @Transactional
    public SettlementRequest createSettlementTransaction(String agreementId, 
                                                        UUID ownerId, 
                                                        LocalDate calculationDate,
                                                        String notes) {
        log.info("Creating settlement transaction for agreement {} by owner {} on date {}", 
                agreementId, ownerId, calculationDate);

        try {
            // Validate agreement ownership
            Agreement agreement = agreementService.getAgreementById(agreementId)
                    .orElseThrow(() -> new NotFoundException("Agreement not found with ID: " + agreementId));
            if (!agreement.getOwnerId().equals(ownerId)) {
                throw new SecurityException("Owner is not authorized for this agreement");
            }

            // Get room allotment
            RoomAllotment allotment = roomAllotmentRepository.findByAgreementId(agreementId)
                    .orElseThrow(() -> new NotFoundException("Room allotment not found for agreement"));

            // Calculate settlement
            SettlementCalculationResult calculationResult = settlementCalculator
                    .calculateSettlement(agreement, allotment, calculationDate);

            if (!calculationResult.isValid()) {
                throw new SettlementCalculationException("Settlement calculation failed: " + 
                        calculationResult.getErrorMessage());
            }

            // Get owner from agreement
            User owner = userRepository.findById(agreement.getOwnerId())
                    .orElseThrow(() -> new NotFoundException("Owner not found"));

            // Create settlement transaction
            SettlementTransaction transaction = settlementCalculator
                    .createSettlementTransaction(calculationResult, ownerId.toString(), notes);

            // Create settlement request with transaction data
            SettlementRequest settlementRequest = SettlementRequest.builder()
                    .agreementId(agreementId)
                    .tenant(allotment.getTenant())
                    .owner(owner)
                    .room(allotment.getRoom())
                    .status(SettlementStatus.SETTLEMENT_TRANSACTION_CREATED)
                    .settlementTransactionData(settlementCalculator.formatSettlementTransaction(transaction))
                    .transactionCreatedAt(LocalDateTime.now())
                    .securityDeposit(calculationResult.getSecurityDeposit())
                    .outstandingRent(calculationResult.getOutstandingRent())
                    .outstandingCharges(calculationResult.getOutstandingCharges())
                    .totalDeductions(calculationResult.getTotalDeductions())
                    .finalSettlementAmount(calculationResult.getFinalSettlementAmount())
                    .settlementType(calculationResult.getSettlementType())
                    .ownerNotes(notes)
                    .build();

            // Save settlement request
            settlementRequest = settlementRepository.save(settlementRequest);

            // Notify tenant immediately
            notifyTenantOfSettlementTransaction(settlementRequest);

            log.info("Settlement transaction created successfully: {}", settlementRequest.getSettlementId());
            return settlementRequest;

        } catch (Exception e) {
            log.error("Failed to create settlement transaction for agreement {}: {}", agreementId, e.getMessage(), e);
            throw new SettlementCalculationException("Failed to create settlement transaction", e);
        }
    }

    /**
     * Processes early settlement requests (before agreement end)
     */
    @Transactional
    public SettlementRequest processEarlySettlement(String agreementId,
                                                   UUID tenantId,
                                                   LocalDate requestedEndDate,
                                                   String tenantNotes) {
        log.info("Processing early settlement request for agreement {} by tenant {} with end date {}", 
                agreementId, tenantId, requestedEndDate);

        try {
            // Validate agreement and tenant
            Agreement agreement = agreementService.getAgreementById(agreementId)
                    .orElseThrow(() -> new NotFoundException("Agreement not found with ID: " + agreementId));
            if (!agreement.getUserId().equals(tenantId)) {
                throw new SecurityException("Tenant is not authorized for this agreement");
            }

            // Get room allotment
            RoomAllotment allotment = roomAllotmentRepository.findByAgreementId(agreementId)
                    .orElseThrow(() -> new NotFoundException("Room allotment not found for agreement"));

            // Validate early settlement eligibility
            validateEarlySettlementEligibility(allotment, requestedEndDate);

            // Check if settlement request already exists
            if (settlementRepository.findByAgreementId(agreementId).isPresent()) {
                throw new IllegalStateException("Settlement request already exists for this agreement");
            }

            // Calculate preliminary settlement (with early exit penalties)
            SettlementCalculationResult preliminaryCalculation = settlementCalculator
                    .calculateSettlement(agreement, allotment, requestedEndDate);

            // Get owner from agreement
            User owner = userRepository.findById(agreement.getOwnerId())
                    .orElseThrow(() -> new NotFoundException("Owner not found"));

            // Create early settlement request
            SettlementRequest settlementRequest = SettlementRequest.builder()
                    .agreementId(agreementId)
                    .tenant(allotment.getTenant())
                    .owner(owner)
                    .room(allotment.getRoom())
                    .status(SettlementStatus.PENDING_OWNER_REVIEW)
                    .requestedEndDate(requestedEndDate)
                    .earlySettlementRequested(true)
                    .tenantNotes(tenantNotes)
                    .build();

            // Save settlement request
            settlementRequest = settlementRepository.save(settlementRequest);

            // Update room allotment status
            allotment.setEarlyExit(true);
            allotment.setSettlementRequestedAt(LocalDateTime.now());
            allotment.updateStatus(RoomAllotmentStatus.SETTLEMENT_REQUESTED, "TENANT");
            roomAllotmentRepository.save(allotment);

            // Notify owner
            notifyOwnerOfEarlySettlementRequest(settlementRequest);

            log.info("Early settlement request created successfully: {}", settlementRequest.getSettlementId());
            return settlementRequest;

        } catch (Exception e) {
            log.error("Failed to process early settlement for agreement {}: {}", agreementId, e.getMessage(), e);
            throw new RuntimeException("Failed to process early settlement request", e);
        }
    }

    /**
     * Handles settlement approval with room availability update
     */
    @Transactional
    public SettlementRequest approveSettlementWithRoomUpdate(UUID settlementId,
                                                            UUID ownerId,
                                                            String ownerNotes,
                                                            boolean updateRoomAvailability) {
        log.info("Approving settlement {} by owner {} with room update: {}", 
                settlementId, ownerId, updateRoomAvailability);

        try {
            // Get settlement request
            SettlementRequest settlement = settlementRepository.findById(settlementId)
                    .orElseThrow(() -> new NotFoundException("Settlement request not found"));

            // Validate owner authorization
            if (!settlement.getOwner().getUserId().equals(ownerId)) {
                throw new SecurityException("Owner is not authorized for this settlement");
            }

            // Update settlement status
            User owner = settlement.getOwner();
            settlement.updateStatus(SettlementStatus.SETTLEMENT_APPROVED, owner);
            settlement.setOwnerNotes(ownerNotes);
            
            // Update room availability if requested
            if (updateRoomAvailability) {
                updateRoomAvailabilityAfterApproval(settlement);
                settlement.markRoomAvailabilityUpdated();
            }

            // Save settlement
            settlement = settlementRepository.save(settlement);

            // Update room allotment status
            RoomAllotment allotment = roomAllotmentRepository.findByAgreementId(settlement.getAgreementId())
                    .orElse(null);
            if (allotment != null) {
                allotment.setSettlementApprovedAt(LocalDateTime.now());
                allotment.updateStatus(RoomAllotmentStatus.ON_NOTICE_PERIOD, "OWNER");
                roomAllotmentRepository.save(allotment);
            }

            // Notify tenant of approval
            notifyTenantOfSettlementApproval(settlement);

            log.info("Settlement approved successfully: {}", settlement.getSettlementId());
            return settlement;

        } catch (Exception e) {
            log.error("Failed to approve settlement {}: {}", settlementId, e.getMessage(), e);
            throw new RuntimeException("Failed to approve settlement", e);
        }
    }

    // ─── Private Helper Methods ──────────────────────────────────────────────

    private void validateEarlySettlementEligibility(RoomAllotment allotment, LocalDate requestedEndDate) {
        // Check if early settlement is allowed from current status
        if (!RoomAllotmentStatus.settlementRequestableStatuses().contains(allotment.getRoomAllotmentStatus())) {
            throw new IllegalStateException("Settlement cannot be requested from current allotment status: " + 
                    allotment.getRoomAllotmentStatus());
        }

        // Validate requested end date
        if (requestedEndDate == null || requestedEndDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Requested end date cannot be in the past");
        }

        // Check if request is indeed early (before original end date)
        if (allotment.getEndDate() != null && !requestedEndDate.isBefore(allotment.getEndDate())) {
            throw new IllegalArgumentException("Requested end date is not before original agreement end date");
        }
    }

    private void updateRoomAvailabilityAfterApproval(SettlementRequest settlement) {
        try {
            RoomAllotment allotment = roomAllotmentRepository.findByAgreementId(settlement.getAgreementId())
                    .orElse(null);
            
            if (allotment != null) {
                // Update end date to requested end date if it's an early settlement
                if (settlement.isEarlySettlementRequested() && settlement.getRequestedEndDate() != null) {
                    allotment.setEndDate(settlement.getRequestedEndDate());
                    roomAllotmentRepository.save(allotment);
                    
                    log.info("Updated room allotment end date to {} for early settlement", 
                            settlement.getRequestedEndDate());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update room availability after settlement approval: {}", e.getMessage());
            // Don't fail the entire operation for room update issues
        }
    }

    // ─── Notification Methods ────────────────────────────────────────────────

    private void notifyTenantOfSettlementTransaction(SettlementRequest settlement) {
        try {
            // Use the existing settlement notification method or create a simple SMS
            String message = String.format("Settlement transaction created for agreement %s. " +
                    "Amount: ₹%.2f (%s). Please check your account for details.",
                    settlement.getAgreementId().substring(0, Math.min(8, settlement.getAgreementId().length())),
                    settlement.getFinalSettlementAmount(),
                    settlement.getSettlementType().equals("OWNER_PAYABLE") ? "Owner pays to you" : "You pay to owner"
            );
            notificationService.sendSms(settlement.getTenant().getPhoneNumber(), message);
        } catch (Exception e) {
            log.warn("Failed to send settlement transaction notification: {}", e.getMessage());
        }
    }

    private void notifyOwnerOfEarlySettlementRequest(SettlementRequest settlement) {
        try {
            // Use existing settlement request notification
            notificationService.sendSettlementRequestNotification(
                    settlement.getOwner(), 
                    settlement.getTenant(), 
                    settlement
            );
        } catch (Exception e) {
            log.warn("Failed to send early settlement request notification: {}", e.getMessage());
        }
    }

    private void notifyTenantOfSettlementApproval(SettlementRequest settlement) {
        try {
            // Use existing settlement approval notification
            notificationService.sendSettlementApprovalNotification(settlement.getTenant(), settlement);
        } catch (Exception e) {
            log.warn("Failed to send settlement approval notification: {}", e.getMessage());
        }
    }



    // ─── Query Methods ───────────────────────────────────────────────────────

    /**
     * Gets all settlement requests with transaction data
     */
    public List<SettlementRequest> getSettlementsWithTransactionData() {
        // TODO: Implement proper repository method
        return settlementRepository.findAll().stream()
                .filter(settlement -> settlement.getSettlementTransactionData() != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    /**
     * Gets early settlement requests for an owner
     */
    public List<SettlementRequest> getEarlySettlementRequestsForOwner(UUID ownerId) {
        // TODO: Implement proper repository method
        return settlementRepository.findAll().stream()
                .filter(settlement -> settlement.getOwner().getUserId().equals(ownerId))
                .filter(SettlementRequest::isEarlySettlementRequested)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    /**
     * Gets settlements that need room availability updates
     */
    public List<SettlementRequest> getSettlementsNeedingRoomUpdates() {
        // TODO: Implement proper repository method
        return settlementRepository.findAll().stream()
                .filter(settlement -> settlement.getStatus() == SettlementStatus.SETTLEMENT_APPROVED)
                .filter(settlement -> !settlement.isRoomAvailabilityUpdated())
                .toList();
    }

    /**
     * Parses settlement transaction data from settlement request
     */
    public SettlementTransaction getSettlementTransaction(SettlementRequest settlement) {
        if (settlement.getSettlementTransactionData() == null) {
            return null;
        }
        return settlementCalculator.parseSettlementData(settlement.getSettlementTransactionData());
    }
}