package com.krunity.HostelManagment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExtensionApprovalDto {
    
    private boolean approved = true;
    
    private String ownerNotes;
    
    private BigDecimal finalActivationAmount;
}