package com.krunity.HostelManagment.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Status enum for Extension Allotment Requests
 * Tracks the lifecycle of tenant requests to extend their current accommodation
 */
public enum ExtendAllotmentStatus {

    /**
     * Extension request has been created by tenant, awaiting owner review.
     * Set by: TENANT (when creating extension request).
     */
    PENDING_OWNER_APPROVAL,

    /**
     * Owner has approved the extension request, new agreement created, awaiting tenant payment.
     * Set by: OWNER (when approving extension request).
     */
    APPROVED_PENDING_PAYMENT,

    /**
     * Tenant has completed payment for the extension.
     * Set by: SYSTEM (after successful payment processing).
     */
    PAYMENT_COMPLETED,

    /**
     * New agreement has been created and activated in the system.
     * Set by: SYSTEM (after payment completion and agreement activation).
     */
    AGREEMENT_CREATED,

    /**
     * Extension is now active, tenant can use the new accommodation period.
     * Set by: SYSTEM (when new agreement period starts).
     */
    ACTIVE,

    /**
     * Owner has rejected the extension request.
     * Set by: OWNER (when declining extension request).
     */
    REJECTED,

    /**
     * Extension request has been cancelled (by tenant or system).
     * Set by: TENANT or SYSTEM.
     */
    CANCELLED,

    /**
     * Extension request has expired due to timeout (payment not completed in time).
     * Set by: SYSTEM (when payment deadline is missed).
     */
    EXPIRED;

    // ─── State Transition Matrix ─────────────────────────────────────────────

    private static final Map<ExtendAllotmentStatus, Set<ExtendAllotmentStatus>> ALLOWED_TRANSITIONS =
        Map.of(
            PENDING_OWNER_APPROVAL,     EnumSet.of(APPROVED_PENDING_PAYMENT, REJECTED, CANCELLED),
            APPROVED_PENDING_PAYMENT,   EnumSet.of(PAYMENT_COMPLETED, EXPIRED, CANCELLED),
            PAYMENT_COMPLETED,          EnumSet.of(AGREEMENT_CREATED, CANCELLED),
            AGREEMENT_CREATED,          EnumSet.of(ACTIVE, CANCELLED),
            ACTIVE,                     EnumSet.noneOf(ExtendAllotmentStatus.class), // Terminal state
            REJECTED,                   EnumSet.noneOf(ExtendAllotmentStatus.class), // Terminal state
            CANCELLED,                  EnumSet.noneOf(ExtendAllotmentStatus.class), // Terminal state
            EXPIRED,                    EnumSet.noneOf(ExtendAllotmentStatus.class)  // Terminal state
        );

    /**
     * Checks if transition from current status to target status is valid.
     */
    public boolean canTransitionTo(ExtendAllotmentStatus target) {
        return ALLOWED_TRANSITIONS
                .getOrDefault(this, EnumSet.noneOf(ExtendAllotmentStatus.class))
                .contains(target);
    }

    // ─── Status Classification Methods ───────────────────────────────────────

    /**
     * Checks if the status represents a terminal state (no further transitions possible).
     */
    public boolean isTerminal() {
        return this == ACTIVE || this == REJECTED || this == CANCELLED || this == EXPIRED;
    }

    /**
     * Checks if the status indicates the request is still pending some action.
     */
    public boolean isPending() {
        return this == PENDING_OWNER_APPROVAL || this == APPROVED_PENDING_PAYMENT;
    }

    /**
     * Checks if the status indicates successful completion.
     */
    public boolean isSuccessful() {
        return this == PAYMENT_COMPLETED || this == AGREEMENT_CREATED || this == ACTIVE;
    }

    /**
     * Checks if the status indicates failure or cancellation.
     */
    public boolean isFailed() {
        return this == REJECTED || this == CANCELLED || this == EXPIRED;
    }

    /**
     * Checks if the status requires tenant action.
     */
    public boolean requiresTenantAction() {
        return this == APPROVED_PENDING_PAYMENT;
    }

    /**
     * Checks if the status requires owner action.
     */
    public boolean requiresOwnerAction() {
        return this == PENDING_OWNER_APPROVAL;
    }

    // ─── Utility Sets ────────────────────────────────────────────────────────

    /**
     * Statuses that indicate active extension requests (not terminated).
     */
    public static Set<ExtendAllotmentStatus> activeStatuses() {
        return EnumSet.of(PENDING_OWNER_APPROVAL, APPROVED_PENDING_PAYMENT, 
                         PAYMENT_COMPLETED, AGREEMENT_CREATED);
    }

    /**
     * Statuses that allow cancellation by tenant.
     */
    public static Set<ExtendAllotmentStatus> cancellableByTenantStatuses() {
        return EnumSet.of(PENDING_OWNER_APPROVAL, APPROVED_PENDING_PAYMENT);
    }

    /**
     * Statuses that allow cancellation by owner.
     */
    public static Set<ExtendAllotmentStatus> cancellableByOwnerStatuses() {
        return EnumSet.of(PENDING_OWNER_APPROVAL, APPROVED_PENDING_PAYMENT);
    }

    /**
     * Statuses that count towards room allocation (agreement exists).
     */
    public static Set<ExtendAllotmentStatus> allocationRelevantStatuses() {
        return EnumSet.of(AGREEMENT_CREATED, ACTIVE);
    }

    /**
     * Gets the display-friendly name for the status.
     */
    public String getDisplayName() {
        switch (this) {
            case PENDING_OWNER_APPROVAL:
                return "Pending Owner Approval";
            case APPROVED_PENDING_PAYMENT:
                return "Approved - Payment Pending";
            case PAYMENT_COMPLETED:
                return "Payment Completed";
            case AGREEMENT_CREATED:
                return "Agreement Created";
            case ACTIVE:
                return "Active";
            case REJECTED:
                return "Rejected";
            case CANCELLED:
                return "Cancelled";
            case EXPIRED:
                return "Expired";
            default:
                return this.name();
        }
    }

    /**
     * Gets the next expected action description for the status.
     */
    public String getNextActionDescription() {
        switch (this) {
            case PENDING_OWNER_APPROVAL:
                return "Waiting for owner to approve extension request";
            case APPROVED_PENDING_PAYMENT:
                return "Waiting for tenant to complete payment";
            case PAYMENT_COMPLETED:
                return "System processing agreement creation";
            case AGREEMENT_CREATED:
                return "Waiting for extension period to start";
            case ACTIVE:
                return "Extension is active";
            case REJECTED:
                return "Extension request was rejected by owner";
            case CANCELLED:
                return "Extension request was cancelled";
            case EXPIRED:
                return "Extension request expired due to timeout";
            default:
                return "Unknown status";
        }
    }
}