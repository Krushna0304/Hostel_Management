package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.ChargeCategory;
import com.krunity.HostelManagment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtherChargeResponse {

    private UUID chargeId;
    private String chargeName;
    private String description;
    private BigDecimal amount;
    private ChargeCategory category;
    private PaymentStatus paymentStatus;

    // Owner details
    private UUID ownerId;
    private String ownerName;

    // Tenant details (for tenant-specific charges)
    private UUID tenantId;
    private String tenantName;

    // Room details (for room-based charges)
    private UUID roomId;
    private String roomNumber;
    private List<TenantSummary> roomTenants; // For room-based charges

    // Hostel details
    private UUID hostelId;
    private String hostelName;

    private LocalDateTime dueDate;
    private LocalDateTime paidDate;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;

    // Installment details
    private Boolean installmentEnabled;
    private Integer installmentCount;
    private BigDecimal installmentAmount;
    private List<InstallmentSummary> installments;

    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantSummary {
        private UUID tenantId;
        private String tenantName;
        private String email;
        private String phoneNumber;
        private BigDecimal splitAmount; // For room-based charges
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallmentSummary {
        private UUID installmentId;
        private Integer installmentNumber;
        private BigDecimal amount;
        private LocalDateTime dueDate;
        private PaymentStatus paymentStatus;
        private LocalDateTime paidDate;
        private BigDecimal paidAmount;
        private BigDecimal remainingAmount;
        private Boolean isOverdue;
    }
}