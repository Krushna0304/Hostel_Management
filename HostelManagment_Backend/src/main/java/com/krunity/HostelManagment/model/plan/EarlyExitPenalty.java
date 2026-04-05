package com.krunity.HostelManagment.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyExitPenalty {
    private String type; // MONTH_RENT, FIXED
    private Integer value; // number of months or fixed amount depending on type
}

