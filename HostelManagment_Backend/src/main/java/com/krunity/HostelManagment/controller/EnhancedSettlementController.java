package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.dto.SettlementCalculationResult;
import com.krunity.HostelManagment.dto.SettlementTransaction;
import com.krunity.HostelManagment.dto.SettlementTransactionRequest;
import com.krunity.HostelManagment.dto.EarlySettlementRequest;
import com.krunity.HostelManagment.dto.SettlementApprovalRequest;
import com.krunity.HostelManagment.model.SettlementRequest;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.service.EnhancedSettlementService;
import com.krunity.HostelManagment.service.SettlementTransactionCalculator;
import com.krunity.HostelManagment.Utils.ApplicationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Enhanced Settlement functionality
 * Handles settlement transactions, early settlements, and room availability updates
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settlements")
public class EnhancedSettlementController {

    @Autowired
    private EnhancedSettlementService enhancedSettlementService;

    @Autowired
    private SettlementTransactionCalculator settlementCalculator;

    // ─── Settlement Transaction Endpoints ────────────────────────────────────

    /**
     * Create settlement transaction at any time based on current plan
     */
    @PostMapping("/transactions")
    public ResponseEntity<?> createSettlementTransaction(
            @Valid @RequestBody SettlementTransactionRequest request) {
        
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Creating settlement transaction for agreement {} by owner {}", 
                    request.getAgreementId(), currentUser.getUserId());
            
            SettlementRequest settlement = enhancedSettlementService.createSettlementTransaction(
                    request.getAgreementId(),
                    currentUser.getUserId(),
                    request.getCalculationDate(),
                    request.getNotes()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(
                createSettlementTransactionResponse(settlement)
            );
            
        } catch (SecurityException e) {
            log.warn("Unauthorized settlement transaction creation attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("ACCESS_DENIED", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create settlement transaction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("TRANSACTION_CREATION_FAILED", e.getMessage()));
        }
    }

    /**
     * Request early settlement (before agreement end date)
     */
    @PostMapping("/early-settlement")
    public ResponseEntity<?> requestEarlySettlement(
            @Valid @RequestBody EarlySettlementRequest request) {
        
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Processing early settlement request for agreement {} by tenant {}", 
                    request.getAgreementId(), currentUser.getUserId());
            
