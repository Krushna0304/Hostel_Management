package com.krunity.HostelManagment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ElectricityAccountDto {
    private UUID accountId;
    private UUID roomId;
    private String roomNumber;
    private UUID hostelId;
    private String hostelName;
    private String accountNumber;
    private UUID ownerId;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}