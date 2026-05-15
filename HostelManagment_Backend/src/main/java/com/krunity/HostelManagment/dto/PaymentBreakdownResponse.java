package com.krunity.HostelManagment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentBreakdownResponse {
    // Amounts collected at agreement acceptance
    private BigDecimal agreementTimeRefundable = BigDecimal.ZERO;     // Security deposit
    private BigDecimal agreementTimeNonRefundable = BigDecimal.ZERO;  // One-time charges
    
    // Amount included in each installment
    private BigDecimal installmentAmount = BigDecimal.ZERO;           // Base rent + monthly charges
    
    // Amounts collected at agreement end
    private BigDecimal endTimeRefundable = BigDecimal.ZERO;           // Deducted from deposit
    private BigDecimal endTimeNonRefundable = BigDecimal.ZERO;        // Additional charges
    
    // Total amounts for reference
    private BigDecimal totalAgreementTime = BigDecimal.ZERO;
    private BigDecimal totalEndTime = BigDecimal.ZERO;
    
    // Breakdown details
    private String currency = "INR";
    private String description = "Payment breakdown based on plan configuration";
}