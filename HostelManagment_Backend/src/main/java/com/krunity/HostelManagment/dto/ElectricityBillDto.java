package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.BillStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ElectricityBillDto {
    private UUID billId;
    private UUID accountId;
    private String accountNumber;
    private UUID roomId;
    private String roomNumber;
    private UUID hostelId;
    private String hostelName;
    private UUID ownerId;
    private UUID tenantId;
    private String tenantName;
    private String tenantPhone;
    private Integer billMonth;
    private Integer billYear;
    private String billPeriod; // "January 2024"
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private BigDecimal totalRemainingForRoom;
    private BillStatus status;
    private LocalDateTime dueDate;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ElectricityPaymentDto> payments;
}