package com.krunity.HostelManagment.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseRules {
    private Boolean smokingAllowed;
    private Boolean petsAllowed;
    private QuietHours quietHours;
}

