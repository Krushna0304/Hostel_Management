package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateRoomAgreementRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Room ID is required")
    private UUID roomId;

    @NotBlank(message = "Plan ID is required")
    private String planId; // Reference to the selected plan

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @FutureOrPresent(message = "End date must be today or in the future")
    private LocalDate endDate;

    // Legacy fields - kept for backward compatibility but deprecated
    @Deprecated
    private BigDecimal rent;
    @Deprecated
    private BigDecimal deposit;
    @Deprecated
    private BigDecimal cleaningCharges;
    @Deprecated
    private BigDecimal maintenanceCharges;
    @Deprecated
    private String lightBillPolicy;
    @Deprecated
    private List<String> facilities;
    @Deprecated
    private Boolean parkingAllowed;
}

