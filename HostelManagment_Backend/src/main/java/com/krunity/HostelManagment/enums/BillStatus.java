package com.krunity.HostelManagment.enums;

public enum BillStatus {
    PENDING,    // No payment made
    PARTIAL,    // Some tenants have paid their share
    PAID,       // Fully paid
    COMPLETED,  // All tenant shares paid (remaining amount is 0)
    OVERDUE     // Past due date
}