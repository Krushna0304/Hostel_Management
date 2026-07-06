package com.krunity.HostelManagment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Per-tenant aggregate of electricity dues for the owner's Collections dashboard.
 */
@Data
@Builder
public class ElectricityCollectionRowDto {
    private UUID tenantId;
    private String tenantName;
    private String hostelName;
    private String roomNumber;
    private int pendingBills;          // number of unpaid shares
    private BigDecimal totalAmount;    // sum of all share amounts (paid + pending)
    private BigDecimal paidAmount;     // sum of paid shares
    private BigDecimal outstandingAmount; // sum of pending shares
    private BigDecimal overdueAmount;  // pending shares whose bill is past its due date
}
