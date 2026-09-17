package com.krunity.HostelManagment.enums;

public enum AgreementType {
    /** Canonical agreement type for a PG room; aligned with PlanType.PG_ROOM. */
    PG_ROOM,
    /** Legacy persisted value retained so existing ROOM agreements remain readable. */
    ROOM,
    WORKER,
    FLAT
}
