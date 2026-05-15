package com.krunity.HostelManagment.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Provider-agnostic order creation request.
 * Amount is in smallest currency unit (paise for INR, cents for USD).
 */
@Data
@Builder
public class CreateOrderRequest {
    private long amount;          // e.g. 850000 = ₹8500
    private String currency;      // e.g. "INR"
    private String receiptId;     // e.g. agreementId — for idempotency & tracking
    private String description;   // e.g. "Agreement activation payment"
}
