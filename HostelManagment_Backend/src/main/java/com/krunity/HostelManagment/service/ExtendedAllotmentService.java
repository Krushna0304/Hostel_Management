package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.ExtensionRequestDto;
import com.krunity.HostelManagment.dto.ExtensionApprovalDto;
import com.krunity.HostelManagment.dto.PaymentDetailsDto;
import com.krunity.HostelManagment.enums.ExtendAllotmentStatus;
import com.krunity.HostelManagment.enums.NotificationType;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.exception.ConflictException;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.model.plan.Duration;
import com.krunity.HostelManagment.repository.ExtendAllotmentRequestRepository;
import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import com.krunity.HostelManagment.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling extend allotment requests and workflow
 * Manages the complete lifecycle from request creation to payment completion
 */
@Slf4j
@Service
public class ExtendedAllotmentService {

    @Autowired
    private ExtendAllotmentRequestRepository extendRequestRepository;

    @Autowired
    private RoomAllotmentRepository roomAllotmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgreementService agreementService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SettlementTransactionCalculator settlementCalculator;

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private RoomAgreementPlanService roomAgreementPlanService;

    // ─── Extension Request Creation ──────────────────────────────────────────

    /**
     * Creates extension request for tenant's current allotment
     */
    @Transactional
    public ExtendAllotmentRequest createExtensionRequest(UUID tenantId, ExtensionRequestDto requestDto) {
        log.info("Creating extension request for tenant {} and agreement {}", 
                tenantId, requestDto.getCurrentAgreementId());

        try {
            // Validate current active agreement
            Agreement currentAgreement = agreementService.getAgreementById(requestDto.getCurrentAgreementId())
                    .orElseThrow(() -> new NotFoundException("Agreement not found: " + requestDto.getCurrentAgreementId()));
            validateAgreementOwnership(currentAgreement, tenantId);

            // Get current room allotment
            RoomAllotment currentAllotment = roomAllotmentRepository
                    .findByAgreementId(requestDto.getCurrentAgreementId())
                    .orElseThrow(() -> new NotFoundException("Current allotment not found"));

            // Validate extension eligibility
            validateExtensionEligibility(currentAllotment, tenantId);

            // Check for existing active extension request
            if (extendRequestRepository.hasActiveTenantExtensionRequest(
                    currentAllotment.getTenant(), requestDto.getCurrentAgreementId())) {
                throw new ConflictException("Active extension request already exists for this agreement");
            }

            // Get tenant and owner
            User tenant = currentAllotment.getTenant();
            User owner = userRepository.findById(currentAgreement.getOwnerId())
                    .orElseThrow(() -> new NotFoundException("Owner not found"));

            // Calculate extension dates (seamless transition)
            LocalDate extensionStartDate = currentAllotment.getEndDate();
            LocalDate extensionEndDate = calculateExtensionEndDate(extensionStartDate, requestDto.getPlanId());

            // Validate no overlap with existing agreements
            validateNoOverlappingAgreements(tenant, currentAllotment.getRoom(), 
                    extensionStartDate, extensionEndDate);

            // Calculate amounts
            BigDecimal activationAmount = calculateActivationAmount(requestDto.getPlanId());
            BigDecimal settlementAdjustment = calculateSettlementAdjustment(currentAgreement, currentAllotment);
            
            // Create extension request
            ExtendAllotmentRequest extensionRequest = ExtendAllotmentRequest.builder()
                    .tenant(tenant)
                    .owner(owner)
                    .currentAgreementId(requestDto.getCurrentAgreementId())
                    .currentRoom(currentAllotment.getRoom())
                    .newPlanId(requestDto.getPlanId())
                    .extensionStartDate(extensionStartDate)
                    .extensionEndDate(extensionEndDate)
                    .status(ExtendAllotmentStatus.PENDING_OWNER_APPROVAL)
                    .activationAmount(activationAmount)
                    .settlementAdjustment(settlementAdjustment)
                    .tenantNotes(requestDto.getTenantNotes())
                    .build();

            // Calculate total amount
            extensionRequest.calculateTotalAmount();

            // Set expiration (7 days from now for owner response)
            extensionRequest.setPaymentExpirationFromNow(7 * 24); // 7 days

            // Save extension request
            extensionRequest = extendRequestRepository.save(extensionRequest);

            // Send owner notification
            sendOwnerNotification(extensionRequest, NotificationType.EXTENSION_REQUEST_CREATED);

            log.info("Extension request created successfully: {}", extensionRequest.getExtendRequestId());
            return extensionRequest;

        } catch (Exception e) {
            log.error("Failed to create extension request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create extension request: " + e.getMessage(), e);
        }
    }

