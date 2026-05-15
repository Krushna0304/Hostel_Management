package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentSummaryResponse {
    private UUID scheduleId;
    private Integer installmentNumber;
    private Long amount;
    private LocalDate dueDate;
    private TransactionStatus paymentStatus;
    private Long paidAmount;
    private LocalDateTime paidAt;
    private Long lateFeeApplied;
    private UUID transactionId;
}