package com.krunity.HostelManagment.dto;

import lombok.Data;

/**
 * Provider-agnostic payment verification request.
 * Frontend sends these fields after completing payment in the provider's UI.
 */
@Data
public class VerifyPaymentRequest {
    private String orderId;       // Provider order ID
    private String paymentId;     // Provider payment ID
    private String signature;     // Provider signature (for HMAC verification)
    private String agreementId;   // Our internal agreement ID (for agreement payments)
    private String scheduleId;    // Our internal schedule ID (for installment payments)
}
