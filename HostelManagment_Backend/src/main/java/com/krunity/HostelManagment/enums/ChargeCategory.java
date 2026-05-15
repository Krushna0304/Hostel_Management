package com.krunity.HostelManagment.enums;

/**
 * Categorizes charges for proper accounting and billing logic
 */
public enum ChargeCategory {
    RENT,                    // Monthly rent (revenue)
    RECURRING_CHARGE,        // Monthly utilities, cleaning (revenue)
    ONE_TIME_CHARGE,         // Registration, admission (revenue)
    REFUNDABLE_DEPOSIT,      // Security, furniture (liability)
    DEDUCTION_CHARGE,        // Exit cleaning, damages (from deposit)
    OTHER_CHARGE_TENANT,     // Charge specific to a tenant
    OTHER_CHARGE_ROOM        // Charge to a room (split among tenants)
}