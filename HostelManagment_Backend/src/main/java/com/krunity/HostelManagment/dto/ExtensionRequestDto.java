package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExtensionRequestDto {
    
    @NotBlank(message = "Current agreement ID is required")
    private String currentAgreementId;
    
    @NotBlank(message = "Plan ID is required")
    private String planId;
    
    private String tenantNotes;
}
