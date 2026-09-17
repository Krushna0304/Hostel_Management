package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "room_allotments")
@Builder
public class RoomAllotment {

    // ─── Identity ─────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "allotment_id", updatable = false, nullable = false)
    private UUID allotmentId;

    // ─── Relationships ────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // A tenant can have a historical/current/future allotment chain when an
    // agreement is extended. A one-to-one mapping creates a UNIQUE constraint
    // on tenant_id and prevents that valid history from being stored.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @Column(nullable = false)
    private String agreementId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_plan_id", nullable = false)
    private TenantPaymentPlan paymentPlanId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_transaction_id", nullable = false)
    private Transaction depositTransactionId;

    // ─── NEW: Extension Tracking ─────────────────────────────────────────────

    /**
     * Reference to the extension request that created this allotment (if applicable)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extension_request_id")
    private ExtendAllotmentRequest extensionRequest;

    /**
     * Flag indicating if this allotment is an extension of a previous one
     */
    @Column(name = "is_extension", columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isExtension = false;

    /**
     * Reference to the parent allotment if this is an extension
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_allotment_id")
    private RoomAllotment parentAllotment;

    // ─── Schedule ─────────────────────────────────────────────────────────────

    @Column(name = "allotment_date")
    private LocalDate allotmentDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Denormalized from the plan snapshot at allotment creation.
     * Used by the cron job: SETTLEMENT_PENDING triggers when
     * currentDate >= (endDate - noticePeriodMonths).
     */
    @Column(name = "notice_period_months")
    private Integer noticePeriodMonths;

    // ─── Status ───────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "room_allotment_status", nullable = false)
    private RoomAllotmentStatus roomAllotmentStatus;

    // ─── NEW: Settlement Task Management ─────────────────────────────────────

    /**
     * Type of settlement task pending (if status is SETTLEMENT_TASK)
     */
    @Column(name = "settlement_task_type", length = 50)
    private String settlementTaskType;

    /**
     * Additional metadata for settlement tasks (JSON format)
     */
    @Column(name = "settlement_task_data", columnDefinition = "TEXT")
    private String settlementTaskData;

    // ─── Settlement flags ─────────────────────────────────────────────────────

    /**
     * True when the tenant requested settlement before the notice-period window opened.
     * Set automatically in requestSettlement() based on endDate vs. noticePeriodMonths.
     *
     * columnDefinition supplies a DEFAULT so that existing rows are backfilled when
     * Hibernate adds this column via ddl-auto=update.
     */
    @Column(name = "early_exit", columnDefinition = "boolean default false")
    @Builder.Default
    private boolean earlyExit = false;

    @Column(name = "settlement_requested_at")
    private LocalDateTime settlementRequestedAt;

    @Column(name = "settlement_approved_at")
    private LocalDateTime settlementApprovedAt;

    // ─── Dual LEFT confirmation ───────────────────────────────────────────────

    /**
     * Tenant's confirmation that they have physically vacated.
     * columnDefinition supplies a DEFAULT so existing rows are backfilled on schema update.
     */
    @Column(name = "tenant_marked_left", columnDefinition = "boolean default false")
    @Builder.Default
    private boolean tenantMarkedLeft = false;

    /**
     * Owner's confirmation that the tenant has physically vacated.
     * columnDefinition supplies a DEFAULT so existing rows are backfilled on schema update.
     */
    @Column(name = "owner_marked_left", columnDefinition = "boolean default false")
    @Builder.Default
    private boolean ownerMarkedLeft = false;

    @Column(name = "tenant_left_at")
    private LocalDateTime tenantLeftAt;

    @Column(name = "owner_left_at")
    private LocalDateTime ownerLeftAt;

    // ─── Audit trail ─────────────────────────────────────────────────────────

    /** ISO actor of the most recent status change: SYSTEM | TENANT | OWNER */
    @Column(name = "last_status_changed_by", length = 10)
    private String lastStatusChangedBy;

    @Column(name = "last_status_changed_at")
    private LocalDateTime lastStatusChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ─── NEW: Enhanced Business Logic ────────────────────────────────────────

    /**
     * Checks if this allotment requires tenant action
     */
    public boolean requiresTenantAction() {
        return RoomAllotmentStatus.tenantActionPendingStatuses().contains(roomAllotmentStatus);
    }

    /**
     * Checks if this allotment is in settlement workflow
     */
    public boolean isInSettlementWorkflow() {
        return RoomAllotmentStatus.settlementInProgressStatuses().contains(roomAllotmentStatus);
    }

    /**
     * Checks if extension requests are allowed from current status
     */
    public boolean canRequestExtension() {
        return RoomAllotmentStatus.extensionRequestableStatuses().contains(roomAllotmentStatus);
    }

    /**
     * Gets the allocation priority for room allocation calculations
     */
    public int getAllocationPriority() {
        return roomAllotmentStatus.getAllocationPriority();
    }

    /**
     * Checks if this is an extended allotment
     */
    public boolean isExtendedAllotment() {
        return isExtension && parentAllotment != null;
    }

    /**
     * Gets the extension chain depth (0 for original, 1 for first extension, etc.)
     */
    public int getExtensionDepth() {
        if (!isExtension || parentAllotment == null) {
            return 0;
        }
        return 1 + parentAllotment.getExtensionDepth();
    }

    /**
     * Gets the root allotment in the extension chain
     */
    public RoomAllotment getRootAllotment() {
        if (!isExtension || parentAllotment == null) {
            return this;
        }
        return parentAllotment.getRootAllotment();
    }

    /**
     * Checks if settlement task is pending
     */
    public boolean hasSettlementTaskPending() {
        return roomAllotmentStatus == RoomAllotmentStatus.SETTLEMENT_TASK &&
               settlementTaskType != null && !settlementTaskType.trim().isEmpty();
    }

    /**
     * Sets settlement task information
     */
    public void setSettlementTask(String taskType, String taskData) {
        this.settlementTaskType = taskType;
        this.settlementTaskData = taskData;
        if (roomAllotmentStatus != RoomAllotmentStatus.SETTLEMENT_TASK) {
            updateStatus(RoomAllotmentStatus.SETTLEMENT_TASK, "SYSTEM");
        }
    }

    /**
     * Clears settlement task information
     */
    public void clearSettlementTask() {
        this.settlementTaskType = null;
        this.settlementTaskData = null;
    }

    /**
     * Updates status with validation and audit trail
     */
    public void updateStatus(RoomAllotmentStatus newStatus, String changedBy) {
        if (!roomAllotmentStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s for allotment %s", 
                    roomAllotmentStatus, newStatus, allotmentId)
            );
        }
        
