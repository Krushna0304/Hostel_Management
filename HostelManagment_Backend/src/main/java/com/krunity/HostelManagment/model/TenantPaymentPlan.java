package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.PaymentFrequency;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "tenant_payment_plans",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "payment_type_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TenantPaymentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "plan_id", updatable = false, nullable = false)
    private UUID planId;

    /* -------------------- Relationships -------------------- */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "payment_type_id", nullable = false)
//    private PaymentType paymentType;


//    @Column(name = "next_due_date")
//    private LocalDate nextDueDate;

    /* -------------------- Payment Details -------------------- */

    @Column(nullable = false)
    private String TenantPlanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_frequency", nullable = false)
    private PaymentFrequency paymentFrequency;

    @Column(name = "deposit_amount", nullable = false)
    private Long depositAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "pending_installments")
    private Integer pendingInstallments;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
