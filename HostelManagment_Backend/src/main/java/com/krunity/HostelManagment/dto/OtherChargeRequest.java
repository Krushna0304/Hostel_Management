package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.ChargeCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtherChargeRequest {

    @NotBlank(message = "Charge name is required")
    @Size(max = 100, message = "Charge name must not exceed 100 characters")
    private String chargeName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Amount must have at most 8 integer digits and 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Category is required")
    private ChargeCategory category; // OTHER_CHARGE_TENANT or OTHER_CHARGE_ROOM

    // For tenant-specific charges
    private UUID tenantId;

    // For room-based charges
    private UUID roomId;

    @NotNull(message = "Hostel ID is required")
    private UUID hostelId;

    private LocalDateTime dueDate;

    @Builder.Default
    private Boolean installmentEnabled = false;

    @Min(value = 2, message = "Installment count must be at least 2")
    @Max(value = 12, message = "Installment count must not exceed 12")
    private Integer installmentCount;

    // Validation method
    public boolean isValid() {
        if (category == ChargeCategory.OTHER_CHARGE_TENANT) {
            return tenantId != null;
        } else if (category == ChargeCategory.OTHER_CHARGE_ROOM) {
            return roomId != null;
        }
        return false;
    }
}