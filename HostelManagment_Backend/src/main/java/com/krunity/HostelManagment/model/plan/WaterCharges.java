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
public class WaterCharges {
    private String type; // METERED, FIXED, INCLUDED
    private BigDecimal monthlyAmount;
    
    // Water charges are typically included in installments
    @Builder.Default
    private PaymentTiming paymentTiming = PaymentTiming.IN_INSTALLMENTS;
    
    @Builder.Default
    private Boolean refundable = false;
}

