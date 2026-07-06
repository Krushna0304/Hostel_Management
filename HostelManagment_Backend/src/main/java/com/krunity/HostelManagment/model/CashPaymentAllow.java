package com.krunity.HostelManagment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Per-owner, per-method switch controlling whether a payment flow may be settled in
 * cash via OTP. Three rows (Electricity Bills, Installments, Other Charges) are
 * seeded when an owner account is created, defaulting to disabled.
 */
@Entity
@Table(
        name = "cash_payment_allow",
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "method_name"})
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CashPaymentAllow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cp_id", updatable = false, nullable = false)
    private UUID cpId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    // Stored as the CashPaymentMethod enum name.
    @Column(name = "method_name", nullable = false)
    private String methodName;

    @Column(name = "is_allowed", nullable = false)
    @Builder.Default
    private Boolean isAllowed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
