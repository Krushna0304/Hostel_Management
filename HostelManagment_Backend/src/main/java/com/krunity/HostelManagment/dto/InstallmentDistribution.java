package com.krunity.HostelManagment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentDistribution {
    private Integer totalMonths;
    private Integer numberOfInstallments;
    private List<InstallmentGroup> installmentGroups;
    private String distributionStrategy; // AUTO, CUSTOM, EQUAL
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallmentGroup {
        private Integer installmentNumber;
        private List<Integer> monthNumbers;
        private Integer monthCount;
        private String description; // e.g., "Months 1-4"
    }
}