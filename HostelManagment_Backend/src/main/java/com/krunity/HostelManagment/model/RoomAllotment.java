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

    @OneToOne(fetch = FetchType.LAZY)
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

    // ─── Schedule ─────────────────────────────────────────────────────────────

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
}
