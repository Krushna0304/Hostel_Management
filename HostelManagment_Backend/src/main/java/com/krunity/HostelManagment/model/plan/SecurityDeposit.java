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
public class SecurityDeposit {
    private BigDecimal amount;
    private Boolean refundable;
    private Integer refundProcessingDays;
    
    // New field to specify when this charge is collected
    @Builder.Default
    private PaymentTiming paymentTiming = PaymentTiming.AT_AGREEMENT;
}

