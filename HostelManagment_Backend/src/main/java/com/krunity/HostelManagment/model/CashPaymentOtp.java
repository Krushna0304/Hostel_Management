package com.krunity.HostelManagment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

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
    
    @Column(name = "agreement_id", nullable = true)
    private String agreementId;
    
    @Column(name = "schedule_id")
    private UUID scheduleId;
    
    @Column(name = "charge_id")
    private String chargeId;
    
    @Column(name = "settlement_id")
    private String settlementId;
    
    @Column(name = "electricity_bill_id")
    private UUID electricityBillId;
    
    @Column(name = "owner_phone", nullable = false)
    private String ownerPhone;
    
    @Column(name = "otp_hash", nullable = false)
    private String otpHash; // Encoded OTP
    
    @Column(name = "expiry_time", nullable = false)
    private Instant expiryTime;
    
    @Column(name = "used", nullable = false)
    @Builder.Default
    private Boolean used = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
