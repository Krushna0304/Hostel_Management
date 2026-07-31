package com.krunity.HostelManagment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.krunity.HostelManagment.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Enhanced Settlement Request entity with new features for the enhanced settlement system
 * This extends the existing SettlementRequest with additional fields and capabilities
 * 
 * Note: This will replace the existing SettlementRequest entity after migration
 */
@Entity
@Table(name = "settlement_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EnhancedSettlementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "settlement_id", updatable = false, nullable = false)
    private UUID settlementId;

    @Column(name = "agreement_id", nullable = false)
    private String agreementId; // MongoDB Agreement ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING_OWNER_REVIEW;

    @Column(name = "requested_end_date")
    private LocalDate requestedEndDate;

    // ─── NEW: Enhanced Settlement Fields ─────────────────────────────────────

    /**
     * Settlement transaction data (JSON format) for flexible transaction storage
     */
    @Column(name = "settlement_transaction_data", columnDefinition = "TEXT")
    private String settlementTransactionData;

    /**
     * Flag indicating if this is an early settlement request (before notice period)
     */
    @Column(name = "early_settlement_requested", columnDefinition = "boolean default false")
    @Builder.Default
    private boolean earlySettlementRequested = false;

    /**
     * User who approved the settlement (owner or admin)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_approved_by")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User settlementApprovedBy;

    /**
     * Flag indicating if this settlement qualifies for automatic processing
     */
    @Column(name = "auto_settlement_eligible", columnDefinition = "boolean default false")
    @Builder.Default
    private boolean autoSettlementEligible = false;

    /**
     * Flag indicating if room availability has been updated after settlement approval
     */
    @Column(name = "room_availability_updated", columnDefinition = "boolean default false")
    @Builder.Default
    private boolean roomAvailabilityUpdated = false;

    /**
     * Timestamp when settlement transaction was created
     */
    @Column(name = "transaction_created_at")
    private LocalDateTime transactionCreatedAt;

    // ─── Financial Summary ───────────────────────────────────────────────────

    @Column(name = "security_deposit", precision = 10, scale = 2)
    private BigDecimal securityDeposit;

    @Column(name = "outstanding_rent", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal outstandingRent = BigDecimal.ZERO;

    @Column(name = "outstanding_charges", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal outstandingCharges = BigDecimal.ZERO;

    @Column(name = "damage_charges", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal damageCharges = BigDecimal.ZERO;

    @Column(name = "cleaning_charges", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal cleaningCharges = BigDecimal.ZERO;

    @Column(name = "other_deductions", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "total_deductions", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "final_settlement_amount", precision = 10, scale = 2)
    private BigDecimal finalSettlementAmount;

    // Settlement Type: TENANT_PAYABLE (negative) or OWNER_PAYABLE (positive)
    @Column(name = "settlement_type", length = 20)
    private String settlementType;

    // Comments and Notes
    @Column(name = "tenant_notes", length = 500)
    private String tenantNotes;

    @Column(name = "owner_notes", length = 500)
    private String ownerNotes;

    @Column(name = "damage_description", length = 1000)
    private String damageDescription;

    // Payment Reference (if settlement involves payment)
    @Column(name = "payment_reference")
    private String paymentReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    // ─── Enhanced Business Logic ─────────────────────────────────────────────

    /**
     * Checks if status transition is allowed
     */
    public boolean canTransitionTo(SettlementStatus newStatus) {
        return this.status.canTransitionTo(newStatus);
    }

    /**
     * Updates the status with validation and timestamps
     */
    public void updateStatus(SettlementStatus newStatus, User approvedBy) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s", this.status, newStatus)
            );
        }

        this.status = newStatus;

        // Set specific timestamps and references for certain status transitions
        switch (newStatus) {
            case SETTLEMENT_APPROVED:
                this.settlementApprovedBy = approvedBy;
                break;
            case SETTLEMENT_TRANSACTION_CREATED:
                this.transactionCreatedAt = LocalDateTime.now();
                break;
            case SETTLEMENT_DONE:
            case COMPLETED:
                this.settledAt = LocalDateTime.now();
                break;
        }
    }

    /**
     * Checks if settlement is in a terminal state
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }

    /**
     * Checks if settlement is pending some action
     */
    public boolean isPending() {
        return status.isPending();
    }

    /**
     * Checks if payment processing is in progress
     */
    public boolean isPaymentInProgress() {
        return status.isPaymentInProgress();
    }

    /**
     * Calculates total deductions from individual components
     */
    public void calculateTotalDeductions() {
        BigDecimal total = BigDecimal.ZERO;
        
        if (outstandingRent != null) total = total.add(outstandingRent);
        if (outstandingCharges != null) total = total.add(outstandingCharges);
        if (damageCharges != null) total = total.add(damageCharges);
        if (cleaningCharges != null) total = total.add(cleaningCharges);
        if (otherDeductions != null) total = total.add(otherDeductions);
        
        this.totalDeductions = total;
    }

    /**
     * Calculates final settlement amount (deposit - deductions)
     */
    public void calculateFinalSettlementAmount() {
        calculateTotalDeductions();
        
        BigDecimal deposit = securityDeposit != null ? securityDeposit : BigDecimal.ZERO;
        this.finalSettlementAmount = deposit.subtract(totalDeductions);
        
        // Determine settlement type
        if (finalSettlementAmount.compareTo(BigDecimal.ZERO) >= 0) {
            this.settlementType = "OWNER_PAYABLE";
        } else {
            this.settlementType = "TENANT_PAYABLE";
            // Make amount positive for display purposes
            this.finalSettlementAmount = this.finalSettlementAmount.abs();
        }
    }

    /**
     * Checks if early settlement penalties should be applied
     */
    public boolean shouldApplyEarlyExitPenalties() {
        return earlySettlementRequested && requestedEndDate != null;
    }

    /**
     * Gets days until requested end date (negative if past)
     */
    public long getDaysUntilRequestedEndDate() {
        if (requestedEndDate == null) {
            return 0;
        }
        return LocalDate.now().until(requestedEndDate).getDays();
    }

    /**
     * Checks if settlement transaction can be created at any time
     */
    public boolean canCreateSettlementTransaction() {
        return status == SettlementStatus.SETTLEMENT_APPROVED || 
               status == SettlementStatus.CALCULATION_IN_PROGRESS;
    }

    /**
     * Marks room availability as updated
     */
    public void markRoomAvailabilityUpdated() {
        this.roomAvailabilityUpdated = true;
    }

    /**
     * Creates summary string for settlement
     */
    public String getSummary() {
        return String.format("Settlement %s - %s: ₹%.2f (%s)", 
            settlementId.toString().substring(0, 8),
            status,
            finalSettlementAmount != null ? finalSettlementAmount : BigDecimal.ZERO,
            settlementType != null ? settlementType : "PENDING"
        );
    }

    /**
     * Validates settlement data completeness
     */
    public boolean isDataComplete() {
        return tenant != null &&
               owner != null &&
               agreementId != null && !agreementId.trim().isEmpty() &&
               status != null;
    }

    // ─── Static Factory Methods ──────────────────────────────────────────────

    /**
     * Creates an early settlement request
     */
    public static EnhancedSettlementRequest createEarlySettlementRequest(
            String agreementId, User tenant, User owner, Room room, 
            LocalDate requestedEndDate, String tenantNotes) {
        
        return EnhancedSettlementRequest.builder()
                .agreementId(agreementId)
                .tenant(tenant)
                .owner(owner)
                .room(room)
                .requestedEndDate(requestedEndDate)
                .earlySettlementRequested(true)
                .tenantNotes(tenantNotes)
                .status(SettlementStatus.PENDING_OWNER_REVIEW)
                .build();
    }

    /**
     * Creates a settlement transaction request
     */
    public static EnhancedSettlementRequest createSettlementTransaction(
            String agreementId, User tenant, User owner, Room room,
            String settlementTransactionData) {
        
        return EnhancedSettlementRequest.builder()
                .agreementId(agreementId)
                .tenant(tenant)
                .owner(owner)
                .room(room)
                .settlementTransactionData(settlementTransactionData)
                .status(SettlementStatus.SETTLEMENT_TRANSACTION_CREATED)
                .transactionCreatedAt(LocalDateTime.now())
                .build();
    }

    // ─── toString and equals/hashCode ────────────────────────────────────────

    @Override
    public String toString() {
        return "EnhancedSettlementRequest{" +
                "settlementId=" + settlementId +
                ", agreementId='" + agreementId + '\'' +
                ", status=" + status +
                ", requestedEndDate=" + requestedEndDate +
                ", earlySettlementRequested=" + earlySettlementRequested +
                ", finalSettlementAmount=" + finalSettlementAmount +
                ", settlementType='" + settlementType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnhancedSettlementRequest that = (EnhancedSettlementRequest) o;
        return settlementId != null && settlementId.equals(that.settlementId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}