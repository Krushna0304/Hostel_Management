package com.krunity.HostelManagment.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class TenantDashboardResponse {
    // Allotment info
    private UUID allotmentId;
    private String roomNumber;
    private String hostelName;
    private String hostelAddress;
    private Integer floorNumber;
    private String allotmentStatus;
    private LocalDate allotmentDate;

    // Payment plan summary
    private UUID planId;
    private String agreementId;
    private Long installmentAmount;
    private String paymentFrequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer pendingInstallments;
    private Long totalPaid;
    private Long totalPending;
    private Integer overdueCount;

    // Next due installment
    private InstallmentResponse nextDueInstallment;
}
