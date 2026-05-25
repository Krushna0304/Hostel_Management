package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SettlementRequestDto {
    
    @NotBlank(message = "Agreement ID is required")
    private String agreementId;
    
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String tenantNotes;
}