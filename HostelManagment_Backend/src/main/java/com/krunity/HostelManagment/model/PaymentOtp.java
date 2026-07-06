package com.krunity.HostelManagment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Links a generated {@link CashPaymentOtp} to a specific request being paid (e.g. a
 * bill id, installment schedule id, or charge id) under a particular
 * {@link CashPaymentAllow} method config. One OTP can have many of these rows,
 * which is what enables a single OTP to authorise a "pay all".
 */
@Entity
@Table(name = "payment_otps")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PaymentOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "otp_id", nullable = false)
    private UUID otpId;

    // References cash_payment_allow.cp_id (owner + method).
    @Column(name = "cp_id", nullable = false)
    private UUID cpId;

    // The id of the entity being paid (bill / schedule / charge / agreement ...).
    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "used", nullable = false)
    @Builder.Default
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
