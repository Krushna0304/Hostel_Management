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
public class MonthlyCleaningCharge {
    private Boolean applicable;
    private BigDecimal amount;
    
    // This is always included in installments and non-refundable
    @Builder.Default
    private PaymentTiming paymentTiming = PaymentTiming.IN_INSTALLMENTS;
    
    @Builder.Default
    private Boolean refundable = false;
}