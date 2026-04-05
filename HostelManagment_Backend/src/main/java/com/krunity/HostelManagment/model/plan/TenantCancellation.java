package com.krunity.HostelManagment.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantCancellation {
    private Boolean allowed;
    private Integer noticePeriodDays;
    private EarlyExitPenalty earlyExitPenalty;
}

