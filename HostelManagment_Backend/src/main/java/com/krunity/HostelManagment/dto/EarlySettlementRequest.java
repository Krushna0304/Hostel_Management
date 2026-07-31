package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EarlySettlementRequest {
    
    @NotBlank(message = "Agreement ID is required")
    private String agreementId;
    
    @NotNull(message = "Requested end date is required")
    private LocalDate requestedEndDate;
    
    @NotBlank(message = "Reason is required")
    private String reason;
    
    private String tenantNotes;
}