package com.krunity.HostelManagment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class ExistingTenantOnboardingRequest {
    @NotNull private UUID hostelId;
    @NotBlank private String planId;
    /** Currently only PG_ROOM is supported by the batch room-allotment workflow. */
    @NotBlank private String agreementType;
    private UUID defaultFloorId;
    @NotEmpty @Valid private List<TenantRow> tenants;

    @Data
    public static class TenantRow {
        @NotBlank private String name;
        @NotBlank private String phoneNumber;
        @NotBlank private String roomNumber;
        private UUID floorId;
        @NotNull private LocalDate startDate;
        @NotNull private LocalDate endDate;
    }
}
