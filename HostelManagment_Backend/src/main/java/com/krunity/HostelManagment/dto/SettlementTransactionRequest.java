package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SettlementTransactionRequest {
    
    @NotBlank(message = "Agreement ID is required")
    private String agreementId;
    
    @NotNull(message = "Calculation date is required")
    private LocalDate calculationDate;
    
    private String notes;
}