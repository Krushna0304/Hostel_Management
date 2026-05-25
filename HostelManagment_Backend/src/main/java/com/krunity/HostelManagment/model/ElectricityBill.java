package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.BillStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "electricity_bills")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ElectricityBill {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "bill_id", updatable = false, nullable = false)
    private UUID billId;
    
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    
    @Column(name = "room_id", nullable = false)
    private UUID roomId;
    
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    
    @Column(name = "tenant_id")
    private UUID tenantId;
    
    @Column(name = "bill_month", nullable = false)
    private Integer billMonth; // 1-12
    
    @Column(name = "bill_year", nullable = false)
    private Integer billYear;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;
    
    @Column(name = "remaining_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal remainingAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BillStatus status = BillStatus.PENDING;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "notes")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private ElectricityAccount electricityAccount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    private Room room;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private User owner;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private User tenant;
    
    // Helper method to update remaining amount
    public void updateRemainingAmount() {
        this.remainingAmount = this.totalAmount.subtract(this.paidAmount);
        
        // Update status based on payment
        if (this.remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.status = BillStatus.PAID;
        } else if (this.paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = BillStatus.PARTIAL;
        } else {
            this.status = BillStatus.PENDING;
        }
    }
}