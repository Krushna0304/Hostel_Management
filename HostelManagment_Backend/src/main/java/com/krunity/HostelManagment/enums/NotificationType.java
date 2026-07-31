package com.krunity.HostelManagment.enums;

/**
 * Notification types for the Enhanced Settlement and Allotment System
 * Extends existing notification system with new notification categories
 */
public enum NotificationType {
    
    // ═══ Plan Expiry Notifications ═══════════════════════════════════════════
    
    /**
     * Reminder sent when plan is approaching expiry (configurable lead time).
     * Sent to: TENANT
     */
    PLAN_EXPIRY_REMINDER,
    
    /**
     * Notification when settlement window opens (within notice period).
     * Sent to: TENANT
     */
    SETTLEMENT_WINDOW_OPEN,
    
    /**
     * Urgent notification when immediate action is required.
     * Sent to: TENANT
     */
    URGENT_ACTION_REQUIRED,
    
    /**
     * Final notice before automatic actions are taken.
     * Sent to: TENANT
     */
    FINAL_NOTICE,
    
    // ═══ Settlement Transaction Notifications ═══════════════════════════════════
    
    /**
     * Notification sent when owner creates a settlement transaction.
     * Sent to: TENANT
     */
    SETTLEMENT_TRANSACTION_CREATED,
    
    /**
     * Notification sent when settlement request is approved by owner.
     * Sent to: TENANT
     */
    SETTLEMENT_APPROVED,
    
    /**
     * Notification sent when settlement is completed.
     * Sent to: TENANT, OWNER
     */
    SETTLEMENT_COMPLETED,
    
    /**
     * Notification sent when early settlement request is submitted.
     * Sent to: OWNER
     */
    EARLY_SETTLEMENT_REQUESTED,
    
    // ═══ Extension Request Notifications ═════════════════════════════════════════
    
    /**
     * Notification sent when tenant submits extension request.
     * Sent to: OWNER
     */
    EXTENSION_REQUEST_CREATED,
    
    /**
     * Notification sent when owner approves extension request.
     * Sent to: TENANT
     */
    EXTENSION_REQUEST_APPROVED,
    
    /**
     * Notification sent when extension request is rejected.
     * Sent to: TENANT
     */
    EXTENSION_REQUEST_REJECTED,
    
    /**
     * Notification sent when extension payment is completed.
     * Sent to: TENANT, OWNER
     */
    EXTENSION_PAYMENT_COMPLETED,
    
    /**
     * Reminder sent when extension payment deadline is approaching.
     * Sent to: TENANT
     */
    EXTENSION_PAYMENT_REMINDER,
    
    /**
     * Notification sent when extension request expires due to timeout.
     * Sent to: TENANT, OWNER
     */
    EXTENSION_REQUEST_EXPIRED,
    
    // ═══ Room Allocation Notifications ═══════════════════════════════════════════
    
    /**
     * Notification sent when room allocation fails due to overbooking.
     * Sent to: TENANT
     */
    ROOM_ALLOCATION_FAILED,
    
    /**
     * Notification sent when room becomes available after overbooking.
     * Sent to: TENANT
     */
    ROOM_BECAME_AVAILABLE,
    
    /**
     * Notification sent when overbooking is detected for a room.
     * Sent to: OWNER
     */
    ROOM_OVERBOOKING_DETECTED,
    
    // ═══ System Notifications ═══════════════════════════════════════════════════
    
    /**
     * Notification for system errors or issues.
     * Sent to: ADMIN
     */
    SYSTEM_ERROR,
    
    /**
     * Notification for audit trail events.
     * Sent to: ADMIN
     */
    AUDIT_EVENT;
    
    // ─── Notification Classification ─────────────────────────────────────────────
    
