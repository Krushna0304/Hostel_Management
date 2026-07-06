package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.SettlementStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SettlementResponseDto {
    
    private UUID settlementId;
    private String agreementId;
    private SettlementStatus status;
    
    // User information (avoiding full User objects)
    private UUID tenantId;
    private String tenantName;
    private UUID ownerId;
    private String ownerName;
    
    // Room information
    private UUID roomId;
    private String roomNumber;
    
    // Financial details
    private BigDecimal securityDeposit;
    private BigDecimal outstandingRent;
    private BigDecimal outstandingCharges;
    private BigDecimal damageCharges;
    private BigDecimal cleaningCharges;
    private BigDecimal otherDeductions;
    private BigDecimal totalDeductions;
    private BigDecimal finalSettlementAmount;
    private String settlementType;
    
    // Notes and descriptions
    private String tenantNotes;
    private String ownerNotes;
    private String damageDescription;
    
    // Payment information
    private String paymentReference;

    // Allotment status (for UI-driven left confirmation)
    private UUID allotmentId;
    private String allotmentStatus;
    private boolean tenantMarkedLeft;
    private boolean ownerMarkedLeft;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime settledAt;
}