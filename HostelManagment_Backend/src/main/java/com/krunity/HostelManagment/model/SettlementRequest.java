package com.krunity.HostelManagment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.krunity.HostelManagment.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
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
}