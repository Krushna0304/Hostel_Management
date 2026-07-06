package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.PaymentStatus;
import com.krunity.HostelManagment.enums.TransactionMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "electricity_payments")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ElectricityPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID paymentId;
    
    @Column(name = "bill_id", nullable = false)
    private UUID billId;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    // The tenant's share of the bill (set when the bill is created and split).
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Null until the tenant actually pays this share.
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode")
    private TransactionMode paymentMode;

    // A share starts PENDING and becomes COMPLETED once the tenant pays it.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // Timestamp when this share was paid.
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_reference")
    private String paymentReference;
    
    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;
    
    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;
    
    @Column(name = "notes")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", insertable = false, updatable = false)
    private ElectricityBill electricityBill;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private User tenant;
}