package com.krunity.HostelManagment.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Legal {
    private Boolean agreementLock;
    private Boolean modificationAllowedAfterSign;
    private String jurisdiction;
}

