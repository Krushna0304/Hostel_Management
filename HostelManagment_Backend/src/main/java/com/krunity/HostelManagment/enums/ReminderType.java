package com.krunity.HostelManagment.enums;

public enum ReminderType {
    BEFORE_DUE_DATE,    // 1 day before due date
    ON_DUE_DATE,        // On the due date
    AFTER_DUE_DATE,     // After due date (overdue)
    OTHER_CHARGE        // New other charge created for tenant
}
