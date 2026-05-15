package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.ChargeCategory;
import com.krunity.HostelManagment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "other_charges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "charge_id", updatable = false, nullable = false)
    private UUID chargeId;

    @Column(name = "charge_name", nullable = false, length = 100)
    private String chargeName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ChargeCategory category; // OTHER_CHARGE_TENANT or OTHER_CHARGE_ROOM

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // Owner who created this charge
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // For tenant-specific charges
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private User tenant;

    // For room-based charges
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    // Hostel context
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "installment_enabled")
    @Builder.Default
    private Boolean installmentEnabled = false;

    @Column(name = "installment_count")
    private Integer installmentCount;

    @Column(name = "installment_amount", precision = 10, scale = 2)
    private BigDecimal installmentAmount;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isTenantSpecific() {
        return category == ChargeCategory.OTHER_CHARGE_TENANT && tenant != null;
    }

    public boolean isRoomBased() {
        return category == ChargeCategory.OTHER_CHARGE_ROOM && room != null;
    }

    public BigDecimal getRemainingAmount() {
        if (paidAmount == null) {
            return amount;
        }
        return amount.subtract(paidAmount);
    }

    public boolean isFullyPaid() {
        return paymentStatus == PaymentStatus.COMPLETED || 
               (paidAmount != null && paidAmount.compareTo(amount) >= 0);
    }
}