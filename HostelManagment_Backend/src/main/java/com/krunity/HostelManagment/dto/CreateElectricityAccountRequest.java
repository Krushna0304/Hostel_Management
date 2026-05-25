package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateElectricityAccountRequest {
    
    @NotNull(message = "Room ID is required")
    private UUID roomId;
    
    @NotBlank(message = "Account number is required")
    private String accountNumber;
}