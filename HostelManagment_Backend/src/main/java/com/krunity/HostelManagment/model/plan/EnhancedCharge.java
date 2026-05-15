package com.krunity.HostelManagment.model.plan;

import com.krunity.HostelManagment.enums.ChargeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedCharge {
    private String chargeName;
    private BigDecimal amount;
    private ChargeCategory category;
    private PaymentTiming timing;
    private Boolean refundable;
    private String description;
    private Boolean applicable;
    
    // For recurring charges
    private String frequency; // MONTHLY, QUARTERLY, etc.
    
    // For deposit charges
    private Integer refundProcessingDays;
}