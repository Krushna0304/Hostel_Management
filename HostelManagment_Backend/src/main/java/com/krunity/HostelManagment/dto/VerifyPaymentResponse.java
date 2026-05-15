package com.krunity.HostelManagment.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Provider-agnostic payment verification response.
 */
@Data
@Builder
public class VerifyPaymentResponse {
    private boolean verified;
    private String paymentId;
    private String orderId;
    private String message;
}
