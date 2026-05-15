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
public class MonthlyMaintenanceCharge {
    private Boolean applicable;
    private BigDecimal amount;
    
    // Monthly maintenance charges are included in installments
    @Builder.Default
    private PaymentTiming paymentTiming = PaymentTiming.IN_INSTALLMENTS;
    
    // Monthly charges are non-refundable
    @Builder.Default
    private Boolean refundable = false;
}