package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_request_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "schedule_id", updatable = false, nullable = false)
    private UUID scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TenantPaymentPlan tenantPaymentPlan;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private TransactionStatus paymentStatus;

    // For partial payments — how much has been paid so far
    @Column(name = "paid_amount", nullable = false)
    @Builder.Default
    private Long paidAmount = 0L;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Late fee applied on this installment (calculated by scheduled job)
    @Column(name = "late_fee_applied", nullable = false)
    @Builder.Default
    private Long lateFeeApplied = 0L;

    // FK to the Transaction record when this installment is paid
    @Column(name = "transaction_id")
    private UUID transactionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
