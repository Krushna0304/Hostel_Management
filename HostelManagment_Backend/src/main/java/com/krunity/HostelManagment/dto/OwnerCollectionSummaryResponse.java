package com.krunity.HostelManagment.dto;

import lombok.Data;

import java.util.List;

@Data
public class OwnerCollectionSummaryResponse {
    private Long totalCollected;
    private Long totalPending;
    private Long totalOverdue;
    private Integer activeTenants;
    private Integer overdueTenantsCount;
    private List<TenantCollectionRow> tenants;

    @Data
    public static class TenantCollectionRow {
        private String tenantId;
        private String tenantName;
        private String roomNumber;
        private String hostelName;
        private Long installmentAmount;
        private Integer pendingInstallments;
        private Integer overdueInstallments;
        private Long totalOverdueAmount;
        private String agreementId;
    }
}
