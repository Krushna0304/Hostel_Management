package com.krunity.HostelManagment.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Duration {
    private String unit; // MONTH, YEAR
    private Integer value;
    private Integer minimumStayMonths;
}

