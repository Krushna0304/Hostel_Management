package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.dto.ExtensionApprovalDto;
import com.krunity.HostelManagment.dto.ExtensionRequestDto;
import com.krunity.HostelManagment.dto.PaymentDetailsDto;
import com.krunity.HostelManagment.model.ExtendAllotmentRequest;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import com.krunity.HostelManagment.service.AgreementService;
import com.krunity.HostelManagment.service.ExtendedAllotmentService;
import com.krunity.HostelManagment.service.RoomAgreementPlanService;
import com.krunity.HostelManagment.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Extended Allotment functionality
 * Handles extension requests, approvals, and payments
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/allotments")
public class ExtendedAllotmentController {

    @Autowired
    private ExtendedAllotmentService extendedAllotmentService;

    @Autowired
    private RoomAgreementPlanService roomAgreementPlanService;

    @Autowired
    private AgreementService agreementService;

    // ─── Extension Request Endpoints ─────────────────────────────────────────

    /**
     * Create extension request for current allotment
     */
    @PostMapping("/extend")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<?> createExtensionRequest(@Valid @RequestBody ExtensionRequestDto request) {
        
        try {
            log.info("Creating extension request for agreement {} by tenant {}", 
                    request.getCurrentAgreementId(), SecurityUtils.getCurrentUserId());
            
            ExtendAllotmentRequest extensionRequest = extendedAllotmentService.createExtensionRequest(
                    SecurityUtils.getCurrentUserId(),
                    request
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(
                createExtensionRequestResponse(extensionRequest)
            );
            
        } catch (SecurityException e) {
            log.warn("Unauthorized extension request attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("ACCESS_DENIED", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Invalid extension request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create extension request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("EXTENSION_REQUEST_FAILED", e.getMessage()));
        }
    }

    /**
     * Owner approve extension request
     */
    @PutMapping("/extend/{requestId}/approve")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> approveExtensionRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody ExtensionApprovalDto approval) {
        
        try {
            log.info("Approving extension request {} by owner {}", 
                    requestId, SecurityUtils.getCurrentUserId());
            
            ExtendAllotmentRequest extensionRequest = extendedAllotmentService.approveExtensionRequest(
                    requestId,
                    SecurityUtils.getCurrentUserId(),
                    approval
            );
            
            return ResponseEntity.ok(createExtensionApprovalResponse(extensionRequest));
            
        } catch (SecurityException e) {
            log.warn("Unauthorized extension approval attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("ACCESS_DENIED", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Invalid extension approval: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_STATE", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to approve extension request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("APPROVAL_FAILED", e.getMessage()));
        }
    }

    /**
     * Process extension payment
     */
    @PostMapping("/extend/{requestId}/payment")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<?> processExtensionPayment(
            @PathVariable UUID requestId,
            @Valid @RequestBody PaymentDetailsDto payment) {
        
        try {
            log.info("Processing extension payment for request {} by tenant {}", 
                    requestId, SecurityUtils.getCurrentUserId());
            
            ExtendAllotmentRequest extensionRequest = extendedAllotmentService.processExtensionPayment(
                    requestId,
                    SecurityUtils.getCurrentUserId(),
                    payment
            );
            
            return ResponseEntity.ok(createExtensionPaymentResponse(extensionRequest));
            
        } catch (SecurityException e) {
            log.warn("Unauthorized extension payment attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("ACCESS_DENIED", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Invalid extension payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_STATE", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to process extension payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("PAYMENT_FAILED", e.getMessage()));
        }
    }

    // ─── Query Endpoints ─────────────────────────────────────────────────────

    /**
     * Get extension requests for current tenant
     */
    @GetMapping("/extend/tenant")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<?> getTenantExtensionRequests() {
        
        try {
            UUID tenantId = SecurityUtils.getCurrentUserId();
            List<ExtendAllotmentRequest> requests = extendedAllotmentService.getTenantExtensionRequests(tenantId);
            
            return ResponseEntity.ok().body(java.util.Map.of(
                "extensionRequests", requests.stream().map(this::createExtensionSummary).toList(),
                "count", requests.size()
            ));
            
        } catch (Exception e) {
            log.error("Failed to get tenant extension requests: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("REQUESTS_RETRIEVAL_FAILED", e.getMessage()));
        }
    }

    /**
     * Get extension requests for a specific tenant (admin/owner access)
     */
    @GetMapping("/extend/tenant/{tenantId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> getTenantExtensionRequestsById(@PathVariable UUID tenantId) {
        
        try {
            log.info("Getting extension requests for tenant {} by user {}", 
                    tenantId, SecurityUtils.getCurrentUserId());
            
            List<ExtendAllotmentRequest> requests = extendedAllotmentService.getTenantExtensionRequests(tenantId);
            
            return ResponseEntity.ok().body(java.util.Map.of(
                "tenantId", tenantId,
                "extensionRequests", requests.stream().map(this::createExtensionSummary).toList(),
                "count", requests.size()
            ));
            
        } catch (Exception e) {
            log.error("Failed to get extension requests for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("REQUESTS_RETRIEVAL_FAILED", e.getMessage()));
        }
    }

    /**
     * Get extension requests for current owner
     */
    @GetMapping("/extend/owner")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> getOwnerExtensionRequests() {
        
        try {
            UUID ownerId = SecurityUtils.getCurrentUserId();
            List<ExtendAllotmentRequest> requests = extendedAllotmentService.getOwnerExtensionRequests(ownerId);
            
            return ResponseEntity.ok().body(java.util.Map.of(
                "extensionRequests", requests.stream().map(this::createExtensionSummary).toList(),
                "count", requests.size()
            ));
            
        } catch (Exception e) {
            log.error("Failed to get owner extension requests: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("REQUESTS_RETRIEVAL_FAILED", e.getMessage()));
        }
    }

    /**
     * Get pending approval requests for current owner
     */
    @GetMapping("/extend/pending-approvals")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> getPendingApprovalRequests() {
        
        try {
            UUID ownerId = SecurityUtils.getCurrentUserId();
            List<ExtendAllotmentRequest> pendingRequests = 
                    extendedAllotmentService.getPendingApprovalRequests(ownerId);
            
            return ResponseEntity.ok().body(java.util.Map.of(
                "pendingRequests", pendingRequests.stream().map(this::createPendingApprovalSummary).toList(),
                "count", pendingRequests.size()
            ));
            
        } catch (Exception e) {
            log.error("Failed to get pending approval requests: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("PENDING_REQUESTS_RETRIEVAL_FAILED", e.getMessage()));
        }
    }

    /**
     * Get extension request details
     */
    @GetMapping("/extend/{requestId}")
    @PreAuthorize("hasRole('TENANT') or hasRole('OWNER')")
    public ResponseEntity<?> getExtensionRequestDetails(@PathVariable UUID requestId) {
        
        try {
            // Implementation would include authorization checks and detailed response
            return ResponseEntity.ok().body(java.util.Map.of(
                "requestId", requestId,
                "message", "Extension request details retrieved successfully"
            ));
            
        } catch (Exception e) {
            log.error("Failed to get extension request details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("REQUEST_DETAILS_RETRIEVAL_FAILED", e.getMessage()));
        }
    }

    // ─── Helper Methods ──────────────────────────────────────────────────────

    private java.util.Map<String, Object> createExtensionRequestResponse(ExtendAllotmentRequest request) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("requestId", request.getExtendRequestId());
        response.put("status", request.getStatus());
        response.put("currentAgreementId", request.getCurrentAgreementId());
        response.put("extensionStartDate", request.getExtensionStartDate());
        response.put("extensionEndDate", request.getExtensionEndDate());
        response.put("activationAmount", request.getActivationAmount());
        response.put("activationAmountBeforeAdjustment", request.getActivationAmountBeforeAdjustment());
        response.put("previousAgreementRefundableAmount", request.getPreviousAgreementRefundableAmount());
        response.put("settlementAdjustment", request.getSettlementAdjustment());
        response.put("totalAmount", request.getTotalAmount());
        response.put("expiresAt", request.getExpiresAt());
        response.put("createdAt", request.getCreatedAt());
        response.put("message", "Extension request sent to owner for approval");
        return response;
    }

    private java.util.Map<String, Object> createExtensionApprovalResponse(ExtendAllotmentRequest request) {
        return java.util.Map.of(
            "requestId", request.getExtendRequestId(),
            "status", request.getStatus(),
            "newAgreementId", request.getNewAgreementId(),
            "paymentAmount", request.getTotalAmount(),
            "paymentDeadline", request.getExpiresAt(),
            "approvedAt", request.getApprovedAt(),
            "message", "Extension approved. New agreement created and visible to tenant."
        );
    }

    private java.util.Map<String, Object> createExtensionPaymentResponse(ExtendAllotmentRequest request) {
        return java.util.Map.of(
            "requestId", request.getExtendRequestId(),
            "status", request.getStatus(),
            "newAgreementId", request.getNewAgreementId(),
            "paymentAmount", request.getTotalAmount(),
            "paymentReference", request.getPaymentReference(),
            "paymentCompletedAt", request.getPaymentCompletedAt(),
            "extensionActive", request.getStatus().toString().equals("ACTIVE"),
            "message", "Extension payment completed. Please accept the generated agreement to finish the extension."
        );
    }

    private java.util.Map<String, Object> createExtensionSummary(ExtendAllotmentRequest request) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", request.getExtendRequestId());
        response.put("requestId", request.getExtendRequestId());
        response.put("currentAgreementId", request.getCurrentAgreementId());
        response.put("newAgreementId", request.getNewAgreementId() != null ? request.getNewAgreementId() : "");
        response.put("status", request.getStatus());
        response.put("extensionPeriod", request.getExtensionStartDate() + " to " + request.getExtensionEndDate());
        response.put("currentEndDate", request.getExtensionStartDate());
        response.put("totalAmount", request.getTotalAmount());
        response.put("activationAmountBeforeAdjustment", request.getActivationAmountBeforeAdjustment());
        response.put("previousAgreementRefundableAmount", request.getPreviousAgreementRefundableAmount());
        response.put("createdAt", request.getCreatedAt());
        response.put("expiresAt", request.getExpiresAt());
        response.put("tenantNotes", request.getTenantNotes());

        if (request.getTenant() != null) {
            response.put("tenantName", request.getTenant().getDisplayName());
            response.put("tenantMobileNumber", request.getTenant().getPhoneNumber());
        }
        if (request.getCurrentRoom() != null) {
            response.put("roomNumber", request.getCurrentRoom().getRoomNumber());
            if (request.getCurrentRoom().getFloor() != null) {
                response.put("floorNumber", request.getCurrentRoom().getFloor().getFloorNumber());
            }
            if (request.getCurrentRoom().getHostel() != null) {
                response.put("hostelName", request.getCurrentRoom().getHostel().getHostelName());
            }
        }
        if (request.getNewPlanId() != null) {
            RoomAgreementPlan plan = roomAgreementPlanService.getPlanById(request.getNewPlanId());
            response.put("planName", plan.getPlanName());
            response.put("planDuration", plan.getDuration() != null
                    ? plan.getDuration().getValue() + " " + plan.getDuration().getUnit()
                    : null);
            response.put("monthlyRent", plan.getRentDetails() != null ? plan.getRentDetails().getMonthlyRent() : null);
            response.put("securityDeposit", plan.getCharges() != null && plan.getCharges().getSecurityDeposit() != null
                    ? plan.getCharges().getSecurityDeposit().getAmount() : null);
        }
        if (request.getNewAgreementId() != null) {
            agreementService.getAgreementById(request.getNewAgreementId())
                    .map(Agreement::getQrToken)
                    .ifPresent(token -> response.put("activationToken", token));
        }
        return response;
    }

    private java.util.Map<String, Object> createPendingApprovalSummary(ExtendAllotmentRequest request) {
        return java.util.Map.of(
            "requestId", request.getExtendRequestId(),
            "tenantName", request.getTenant().getDisplayName(),
            "roomNumber", request.getCurrentRoom().getRoomNumber(),
            "currentAgreementId", request.getCurrentAgreementId(),
            "extensionPeriod", request.getExtensionStartDate() + " to " + request.getExtensionEndDate(),
            "requestedAmount", request.getTotalAmount(),
            "tenantNotes", request.getTenantNotes() != null ? request.getTenantNotes() : "",
            "createdAt", request.getCreatedAt(),
            "daysRemaining", request.getRemainingHoursUntilExpiration() / 24
        );
    }

    private java.util.Map<String, Object> createErrorResponse(String errorCode, String message) {
        return java.util.Map.of(
            "error", errorCode,
            "message", message,
            "timestamp", java.time.LocalDateTime.now()
        );
    }
}
