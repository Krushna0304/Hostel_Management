package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.TransactionMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecordPaymentRequest {

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1")
    private Long amount;

    private TransactionMode paymentMode;
    
    // For cash payments
    private String otp;
    
    // For online payments
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}
