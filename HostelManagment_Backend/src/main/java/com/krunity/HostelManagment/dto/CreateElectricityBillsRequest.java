package com.krunity.HostelManagment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateElectricityBillsRequest {
    
    @NotNull(message = "Bill month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer billMonth;
    
    @NotNull(message = "Bill year is required")
    @Min(value = 2020, message = "Year must be valid")
    private Integer billYear;
    
    @NotEmpty(message = "At least one bill is required")
    @Valid
    private List<BillItem> bills;
    
    @Data
    public static class BillItem {
        @NotNull(message = "Account ID is required")
        private UUID accountId;
        
        @NotNull(message = "Amount is required")
        @Min(value = 0, message = "Amount must be positive")
        private BigDecimal amount;
        
        private String notes;
    }
}