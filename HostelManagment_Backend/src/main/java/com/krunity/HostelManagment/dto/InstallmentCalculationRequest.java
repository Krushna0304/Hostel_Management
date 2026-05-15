package com.krunity.HostelManagment.dto;

import lombok.Data;

@Data
public class InstallmentCalculationRequest {
    private Integer totalMonths;
    private Integer numberOfInstallments;
    private String distributionStrategy; // AUTO, EQUAL, CUSTOM
}