    /**
     * Owner approves extension request
     */
    @Transactional
    public ExtendAllotmentRequest approveExtensionRequest(UUID requestId, UUID ownerId, 
                                                         ExtensionApprovalDto approvalDto) {
        log.info("Approving extension request {} by owner {}", requestId, ownerId);

        try {
            // Get extension request
            ExtendAllotmentRequest extensionRequest = extendRequestRepository.findById(requestId)
                    .orElseThrow(() -> new NotFoundException("Extension request not found"));

            // Validate ownership and status
            validateOwnershipAndStatus(extensionRequest, ownerId, ExtendAllotmentStatus.PENDING_OWNER_APPROVAL);

            // Update extension request
            extensionRequest.updateStatus(ExtendAllotmentStatus.APPROVED_PENDING_PAYMENT);
            extensionRequest.setOwnerNotes(approvalDto.getOwnerNotes());
            
            // Update final activation amount if provided
            if (approvalDto.getFinalActivationAmount() != null) {
                extensionRequest.setActivationAmount(approvalDto.getFinalActivationAmount());
                extensionRequest.calculateTotalAmount();
            }

            // Create new agreement in DRAFT status
            String newAgreementId = createNewDraftAgreement(extensionRequest);
            extensionRequest.setNewAgreementId(newAgreementId);

            // Set payment deadline (72 hours from approval)
            extensionRequest.setPaymentExpirationFromNow(72);

            // Save extension request
            extensionRequest = extendRequestRepository.save(extensionRequest);

            // Send tenant notification
            sendTenantNotification(extensionRequest, NotificationType.EXTENSION_REQUEST_APPROVED);

            log.info("Extension request approved successfully: {}", extensionRequest.getExtendRequestId());
            return extensionRequest;

        } catch (NotFoundException | SecurityException | IllegalStateException e) {
            log.error("Failed to approve extension request: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to approve extension request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to approve extension request: " + e.getMessage(), e);
        }
    }

    /**
     * Tenant accepts and pays for extension
     */
    @Transactional
    public ExtendAllotmentRequest processExtensionPayment(UUID requestId, UUID tenantId, 
                                                         PaymentDetailsDto paymentDto) {
        log.info("Processing extension payment for request {} by tenant {}", requestId, tenantId);

        try {
            // Get extension request
            ExtendAllotmentRequest extensionRequest = extendRequestRepository.findById(requestId)
                    .orElseThrow(() -> new NotFoundException("Extension request not found"));

            // Validate tenant ownership and status
            validateTenantOwnershipAndStatus(extensionRequest, tenantId, 
                    ExtendAllotmentStatus.APPROVED_PENDING_PAYMENT);

            // Validate payment is within deadline
            if (extensionRequest.isExpired()) {
                extensionRequest.updateStatus(ExtendAllotmentStatus.EXPIRED);
                extendRequestRepository.save(extensionRequest);
                throw new IllegalStateException("Extension request has expired");
            }

            // Process payment
            boolean paymentSuccess = processPayment(extensionRequest, paymentDto);
            if (!paymentSuccess) {
                throw new RuntimeException("Payment processing failed");
            }

            // Update extension request status
            extensionRequest.updateStatus(ExtendAllotmentStatus.PAYMENT_COMPLETED);
            extensionRequest.setPaymentReference(paymentDto.getPaymentReference());

            // Activate new agreement
            activateNewAgreement(extensionRequest);
            extensionRequest.updateStatus(ExtendAllotmentStatus.AGREEMENT_CREATED);

            // Create new room allotment
            createNewRoomAllotment(extensionRequest);

            // Update extension request to active
            extensionRequest.updateStatus(ExtendAllotmentStatus.ACTIVE);

            // Save extension request
            extensionRequest = extendRequestRepository.save(extensionRequest);

            // Send confirmation notifications
            sendTenantNotification(extensionRequest, NotificationType.EXTENSION_PAYMENT_COMPLETED);
            sendOwnerNotification(extensionRequest, NotificationType.EXTENSION_PAYMENT_COMPLETED);

            log.info("Extension payment processed successfully: {}", extensionRequest.getExtendRequestId());
            return extensionRequest;

        } catch (Exception e) {
            log.error("Failed to process extension payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process extension payment: " + e.getMessage(), e);
        }
    }

