package com.krunity.HostelManagment.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Provider-agnostic order creation response.
 * Frontend uses orderId + providerKey to open the payment UI.
 */
@Data
@Builder
public class CreateOrderResponse {
    private String orderId;       // Provider's order ID (e.g. Razorpay order_xxx)
    private String providerKey;   // Public API key to pass to frontend SDK
    private long amount;          // Amount in smallest unit
    private String currency;
    private String receiptId;
    private String provider;      // "razorpay" | "stripe" | "mock"
}
