package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.BillingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "monthly_billing_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyBillingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "record_id", updatable = false, nullable = false)
    private UUID recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_plan_id", nullable = false)
    private TenantPaymentPlan paymentPlan;

    @Column(name = "month_number", nullable = false)
    private Integer monthNumber;

    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    @Column(name = "rent_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal rentAmount;

    @Column(name = "recurring_charges", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal recurringCharges = BigDecimal.ZERO;

    @Column(name = "assigned_installment_id")
    private UUID assignedInstallmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BillingStatus status = BillingStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}