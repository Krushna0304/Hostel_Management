package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFlatAgreementRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Room ID is required")
    private UUID roomId;

    @NotBlank(message = "Plan ID is required")
    private String planId;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    /**
     * Co-tenant names for flat agreements.
     * The maximum number of co-tenants is dynamically validated based on the selected room's available beds.
     * Formula: Max co-tenants = room.availableBeds - 1 (1 bed reserved for primary tenant)
     * This validation is performed in the service layer to ensure room capacity is not exceeded.
     */
    @Builder.Default
    private List<@Size(max = 100, message = "Co-tenant name must not exceed 100 characters") String> coTenantNames = new ArrayList<>();
}