            SettlementRequest settlement = enhancedSettlementService.processEarlySettlement(
                    request.getAgreementId(),
                    currentUser.getUserId(),
                    request.getRequestedEndDate(),
                    request.getReason()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(
                createEarlySettlementResponse(settlement)
            );
            
        } catch (SecurityException e) {
            log.warn("Unauthorized early settlement request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("ACCESS_DENIED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid early settlement request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to process early settlement: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("EARLY_SETTLEMENT_FAILED", e.getMessage()));
        }
    }

    /**
     * Approve settlement with room availability update
     */
    @PutMapping("/{settlementId}/approve-with-room-update")
    public ResponseEntity<?> approveSettlementWithRoomUpdate(
            @PathVariable UUID settlementId,
            @Valid @RequestBody SettlementApprovalRequest request) {
        
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Approving settlement {} with room update by owner {}", 
                    settlementId, currentUser.getUserId());
            
            SettlementRequest settlement = enhancedSettlementService.approveSettlementWithRoomUpdate(
                    settlementId,
                    currentUser.getUserId(),
                    request.getOwnerNotes(),
                    request.isUpdateRoomAvailability()
            );
            
            return ResponseEntity.ok(createSettlementApprovalResponse(settlement));
            
        } catch (SecurityException e) {
            log.warn("Unauthorized settlement approval attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("ACCESS_DENIED", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to approve settlement: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("APPROVAL_FAILED", e.getMessage()));
        }
    }

    /**
     * Get settlement transaction details
     */
    @GetMapping("/{settlementId}/transaction")
    public ResponseEntity<?> getSettlementTransaction(@PathVariable UUID settlementId) {
        
        try {
            // Implementation would include authorization checks
            // For now, returning basic response structure
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("settlementId", settlementId);
            response.put("message", "Settlement transaction details retrieved successfully");
            
            return ResponseEntity.ok().body(response);
            
        } catch (Exception e) {
            log.error("Failed to get settlement transaction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("TRANSACTION_RETRIEVAL_FAILED", e.getMessage()));
        }
    }

    // ─── Query Endpoints ─────────────────────────────────────────────────────

    /**
     * Get settlements with transaction data for owner
     */
    @GetMapping("/with-transactions")
    public ResponseEntity<?> getSettlementsWithTransactions() {
        
        try {
            List<SettlementRequest> settlements = enhancedSettlementService.getSettlementsWithTransactionData();
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("settlements", settlements.stream().map(this::createSettlementSummary).toList());
            response.put("count", settlements.size());
            
            return ResponseEntity.ok().body(response);
            
        } catch (Exception e) {
            log.error("Failed to get settlements with transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("SETTLEMENTS_RETRIEVAL_FAILED", e.getMessage()));
        }
    }

    /**
     * Get early settlement requests for owner
     */
    @GetMapping("/early-settlements")
    public ResponseEntity<?> getEarlySettlementRequests() {
        
        try {
            User currentUser = ApplicationContext.getUser();
            UUID currentUserId = currentUser.getUserId();
            List<SettlementRequest> earlySettlements = 
                    enhancedSettlementService.getEarlySettlementRequestsForOwner(currentUserId);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("earlySettlements", earlySettlements.stream().map(this::createEarlySettlementSummary).toList());
            response.put("count", earlySettlements.size());
            
            return ResponseEntity.ok().body(response);
            
        } catch (Exception e) {
            log.error("Failed to get early settlement requests: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("EARLY_SETTLEMENTS_RETRIEVAL_FAILED", e.getMessage()));
        }
    }

    // ─── Helper Methods ──────────────────────────────────────────────────────

    private java.util.Map<String, Object> createSettlementTransactionResponse(SettlementRequest settlement) {
        SettlementTransaction transaction = enhancedSettlementService.getSettlementTransaction(settlement);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("settlementId", settlement.getSettlementId());
        response.put("status", settlement.getStatus());
        response.put("agreementId", settlement.getAgreementId());
        response.put("settlementAmount", settlement.getFinalSettlementAmount() != null ? 
                settlement.getFinalSettlementAmount() : 0);
        response.put("settlementType", settlement.getSettlementType() != null ? 
                settlement.getSettlementType() : "PENDING");
        response.put("transactionId", transaction != null ? transaction.getTransactionId() : null);
        response.put("notificationSent", true);
        response.put("createdAt", settlement.getCreatedAt());
        response.put("message", "Settlement transaction created successfully");
        
        return response;
    }

    private java.util.Map<String, Object> createEarlySettlementResponse(SettlementRequest settlement) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("settlementId", settlement.getSettlementId());
        response.put("status", settlement.getStatus());
        response.put("agreementId", settlement.getAgreementId());
        response.put("requestedEndDate", settlement.getRequestedEndDate());
        response.put("earlyExit", settlement.isEarlySettlementRequested());
        response.put("estimatedPenalty", 0); // Would be calculated based on plan
        response.put("createdAt", settlement.getCreatedAt());
        response.put("message", "Early settlement request submitted successfully");
        
        return response;
    }

    private java.util.Map<String, Object> createSettlementApprovalResponse(SettlementRequest settlement) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("settlementId", settlement.getSettlementId());
        response.put("status", settlement.getStatus());
        response.put("agreementId", settlement.getAgreementId());
        response.put("finalAmount", settlement.getFinalSettlementAmount() != null ? 
                settlement.getFinalSettlementAmount() : 0);
        response.put("settlementType", settlement.getSettlementType() != null ? 
                settlement.getSettlementType() : "PENDING");
        response.put("roomAvailabilityUpdated", settlement.isRoomAvailabilityUpdated());
        response.put("approvedAt", settlement.getUpdatedAt());
        response.put("message", "Settlement approved successfully");
        
        return response;
    }

    private java.util.Map<String, Object> createSettlementSummary(SettlementRequest settlement) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("settlementId", settlement.getSettlementId());
        response.put("agreementId", settlement.getAgreementId());
        response.put("tenantName", settlement.getTenant() != null ? settlement.getTenant().getDisplayName() : "Unknown");
        response.put("status", settlement.getStatus());
        response.put("amount", settlement.getFinalSettlementAmount() != null ? 
                settlement.getFinalSettlementAmount() : 0);
        response.put("type", settlement.getSettlementType() != null ? settlement.getSettlementType() : "PENDING");
        response.put("hasTransactionData", settlement.getSettlementTransactionData() != null);
        response.put("createdAt", settlement.getCreatedAt());
        
        return response;
    }

    private java.util.Map<String, Object> createEarlySettlementSummary(SettlementRequest settlement) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("settlementId", settlement.getSettlementId());
        response.put("agreementId", settlement.getAgreementId());
        response.put("tenantName", settlement.getTenant() != null ? settlement.getTenant().getDisplayName() : "Unknown");
        response.put("requestedEndDate", settlement.getRequestedEndDate());
        response.put("status", settlement.getStatus());
        response.put("earlyExit", settlement.isEarlySettlementRequested());
        response.put("createdAt", settlement.getCreatedAt());
        response.put("tenantNotes", settlement.getTenantNotes() != null ? settlement.getTenantNotes() : "");
        
        return response;
    }

    private java.util.Map<String, Object> createErrorResponse(String errorCode, String message) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("error", errorCode);
        response.put("message", message);
        response.put("timestamp", java.time.LocalDateTime.now());
        
        return response;
    }
}