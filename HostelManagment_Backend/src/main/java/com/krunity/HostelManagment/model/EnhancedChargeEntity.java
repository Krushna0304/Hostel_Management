package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.ChargeCategory;
import com.krunity.HostelManagment.model.plan.PaymentTiming;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "enhanced_charges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnhancedChargeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "charge_id", updatable = false, nullable = false)
    private UUID chargeId;

    @Column(name = "plan_id", nullable = false)
    private String planId; // Reference to MongoDB RoomAgreementPlan

    @Column(name = "charge_name", nullable = false, length = 100)
    private String chargeName;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ChargeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "timing", nullable = false)
    private PaymentTiming timing;

    @Column(name = "refundable")
    @Builder.Default
    private Boolean refundable = false;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "applicable")
    @Builder.Default
    private Boolean applicable = true;

    @Column(name = "frequency", length = 50)
    private String frequency;

    @Column(name = "refund_processing_days")
    private Integer refundProcessingDays;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}