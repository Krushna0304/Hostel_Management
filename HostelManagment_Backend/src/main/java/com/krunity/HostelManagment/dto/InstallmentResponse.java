package com.krunity.HostelManagment.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class InstallmentResponse {
    private UUID scheduleId;
    private Integer installmentNumber;
    private Long amount;
    private LocalDate dueDate;
    private String paymentStatus;   // SCHEDULED / OVERDUE / PARTIALLY_PAID / COMPLETED
    private Long paidAmount;
    private Long lateFeeApplied;
    private LocalDateTime paidAt;
    private UUID transactionId;
    private String paymentMode;     // CASH / ONLINE (populated for COMPLETED installments)
}
