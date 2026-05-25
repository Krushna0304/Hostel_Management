package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettlementApprovalDto {
    
    @DecimalMin(value = "0.0", message = "Damage charges cannot be negative")
    private BigDecimal damageCharges = BigDecimal.ZERO;
    
    @DecimalMin(value = "0.0", message = "Cleaning charges cannot be negative")
    private BigDecimal cleaningCharges = BigDecimal.ZERO;
    
    @DecimalMin(value = "0.0", message = "Other deductions cannot be negative")
    private BigDecimal otherDeductions = BigDecimal.ZERO;
    
    @Size(max = 500, message = "Owner notes cannot exceed 500 characters")
    private String ownerNotes;
    
    @Size(max = 1000, message = "Damage description cannot exceed 1000 characters")
    private String damageDescription;
    
    private boolean approved = true;
}