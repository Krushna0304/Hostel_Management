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

@Entity
@Table(name = "settlement_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SettlementRequest {

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

    // Financial Summary
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

    // Enhanced Settlement System Fields
    @Column(name = "settlement_transaction_data", columnDefinition = "TEXT")
    private String settlementTransactionData;

    @Column(name = "early_settlement_requested")
    @Builder.Default
    private boolean earlySettlementRequested = false;

    @Column(name = "settlement_approved_by")
    private UUID settlementApprovedBy;

    @Column(name = "auto_settlement_eligible")
    @Builder.Default
    private boolean autoSettlementEligible = false;

    @Column(name = "transaction_created_at")
    private LocalDateTime transactionCreatedAt;

    @Column(name = "settlement_requested_at")
    private LocalDateTime settlementRequestedAt;

    @Column(name = "settlement_approved_at")
    private LocalDateTime settlementApprovedAt;

    @Column(name = "room_availability_updated")
    @Builder.Default
    private boolean roomAvailabilityUpdated = false;

    // Business Methods
    
    /**
     * Updates the status of the settlement request
     */
    public void updateStatus(SettlementStatus newStatus, User updatedBy) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        
        // Set specific timestamps and fields based on status
        switch (newStatus) {
            case SETTLEMENT_APPROVED:
                this.settlementApprovedAt = LocalDateTime.now();
                if (updatedBy != null) {
                    this.settlementApprovedBy = updatedBy.getUserId();
                }
                break;
            case SETTLEMENT_DONE:
            case COMPLETED:
                this.settledAt = LocalDateTime.now();
                break;
            default:
                // No specific timestamp for other statuses
                break;
        }
    }

    /**
     * Updates the status without specifying updater
     */
    public void updateStatus(SettlementStatus newStatus, String updaterType) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        
        // Set specific timestamps based on status
        switch (newStatus) {
            case PENDING_OWNER_REVIEW:
                this.settlementRequestedAt = LocalDateTime.now();
                break;
            case SETTLEMENT_DONE:
            case COMPLETED:
                this.settledAt = LocalDateTime.now();
                break;
            default:
                break;
        }
    }

    /**
     * Marks room availability as updated
     */
    public void markRoomAvailabilityUpdated() {
        this.roomAvailabilityUpdated = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if this is an early settlement request
     */
    public boolean isEarlySettlementRequested() {
        return earlySettlementRequested;
    }

    /**
     * Checks if room availability has been updated
     */
    public boolean isRoomAvailabilityUpdated() {
        return roomAvailabilityUpdated;
    }

    /**
     * Gets settlement transaction data
     */
    public String getSettlementTransactionData() {
        return settlementTransactionData;
    }

    /**
     * Sets settlement transaction data
     */
    public void setSettlementTransactionData(String settlementTransactionData) {
        this.settlementTransactionData = settlementTransactionData;
        this.updatedAt = LocalDateTime.now();
    }
}