package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.PaymentStatus;
import com.krunity.HostelManagment.enums.TransactionMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ElectricityPaymentDto {
    private UUID paymentId;
    private UUID billId;
    private UUID tenantId;
    private String tenantName;
    private String billPeriod; // "July 2026" — populated for owner collection history
    private String roomNumber;
    private BigDecimal amount;
    private TransactionMode paymentMode;
    private PaymentStatus status;
    private String paymentReference;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}