    /**
     * Gets the priority level of the notification (1 = highest, 5 = lowest).
     */
    public int getPriority() {
        switch (this) {
            case URGENT_ACTION_REQUIRED:
            case FINAL_NOTICE:
            case ROOM_ALLOCATION_FAILED:
            case SYSTEM_ERROR:
                return 1; // Critical
                
            case SETTLEMENT_TRANSACTION_CREATED:
            case SETTLEMENT_APPROVED:
            case EXTENSION_PAYMENT_REMINDER:
            case EXTENSION_REQUEST_EXPIRED:
            case ROOM_OVERBOOKING_DETECTED:
                return 2; // High
                
            case PLAN_EXPIRY_REMINDER:
            case EARLY_SETTLEMENT_REQUESTED:
            case EXTENSION_REQUEST_CREATED:
            case EXTENSION_REQUEST_APPROVED:
            case EXTENSION_REQUEST_REJECTED:
                return 3; // Medium
                
            case SETTLEMENT_WINDOW_OPEN:
            case SETTLEMENT_COMPLETED:
            case EXTENSION_PAYMENT_COMPLETED:
            case ROOM_BECAME_AVAILABLE:
                return 4; // Low
                
            case AUDIT_EVENT:
                return 5; // Informational
                
            default:
                return 3; // Medium (default)
        }
    }
    
    /**
     * Checks if notification requires immediate delivery.
     */
    public boolean requiresImmediateDelivery() {
        return getPriority() <= 2;
    }
    
    /**
     * Checks if notification allows retry on delivery failure.
     */
    public boolean allowsRetry() {
        return getPriority() <= 3;
    }
    
    /**
     * Gets the default retry count for failed deliveries.
     */
    public int getDefaultRetryCount() {
        switch (getPriority()) {
            case 1: return 5; // Critical - retry up to 5 times
            case 2: return 3; // High - retry up to 3 times
            case 3: return 2; // Medium - retry up to 2 times
            default: return 1; // Low/Informational - retry once
        }
    }
    
    /**
     * Gets the target recipient type for the notification.
     */
    public RecipientType getDefaultRecipientType() {
        switch (this) {
            case EARLY_SETTLEMENT_REQUESTED:
            case EXTENSION_REQUEST_CREATED:
            case ROOM_OVERBOOKING_DETECTED:
                return RecipientType.OWNER;
                
            case SYSTEM_ERROR:
            case AUDIT_EVENT:
                return RecipientType.ADMIN;
                
            default:
                return RecipientType.TENANT;
        }
    }
    
    /**
     * Gets the default message template name for the notification.
     */
    public String getDefaultTemplateName() {
        return this.name().toLowerCase();
    }
    
    /**
     * Checks if notification supports SMS delivery.
     */
    public boolean supportsSMS() {
        // All notifications support SMS except audit events
        return this != AUDIT_EVENT;
    }
    
    /**
     * Checks if notification supports email delivery.
     */
    public boolean supportsEmail() {
        // All notifications support email
        return true;
    }
    
    /**
     * Gets user-friendly display name.
     */
    public String getDisplayName() {
        switch (this) {
            case PLAN_EXPIRY_REMINDER:
                return "Plan Expiry Reminder";
            case SETTLEMENT_WINDOW_OPEN:
                return "Settlement Window Open";
            case URGENT_ACTION_REQUIRED:
                return "Urgent Action Required";
            case FINAL_NOTICE:
                return "Final Notice";
            case SETTLEMENT_TRANSACTION_CREATED:
                return "Settlement Transaction Created";
            case SETTLEMENT_APPROVED:
                return "Settlement Approved";
            case SETTLEMENT_COMPLETED:
                return "Settlement Completed";
            case EARLY_SETTLEMENT_REQUESTED:
                return "Early Settlement Requested";
            case EXTENSION_REQUEST_CREATED:
                return "Extension Request Created";
            case EXTENSION_REQUEST_APPROVED:
                return "Extension Request Approved";
            case EXTENSION_REQUEST_REJECTED:
                return "Extension Request Rejected";
            case EXTENSION_PAYMENT_COMPLETED:
                return "Extension Payment Completed";
            case EXTENSION_PAYMENT_REMINDER:
                return "Extension Payment Reminder";
            case EXTENSION_REQUEST_EXPIRED:
                return "Extension Request Expired";
            case ROOM_ALLOCATION_FAILED:
                return "Room Allocation Failed";
            case ROOM_BECAME_AVAILABLE:
                return "Room Became Available";
            case ROOM_OVERBOOKING_DETECTED:
                return "Room Overbooking Detected";
            case SYSTEM_ERROR:
                return "System Error";
            case AUDIT_EVENT:
                return "Audit Event";
            default:
                return this.name().replace("_", " ");
        }
    }
    
    /**
     * Recipient type enum for notifications
     */
    public enum RecipientType {
        TENANT, OWNER, ADMIN
    }
}