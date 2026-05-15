package com.krunity.HostelManagment.model.plan;

/**
 * Enum to specify when a charge should be collected
 */
public enum PaymentTiming {
    AT_AGREEMENT,    // Collected when agreement is accepted (e.g., security deposit, one-time charges)
    IN_INSTALLMENTS, // Included in monthly installments (e.g., monthly cleaning, utilities)
    AT_END          // Collected at the end of agreement (e.g., deep cleaning on exit)
}