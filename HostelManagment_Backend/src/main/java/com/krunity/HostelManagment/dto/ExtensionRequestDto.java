package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ExtensionRequestDto {
    
    @NotBlank(message = "Current agreement ID is required")
    private String currentAgreementId;
    
    @NotNull(message = "Plan ID is required")
    private UUID planId;
    
    private String tenantNotes;
}