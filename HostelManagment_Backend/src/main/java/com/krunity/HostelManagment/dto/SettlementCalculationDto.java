package com.krunity.HostelManagment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SettlementCalculationDto {
    
    private String settlementId;
    private String agreementId;
    private String tenantName;
    private String roomNumber;
    
    // Financial Breakdown
    private BigDecimal securityDeposit;
    private BigDecimal outstandingRent;
    private BigDecimal outstandingCharges;
    private BigDecimal damageCharges;
    private BigDecimal cleaningCharges;
    private BigDecimal otherDeductions;
    private BigDecimal totalDeductions;
    private BigDecimal finalSettlementAmount;
    
    // Settlement Type
    private String settlementType; // TENANT_PAYABLE or OWNER_PAYABLE
    
    // Settlement Status
    private String status; // PENDING_OWNER_REVIEW, PENDING_TENANT_PAYMENT, PENDING_OWNER_PAYMENT, COMPLETED, etc.
    
    // Settlement completion details
    private LocalDateTime settledAt;
    private String paymentReference;
    
    // Outstanding Items Details
    private List<OutstandingItemDto> outstandingItems;
    
    // Notes
    private String tenantNotes;
    private String ownerNotes;
    private String damageDescription;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    public static class OutstandingItemDto {
        private String type; // RENT, RECURRING_CHARGE, ONE_TIME_CHARGE
        private String description;
        private BigDecimal amount;
        private String dueDate;
        private String status;
    }
}