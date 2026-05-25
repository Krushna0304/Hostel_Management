package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.TransactionMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ElectricityPaymentRequest {
    
    @NotNull(message = "Bill ID is required")
    private UUID billId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @NotNull(message = "Payment mode is required")
    private TransactionMode paymentMode;
    
    private String paymentReference;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String notes;
    private String otp;
}