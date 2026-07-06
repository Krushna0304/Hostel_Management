package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.PaymentStatus;
import com.krunity.HostelManagment.enums.TransactionMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single tenant's share of a room-based "split among tenants" {@link OtherCharge}
 * that is paid in full (no installments). Mirrors {@code ElectricityPayment}: one
 * PENDING row is created per eligible tenant when the charge is created, and each
 * tenant pays their own share.
 */
@Entity
@Table(name = "other_charge_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherChargePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID paymentId;

    @Column(name = "charge_id", nullable = false)
    private UUID chargeId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    // The tenant's share of the charge.
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // A share starts PENDING and becomes COMPLETED once the tenant pays it.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // Null until the tenant actually pays this share.
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode")
    private TransactionMode paymentMode;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id", insertable = false, updatable = false)
    private OtherCharge otherCharge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private User tenant;
}
