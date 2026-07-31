package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.ExtendAllotmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "extend_allotment_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ExtendAllotmentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "extend_request_id")
    private UUID extendRequestId;

    // References
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "current_agreement_id", nullable = false)
    private String currentAgreementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_room_id")
    private Room currentRoom;

    // Extension Details
    @Column(name = "new_plan_id", nullable = false)
    private UUID newPlanId;

    @Column(name = "extension_start_date", nullable = false)
    private LocalDate extensionStartDate;

    @Column(name = "extension_end_date", nullable = false)
    private LocalDate extensionEndDate;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ExtendAllotmentStatus status = ExtendAllotmentStatus.PENDING_OWNER_APPROVAL;

    // Financial Details
    @Column(name = "activation_amount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal activationAmount = BigDecimal.ZERO;

    @Column(name = "settlement_adjustment", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal settlementAdjustment = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "payment_reference")
    private String paymentReference;

    // New Agreement Reference (after creation)
    @Column(name = "new_agreement_id")
    private String newAgreementId;

    // Notes
    @Column(name = "tenant_notes", length = 500)
    private String tenantNotes;

    @Column(name = "owner_notes", length = 500)
    private String ownerNotes;

    // Timestamps
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "payment_completed_at")
    private LocalDateTime paymentCompletedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Business Methods

    /**
     * Updates the status of the extension request
     */
    public void updateStatus(ExtendAllotmentStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        
        // Set specific timestamps based on status
        switch (newStatus) {
            case APPROVED_PENDING_PAYMENT:
                this.approvedAt = LocalDateTime.now();
                break;
            case PAYMENT_COMPLETED:
                this.paymentCompletedAt = LocalDateTime.now();
                break;
            default:
                // No specific timestamp for other statuses
                break;
        }
    }

    /**
     * Calculates the total amount (activation amount +/- settlement adjustment)
     */
    public void calculateTotalAmount() {
        if (activationAmount != null && settlementAdjustment != null) {
            this.totalAmount = activationAmount.add(settlementAdjustment);
        } else if (activationAmount != null) {
            this.totalAmount = activationAmount;
        } else {
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    /**
     * Sets expiration time from current moment plus specified hours
     */
    public void setPaymentExpirationFromNow(int hours) {
        this.expiresAt = LocalDateTime.now().plusHours(hours);
    }

    /**
     * Checks if the extension request has expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Gets remaining hours until expiration
     */
    public long getRemainingHoursUntilExpiration() {
        if (expiresAt == null) {
            return Long.MAX_VALUE;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiresAt)) {
            return 0;
        }
        
        return java.time.Duration.between(now, expiresAt).toHours();
    }

    /**
     * Checks if the request can be cancelled
     */
    public boolean canBeCancelled() {
        return status == ExtendAllotmentStatus.PENDING_OWNER_APPROVAL || 
               status == ExtendAllotmentStatus.APPROVED_PENDING_PAYMENT;
    }

    /**
     * Checks if payment can be processed
     */
    public boolean canProcessPayment() {
        return status == ExtendAllotmentStatus.APPROVED_PENDING_PAYMENT && !isExpired();
    }

    /**
     * Validation constraints check
     */
    public boolean isValidExtensionPeriod() {
        return extensionEndDate != null && extensionStartDate != null && 
               extensionEndDate.isAfter(extensionStartDate);
    }

    /**
     * Checks if amounts are non-negative
     */
    public boolean hasValidAmounts() {
        return activationAmount != null && activationAmount.compareTo(BigDecimal.ZERO) >= 0 &&
               totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) >= 0;
    }
}