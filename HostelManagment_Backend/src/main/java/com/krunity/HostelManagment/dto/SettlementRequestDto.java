package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SettlementRequestDto {
    
    @NotBlank(message = "Agreement ID is required")
    private String agreementId;

    @NotNull(message = "Requested move-out date is required")
    @FutureOrPresent(message = "Requested move-out date must be today or in the future")
    private LocalDate requestedEndDate;
    
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String tenantNotes;
}