        this.roomAllotmentStatus = newStatus;
        this.lastStatusChangedBy = changedBy;
        this.lastStatusChangedAt = LocalDateTime.now();

        // Clear settlement task if transitioning away from SETTLEMENT_TASK
        if (newStatus != RoomAllotmentStatus.SETTLEMENT_TASK) {
            clearSettlementTask();
        }
    }

    /**
     * Creates an extension of this allotment
     */
    public RoomAllotment createExtension(ExtendAllotmentRequest extensionRequest, 
                                       String newAgreementId, 
                                       TenantPaymentPlan newPaymentPlan,
                                       Transaction newDepositTransaction) {
        return RoomAllotment.builder()
                .room(this.room)
                .tenant(this.tenant)
                .agreementId(newAgreementId)
                .paymentPlanId(newPaymentPlan)
                .depositTransactionId(newDepositTransaction)
                .extensionRequest(extensionRequest)
                .isExtension(true)
                .parentAllotment(this)
                .startDate(extensionRequest.getExtensionStartDate())
                .endDate(extensionRequest.getExtensionEndDate())
                .noticePeriodMonths(this.noticePeriodMonths) // Inherit from parent
                .roomAllotmentStatus(RoomAllotmentStatus.UPCOMING)
                .lastStatusChangedBy("SYSTEM")
                .lastStatusChangedAt(LocalDateTime.now())
                .build();
    }
}
