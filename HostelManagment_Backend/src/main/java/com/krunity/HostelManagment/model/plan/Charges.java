package com.krunity.HostelManagment.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Charges {
    private SecurityDeposit securityDeposit;
    private CleaningCharges cleaningCharges;
    private MaintenanceCharges maintenanceCharges;
    private UtilityCharges utilityCharges;
}

