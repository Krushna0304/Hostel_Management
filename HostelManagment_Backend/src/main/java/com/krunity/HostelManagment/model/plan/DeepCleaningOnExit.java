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
public class DeepCleaningOnExit {
    private Boolean applicable;
    private BigDecimal amount;
    
    // New field to specify when this charge is collected
    @Builder.Default
    private PaymentTiming paymentTiming = PaymentTiming.AT_END;
    
    // New field to specify if it's refundable (deducted from deposit)
    @Builder.Default
    private Boolean refundable = false;
}

