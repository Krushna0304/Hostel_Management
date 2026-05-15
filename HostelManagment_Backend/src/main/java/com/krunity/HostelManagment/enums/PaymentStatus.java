package com.krunity.HostelManagment.enums;

/**
 * Payment status for charges and installments
 */
public enum PaymentStatus {
    PENDING,        // Payment not yet made
    PARTIALLY_PAID, // Partial payment received
    COMPLETED,      // Full payment received
    OVERDUE,        // Payment is past due date
    CANCELLED,      // Payment/charge cancelled
    SCHEDULED       // Payment is scheduled for future
}