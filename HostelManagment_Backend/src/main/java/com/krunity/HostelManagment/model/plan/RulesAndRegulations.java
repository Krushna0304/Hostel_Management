package com.krunity.HostelManagment.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RulesAndRegulations {
    private HouseRules houseRules;
    @Builder.Default
    private List<String> facilityUsageRules = new ArrayList<>();
}

