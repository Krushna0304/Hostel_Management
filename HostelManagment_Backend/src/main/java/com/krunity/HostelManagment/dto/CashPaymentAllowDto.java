package com.krunity.HostelManagment.dto;

import lombok.Builder;
import lombok.Data;

/**
 * One togglable cash-payment method for the owner Settings screen.
 */
@Data
@Builder
public class CashPaymentAllowDto {
    private String method;       // CashPaymentMethod enum name
    private String displayName;  // human-readable label
    private boolean isAllowed;
}
