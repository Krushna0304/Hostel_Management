package com.krunity.HostelManagment.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneTimeMaintenanceCharge {
    private Boolean applicable;
    private BigDecimal amount;
    
    // One-time maintenance charges are collected at agreement acceptance
    @Builder.Default
    private PaymentTiming paymentTiming = PaymentTiming.AT_AGREEMENT;
    
    // Can be refundable or non-refundable
    @Builder.Default
    private Boolean refundable = false;
}