    // ─── Query Methods ───────────────────────────────────────────────────────

    /**
     * Get extension requests for tenant
     */
    public List<ExtendAllotmentRequest> getTenantExtensionRequests(UUID tenantId) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found"));
        return extendRequestRepository.findByTenantOrderByCreatedAtDesc(tenant);
    }

    /**
     * Get extension requests for owner
     */
    public List<ExtendAllotmentRequest> getOwnerExtensionRequests(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner not found"));
        return extendRequestRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    /**
     * Get pending approval requests for owner
     */
    public List<ExtendAllotmentRequest> getPendingApprovalRequests(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner not found"));
        return extendRequestRepository.findPendingOwnerApprovalRequests(owner);
    }

    // ─── Private Helper Methods ──────────────────────────────────────────────

    private void validateAgreementOwnership(Agreement agreement, UUID tenantId) {
        if (!agreement.getUserId().equals(tenantId)) {
            throw new SecurityException("Tenant is not authorized for this agreement");
        }
    }

    private void validateExtensionEligibility(RoomAllotment allotment, UUID tenantId) {
        if (!allotment.getTenant().getUserId().equals(tenantId)) {
            throw new SecurityException("Tenant is not authorized for this allotment");
        }

        if (!allotment.canRequestExtension()) {
            throw new IllegalStateException("Extension cannot be requested from current allotment status: " + 
                    allotment.getRoomAllotmentStatus());
        }
    }

    private void validateOwnershipAndStatus(ExtendAllotmentRequest request, UUID ownerId, 
                                          ExtendAllotmentStatus expectedStatus) {
        if (!request.getOwner().getUserId().equals(ownerId)) {
            throw new SecurityException("Owner is not authorized for this extension request");
        }

        if (request.getStatus() != expectedStatus) {
            throw new IllegalStateException("Extension request is not in expected status: " + expectedStatus);
        }
    }

    private void validateTenantOwnershipAndStatus(ExtendAllotmentRequest request, UUID tenantId, 
                                                ExtendAllotmentStatus expectedStatus) {
        if (!request.getTenant().getUserId().equals(tenantId)) {
            throw new SecurityException("Tenant is not authorized for this extension request");
        }

        if (request.getStatus() != expectedStatus) {
            throw new IllegalStateException("Extension request is not in expected status: " + expectedStatus);
        }
    }

    /**
     * Calculate extension end date based on plan duration with proper handling of different duration types
     * Implements seamless transition date calculation ensuring no gaps or overlaps
     * 
     * @param startDate The extension start date (current agreement end date)
     * @param planId The plan ID for the extension
     * @return The calculated extension end date
     */
    private LocalDate calculateExtensionEndDate(LocalDate startDate, UUID planId) {
        log.debug("Calculating extension end date for plan {} starting from {}", planId, startDate);
        
        try {
            // Get the plan details
            RoomAgreementPlan plan = roomAgreementPlanService.getPlanById(planId.toString());
            
            // Get duration configuration from plan
            Duration duration = plan.getDuration();
            if (duration == null) {
                log.warn("Plan {} has no duration configuration, defaulting to 12 months", planId);
                return startDate.plusMonths(12);
            }
            
            // Calculate end date based on duration unit and value
            LocalDate endDate = calculateEndDateFromDuration(startDate, duration);
            
            // Handle edge cases for month-end dates
            endDate = handleMonthEndEdgeCases(startDate, endDate, duration);
            
            log.debug("Calculated extension end date: {} for plan {} (duration: {} {})", 
                    endDate, planId, duration.getValue(), duration.getUnit());
            
            return endDate;
            
        } catch (Exception e) {
            log.error("Failed to calculate extension end date for plan {}: {}", planId, e.getMessage(), e);
            
            // Fallback to 12 months if calculation fails
            LocalDate fallbackEndDate = startDate.plusMonths(12);
            log.warn("Using fallback end date: {} due to calculation failure", fallbackEndDate);
            return fallbackEndDate;
        }
    }
    
    /**
     * Calculate end date based on duration configuration
     */
    private LocalDate calculateEndDateFromDuration(LocalDate startDate, Duration duration) {
        String unit = duration.getUnit() != null ? duration.getUnit().toUpperCase() : "MONTH";
        Integer value = duration.getValue() != null ? duration.getValue() : 12;
        
        switch (unit) {
            case "MONTH":
            case "MONTHS":
                return startDate.plusMonths(value);
                
            case "YEAR":
            case "YEARS":
                return startDate.plusYears(value);
                
            case "DAY":
            case "DAYS":
                return startDate.plusDays(value);
                
            case "WEEK":
            case "WEEKS":
                return startDate.plusWeeks(value);
                
            default:
                log.warn("Unsupported duration unit: {}, defaulting to months", unit);
                return startDate.plusMonths(value);
        }
    }
    
    /**
     * Handle edge cases for month-end dates to ensure consistent behavior
     * For example, if start date is Jan 31 and we add 1 month, 
     * we want to handle leap years and months with different day counts properly
     */
    private LocalDate handleMonthEndEdgeCases(LocalDate startDate, LocalDate calculatedEndDate, Duration duration) {
        // If duration is in months and original start date was at month end,
        // try to maintain the month-end pattern
        if ("MONTH".equalsIgnoreCase(duration.getUnit()) || "MONTHS".equalsIgnoreCase(duration.getUnit())) {
            
            // Check if start date is the last day of the month
            if (startDate.equals(startDate.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()))) {
                // Make the end date also the last day of its month
                return calculatedEndDate.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
            }
            
            // For leap year handling: if original date was Feb 29 and target month doesn't have 29 days,
            // adjust to the last day of that month
            if (startDate.getMonthValue() == 2 && startDate.getDayOfMonth() == 29) {
                int targetMonth = calculatedEndDate.getMonthValue();
                int targetYear = calculatedEndDate.getYear();
                
                // Check if target month can accommodate day 29
                java.time.YearMonth targetYearMonth = java.time.YearMonth.of(targetYear, targetMonth);
                if (targetYearMonth.lengthOfMonth() < 29) {
                    return calculatedEndDate.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
                }
            }
        }
        
        return calculatedEndDate;
    }

    private void validateNoOverlappingAgreements(User tenant, Room room, 
                                               LocalDate startDate, LocalDate endDate) {
        // Check for overlapping agreements for this tenant in any room
        // Implementation would check existing active agreements
        log.debug("Validating no overlapping agreements for tenant {} from {} to {}", 
                tenant.getUserId(), startDate, endDate);
    }

    private BigDecimal calculateActivationAmount(UUID planId) {
        // This would get plan details and calculate activation amount
        // For now, return a default amount
        return BigDecimal.valueOf(5000.00); // Default activation amount
    }

    private BigDecimal calculateSettlementAdjustment(Agreement agreement, RoomAllotment allotment) {
        // This would calculate any settlement adjustments based on current agreement
        // For now, return zero
        return BigDecimal.ZERO;
    }

    private String createNewDraftAgreement(ExtendAllotmentRequest extensionRequest) {
        log.info("Creating new draft agreement for extension request: {}", extensionRequest.getExtendRequestId());
        
        try {
            // Get the plan details
            RoomAgreementPlan planSnapshot = roomAgreementPlanService.getPlanById(extensionRequest.getNewPlanId().toString());
            
            // Create new agreement for extension
            Agreement newAgreement = Agreement.builder()
                    .type(com.krunity.HostelManagment.enums.AgreementType.ROOM) // Extensions are for room agreements
                    .userId(extensionRequest.getTenant().getUserId())
                    .roomId(extensionRequest.getCurrentRoom().getRoomId())
                    .ownerId(extensionRequest.getOwner().getUserId())
                    .planId(extensionRequest.getNewPlanId().toString())
                    .planSnapshot(planSnapshot)
                    .startDate(extensionRequest.getExtensionStartDate())
                    .endDate(extensionRequest.getExtensionEndDate())
                    .status(com.krunity.HostelManagment.enums.AgreementStatus.DRAFT) // Start in DRAFT status
                    .qrUsed(false)
                    .createdAt(java.time.Instant.now())
                    .build();
            
            // Use AgreementService to create the agreement (this handles MongoDB storage)
            Agreement savedAgreement = agreementService.createAgreement(newAgreement);
            
            log.info("Successfully created draft agreement: {} for extension request: {}", 
                    savedAgreement.getId(), extensionRequest.getExtendRequestId());
            
            return savedAgreement.getId();
            
        } catch (Exception e) {
            log.error("Failed to create draft agreement for extension request {}: {}", 
                    extensionRequest.getExtendRequestId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create draft agreement: " + e.getMessage(), e);
        }
    }

    private boolean processPayment(ExtendAllotmentRequest extensionRequest, PaymentDetailsDto paymentDto) {
        // This would integrate with the payment service to process payment
        // For now, return true (assuming successful payment)
        log.info("Processing payment for extension request: {}, Amount: {}", 
                extensionRequest.getExtendRequestId(), extensionRequest.getTotalAmount());
        return true;
    }

    private void activateNewAgreement(ExtendAllotmentRequest extensionRequest) {
        log.info("Activating new agreement: {}", extensionRequest.getNewAgreementId());
        
        try {
            // Get the agreement from MongoDB
            Agreement agreement = agreementService.getAgreementById(extensionRequest.getNewAgreementId())
                    .orElseThrow(() -> new NotFoundException("New agreement not found: " + extensionRequest.getNewAgreementId()));
            
            // Update status to PENDING_TENANT_ACTION (ready for tenant to see and accept)
            agreement.setStatus(com.krunity.HostelManagment.enums.AgreementStatus.PENDING_TENANT_ACTION);
            agreement.setActivatedAt(java.time.Instant.now());
            
            // Generate QR token for tenant activation
            String qrToken = generateQrToken();
            agreement.setQrToken(qrToken);
            agreement.setQrExpiry(java.time.Instant.now().plus(72, java.time.temporal.ChronoUnit.HOURS)); // 72 hours expiry
            
            // Save the updated agreement
            agreementService.createAgreement(agreement);
            
            log.info("Successfully activated agreement: {} for extension request: {}", 
                    extensionRequest.getNewAgreementId(), extensionRequest.getExtendRequestId());
            
        } catch (Exception e) {
            log.error("Failed to activate agreement {} for extension request {}: {}", 
                    extensionRequest.getNewAgreementId(), extensionRequest.getExtendRequestId(), e.getMessage(), e);
            throw new RuntimeException("Failed to activate agreement: " + e.getMessage(), e);
        }
    }
    
    private String generateQrToken() {
        return UUID.randomUUID().toString().replace("-", "") + 
               java.time.Instant.now().toEpochMilli();
    }

    private void createNewRoomAllotment(ExtendAllotmentRequest extensionRequest) {
        // Get current allotment
        RoomAllotment currentAllotment = roomAllotmentRepository
                .findByAgreementId(extensionRequest.getCurrentAgreementId())
                .orElseThrow(() -> new NotFoundException("Current allotment not found"));

        // Create new allotment as extension
        RoomAllotment newAllotment = currentAllotment.createExtension(
                extensionRequest,
                extensionRequest.getNewAgreementId(),
                currentAllotment.getPaymentPlanId(), // Reuse or create new payment plan
                currentAllotment.getDepositTransactionId() // Reuse or create new deposit transaction
        );

        roomAllotmentRepository.save(newAllotment);
        log.info("Created new room allotment for extension: {}", newAllotment.getAllotmentId());
    }

    private void sendTenantNotification(ExtendAllotmentRequest extensionRequest, NotificationType type) {
        try {
            String message = createNotificationMessage(extensionRequest, type, true);
            notificationService.sendNotification(
                    extensionRequest.getTenant(),
                    type,
                    type.getDisplayName(),
                    message,
                    createNotificationVariables(extensionRequest)
            );
        } catch (Exception e) {
            log.warn("Failed to send tenant notification: {}", e.getMessage());
        }
    }

    private void sendOwnerNotification(ExtendAllotmentRequest extensionRequest, NotificationType type) {
        try {
            String message = createNotificationMessage(extensionRequest, type, false);
            notificationService.sendNotification(
                    extensionRequest.getOwner(),
                    type,
                    type.getDisplayName(),
                    message,
                    createNotificationVariables(extensionRequest)
            );
        } catch (Exception e) {
            log.warn("Failed to send owner notification: {}", e.getMessage());
        }
    }

    private String createNotificationMessage(ExtendAllotmentRequest extensionRequest, 
                                           NotificationType type, boolean isForTenant) {
        switch (type) {
            case EXTENSION_REQUEST_CREATED:
                return String.format("Extension request created for room %s from %s to %s. Amount: ₹%.2f",
                        extensionRequest.getCurrentRoom().getRoomNumber(),
                        extensionRequest.getExtensionStartDate(),
                        extensionRequest.getExtensionEndDate(),
                        extensionRequest.getTotalAmount());
            case EXTENSION_REQUEST_APPROVED:
                return String.format("Your extension request has been approved! Please pay ₹%.2f by %s to confirm.",
                        extensionRequest.getTotalAmount(),
                        extensionRequest.getExpiresAt());
            case EXTENSION_PAYMENT_COMPLETED:
                return isForTenant ? 
                        "Extension payment completed successfully! Your new accommodation period is now active." :
                        String.format("Extension payment received from %s for room %s.",
                                extensionRequest.getTenant().getDisplayName(),
                                extensionRequest.getCurrentRoom().getRoomNumber());
            default:
                return "Extension request notification";
        }
    }

    private java.util.Map<String, String> createNotificationVariables(ExtendAllotmentRequest extensionRequest) {
        java.util.Map<String, String> variables = new java.util.HashMap<>();
        variables.put("tenantName", extensionRequest.getTenant().getDisplayName());
        variables.put("ownerName", extensionRequest.getOwner().getDisplayName());
        variables.put("roomNumber", extensionRequest.getCurrentRoom().getRoomNumber());
        variables.put("extensionStartDate", extensionRequest.getExtensionStartDate().toString());
        variables.put("extensionEndDate", extensionRequest.getExtensionEndDate().toString());
        variables.put("totalAmount", extensionRequest.getTotalAmount().toString());
        variables.put("paymentDeadline", extensionRequest.getExpiresAt() != null ? 
                extensionRequest.getExpiresAt().toString() : "TBD");
        return variables;
    }
}