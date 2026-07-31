package com.krunity.HostelManagment.enums;

/**
 * Enhanced Settlement Status enum supporting the new settlement workflow
 * Maintains backward compatibility with existing statuses while adding new ones
 */
public enum SettlementStatus {
    // Existing statuses (maintained for backward compatibility)
    PENDING_OWNER_REVIEW,
    CALCULATION_IN_PROGRESS,
    PENDING_TENANT_PAYMENT,
    PENDING_OWNER_PAYMENT,
    PAYMENT_IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    REJECTED,
    
    // New enhanced settlement statuses
    SETTLEMENT_TRANSACTION_CREATED,  // When owner creates settlement transaction at any time
    SETTLEMENT_APPROVED,             // When owner approves settlement request
    SETTLEMENT_DONE;                 // When settlement payment is completed
    
    /**
     * Checks if the status represents a terminal state (no further transitions possible)
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == REJECTED || this == SETTLEMENT_DONE;
    }
    
    /**
     * Checks if the status indicates settlement is pending some action
     */
    public boolean isPending() {
        return this == PENDING_OWNER_REVIEW || 
               this == PENDING_TENANT_PAYMENT || 
               this == PENDING_OWNER_PAYMENT ||
               this == SETTLEMENT_TRANSACTION_CREATED;
    }
    
    /**
     * Checks if the status indicates payment processing
     */
    public boolean isPaymentInProgress() {
        return this == PAYMENT_IN_PROGRESS || this == CALCULATION_IN_PROGRESS;
    }
    
    /**
     * Gets the next valid statuses from current status
     */
    public SettlementStatus[] getValidTransitions() {
        switch (this) {
            case PENDING_OWNER_REVIEW:
                return new SettlementStatus[]{SETTLEMENT_APPROVED, CALCULATION_IN_PROGRESS, REJECTED};
            case CALCULATION_IN_PROGRESS:
                return new SettlementStatus[]{SETTLEMENT_TRANSACTION_CREATED, PENDING_TENANT_PAYMENT, PENDING_OWNER_PAYMENT};
            case SETTLEMENT_TRANSACTION_CREATED:
                return new SettlementStatus[]{PENDING_TENANT_PAYMENT, PENDING_OWNER_PAYMENT, SETTLEMENT_DONE};
            case SETTLEMENT_APPROVED:
                return new SettlementStatus[]{SETTLEMENT_TRANSACTION_CREATED, CALCULATION_IN_PROGRESS};
            case PENDING_TENANT_PAYMENT:
            case PENDING_OWNER_PAYMENT:
                return new SettlementStatus[]{PAYMENT_IN_PROGRESS, CANCELLED};
            case PAYMENT_IN_PROGRESS:
                return new SettlementStatus[]{SETTLEMENT_DONE, COMPLETED, CANCELLED};
            case SETTLEMENT_DONE:
                return new SettlementStatus[]{COMPLETED};
            default:
                return new SettlementStatus[0]; // Terminal states
        }
    }
    
    /**
     * Validates if transition from current status to target status is allowed
     */
    public boolean canTransitionTo(SettlementStatus targetStatus) {
        if (this.isTerminal()) {
            return false; // No transitions from terminal states
        }
        
        SettlementStatus[] validTransitions = getValidTransitions();
        for (SettlementStatus validStatus : validTransitions) {
            if (validStatus == targetStatus) {
                return true;
            }
        }
        return false;
    }
}