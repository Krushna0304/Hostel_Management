package com.krunity.HostelManagment.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Enhanced Room Allotment Status enum supporting the new settlement and extension workflows
 * Maintains backward compatibility while adding new statuses for advanced workflows
 */
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
     * NEW: Pending action from tenant (payment, agreement acceptance, etc.).
     * Used for tracking agreements awaiting tenant action in room allocation.
     * Set by: SYSTEM.
     */
    ALLOTMENT_ACTION_PENDING,
    
    /**
     * NEW: Settlement-related task pending (calculation, transaction creation, etc.).
     * Used for tracking settlement workflow progress.
     * Set by: SYSTEM.
     */
    SETTLEMENT_TASK,

    /**
     * Both tenant and owner have confirmed the tenant has left.
     * Set by: SYSTEM (when both dual-confirmation flags are true).
     */
    LEFT;

    // ─── Enhanced Transition Matrix ──────────────────────────────────────────

    private static final Map<RoomAllotmentStatus, Set<RoomAllotmentStatus>> ALLOWED_TRANSITIONS =
        Map.of(
            UPCOMING,                   EnumSet.of(ACTIVE, SETTLEMENT_PENDING, SETTLEMENT_REQUESTED, ALLOTMENT_ACTION_PENDING),
            ACTIVE,                     EnumSet.of(SETTLEMENT_PENDING, SETTLEMENT_REQUESTED, SETTLEMENT_TASK, ALLOTMENT_ACTION_PENDING),
            SETTLEMENT_PENDING,         EnumSet.of(SETTLEMENT_REQUESTED, SETTLEMENT_TASK),
            SETTLEMENT_REQUESTED,       EnumSet.of(ON_NOTICE_PERIOD, SETTLEMENT_TASK),
            ON_NOTICE_PERIOD,          EnumSet.of(LEFT, ALLOTMENT_ACTION_PENDING),
            ALLOTMENT_ACTION_PENDING,  EnumSet.of(ACTIVE, LEFT, SETTLEMENT_REQUESTED, SETTLEMENT_TASK),
            SETTLEMENT_TASK,           EnumSet.of(SETTLEMENT_PENDING, SETTLEMENT_REQUESTED, ON_NOTICE_PERIOD, ALLOTMENT_ACTION_PENDING),
            LEFT,                      EnumSet.noneOf(RoomAllotmentStatus.class)
        );

    /** Returns true only for transitions listed in the matrix above. */
    public boolean canTransitionTo(RoomAllotmentStatus target) {
        return ALLOWED_TRANSITIONS
                .getOrDefault(this, EnumSet.noneOf(RoomAllotmentStatus.class))
                .contains(target);
    }

    // ─── Enhanced Utility Sets ───────────────────────────────────────────────

    /**
     * Statuses that count as "occupying" a bed for availability calculations.
     * LEFT is explicitly excluded.
     */
    public static Set<RoomAllotmentStatus> occupyingStatuses() {
        return EnumSet.of(UPCOMING, ACTIVE, SETTLEMENT_PENDING, SETTLEMENT_REQUESTED, 
                         ON_NOTICE_PERIOD, ALLOTMENT_ACTION_PENDING, SETTLEMENT_TASK);
    }

    /**
     * Statuses from which a tenant can request settlement.
     */
    public static Set<RoomAllotmentStatus> settlementRequestableStatuses() {
        return EnumSet.of(UPCOMING, ACTIVE, SETTLEMENT_PENDING);
    }
    
    /**
     * NEW: Statuses that require tenant action (used for room allocation display).
     */
    public static Set<RoomAllotmentStatus> tenantActionPendingStatuses() {
        return EnumSet.of(ALLOTMENT_ACTION_PENDING);
    }
    
    /**
     * NEW: Statuses that indicate settlement workflow is in progress.
     */
    public static Set<RoomAllotmentStatus> settlementInProgressStatuses() {
        return EnumSet.of(SETTLEMENT_PENDING, SETTLEMENT_REQUESTED, SETTLEMENT_TASK, ON_NOTICE_PERIOD);
    }
    
    /**
     * NEW: Statuses that allow extension requests.
     */
    public static Set<RoomAllotmentStatus> extensionRequestableStatuses() {
        return EnumSet.of(ACTIVE, SETTLEMENT_PENDING);
    }
    
    /**
     * NEW: Checks if status represents a terminal state.
     */
    public boolean isTerminal() {
        return this == LEFT;
    }
    
    /**
     * NEW: Checks if status requires system attention.
     */
    public boolean requiresSystemAttention() {
        return this == SETTLEMENT_TASK || this == ALLOTMENT_ACTION_PENDING;
    }
    
    /**
     * NEW: Gets priority level for room allocation (lower number = higher priority).
     */
    public int getAllocationPriority() {
        switch (this) {
            case ACTIVE:
            case ON_NOTICE_PERIOD:
                return 1; // Highest priority (existing tenants)
            case UPCOMING:
            case SETTLEMENT_PENDING:
            case SETTLEMENT_REQUESTED:
            case SETTLEMENT_TASK:
                return 2; // Medium priority
            case ALLOTMENT_ACTION_PENDING:
                return 3; // Lower priority (pending action)
            case LEFT:
                return 999; // No priority (should not occupy bed)
            default:
                return 2;
        }
    }
}
