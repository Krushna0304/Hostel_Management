package com.krunity.HostelManagment.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceCharges {
    @Builder.Default
    private Boolean included = true;
    @Builder.Default
    private List<String> covers = new ArrayList<>();
    
    // New fields for different types of maintenance charges
    private OneTimeMaintenanceCharge oneTimeMaintenanceCharge;
    private MonthlyMaintenanceCharge monthlyMaintenanceCharge;
}

