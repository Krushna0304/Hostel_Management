package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "other_charge_installments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherChargeInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "installment_id", updatable = false, nullable = false)
    private UUID installmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id", nullable = false)
    private OtherCharge otherCharge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant; // The tenant responsible for this installment

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public BigDecimal getRemainingAmount() {
        if (paidAmount == null) {
            return amount;
        }
        return amount.subtract(paidAmount);
    }

    public boolean isOverdue() {
        return paymentStatus != PaymentStatus.COMPLETED && 
               dueDate.isBefore(LocalDateTime.now());
    }

    public boolean isFullyPaid() {
        return paymentStatus == PaymentStatus.COMPLETED || 
               (paidAmount != null && paidAmount.compareTo(amount) >= 0);
    }
}