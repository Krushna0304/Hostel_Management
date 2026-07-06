package com.krunity.HostelManagment.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum RoomAllotmentStatus {

    /**
     * Agreement created; tenant has not yet physically arrived.
     * Set by: SYSTEM (on agreement acceptance).
     */
    UPCOMING,

    /**
     * Tenant has arrived and marked themselves active.
     * Set by: TENANT.
     */
    ACTIVE,

    /**
     * System auto-flag: within the notice-period window before agreement end date.
     * Formula: currentDate >= (endDate - noticePeriodMonths).
     * Set by: SYSTEM (daily cron).
     */
    SETTLEMENT_PENDING,

    /**
     * Tenant has formally requested to vacate.
     * Allowed from: UPCOMING, ACTIVE, SETTLEMENT_PENDING.
     * Set by: TENANT.
     */
    SETTLEMENT_REQUESTED,

    /**
     * Owner has approved the settlement; tenant is serving notice period.
     * Set by: OWNER.
     */
    ON_NOTICE_PERIOD,

    /**
     * Both tenant and owner have confirmed the tenant has left.
     * Set by: SYSTEM (when both dual-confirmation flags are true).
     */
    LEFT;

    // ─── Transition matrix ────────────────────────────────────────────────────

    private static final Map<RoomAllotmentStatus, Set<RoomAllotmentStatus>> ALLOWED_TRANSITIONS =
        Map.of(
            UPCOMING,            EnumSet.of(ACTIVE, SETTLEMENT_PENDING, SETTLEMENT_REQUESTED),
            ACTIVE,              EnumSet.of(SETTLEMENT_PENDING, SETTLEMENT_REQUESTED),
            SETTLEMENT_PENDING,  EnumSet.of(SETTLEMENT_REQUESTED),
            SETTLEMENT_REQUESTED,EnumSet.of(ON_NOTICE_PERIOD),
            ON_NOTICE_PERIOD,    EnumSet.of(LEFT),
            LEFT,                EnumSet.noneOf(RoomAllotmentStatus.class)
        );

    /** Returns true only for transitions listed in the matrix above. */
    public boolean canTransitionTo(RoomAllotmentStatus target) {
        return ALLOWED_TRANSITIONS
                .getOrDefault(this, EnumSet.noneOf(RoomAllotmentStatus.class))
                .contains(target);
    }

    // ─── Utility sets ─────────────────────────────────────────────────────────

    /**
     * Statuses that count as "occupying" a bed for availability calculations.
     * LEFT is explicitly excluded.
     */
    public static Set<RoomAllotmentStatus> occupyingStatuses() {
        return EnumSet.of(UPCOMING, ACTIVE, SETTLEMENT_PENDING, SETTLEMENT_REQUESTED, ON_NOTICE_PERIOD);
    }

    /**
     * Statuses from which a tenant can request settlement.
     */
    public static Set<RoomAllotmentStatus> settlementRequestableStatuses() {
        return EnumSet.of(UPCOMING, ACTIVE, SETTLEMENT_PENDING);
    }
}
