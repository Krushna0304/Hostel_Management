package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.TransactionMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AcceptAgreementRequest {
    @NotNull(message = "Payment mode is required")
    private TransactionMode paymentMode;

    private String otp;           // For CASH payments — OTP from owner

    // For ONLINE payments — Razorpay fields sent after frontend checkout
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}

