package com.krunity.HostelManagment.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningCharges {
    private Boolean included;
    private String cleaningFrequency; // WEEKLY, MONTHLY, etc.
    private DeepCleaningOnExit deepCleaningOnExit;
}

