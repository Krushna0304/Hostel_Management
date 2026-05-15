package com.krunity.HostelManagment.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class PaymentLedgerResponse {
    private UUID planId;
    private String agreementId;
    private Long installmentAmount;
    private String paymentFrequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalPaid;
    private Long totalPending;
    private Integer overdueCount;
    private List<InstallmentResponse> installments;
}
