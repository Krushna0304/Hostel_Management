package com.krunity.HostelManagment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ExistingTenantOnboardingResponse {
    private int onboardedCount;
    private List<Result> tenants;

    @Data
    @AllArgsConstructor
    public static class Result {
        private String name;
        private String username;
        /** Returned once to the owner; only its BCrypt hash is persisted. */
        private String temporaryPassword;
        private String roomNumber;
        private String agreementStatus;
        private String paymentPlanStatus;
    }
}
