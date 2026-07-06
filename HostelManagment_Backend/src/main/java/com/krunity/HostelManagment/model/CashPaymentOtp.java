package com.krunity.HostelManagment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A single generated OTP. It is intentionally decoupled from any payment type —
 * what the OTP authorises is recorded in {@link PaymentOtp} rows that reference it.
 * One OTP can authorise many requests (e.g. a "pay all" across several bills).
 */
@Entity
@Table(name = "cash_payment_otps")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CashPaymentOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "otp_id", updatable = false, nullable = false)
    private UUID otpId;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash; // Encoded (BCrypt) OTP

    @Column(name = "expiry_time", nullable = false)
    private Instant expiryTime;

    @Column(name = "owner_phone", nullable = false)
    private String ownerPhone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
