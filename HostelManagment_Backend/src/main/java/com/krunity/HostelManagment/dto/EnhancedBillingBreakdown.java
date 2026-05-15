package com.krunity.HostelManagment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedBillingBreakdown {
    private ActivationAmount activationAmount;
    private MonthlyAmount monthlyAmount;
    private InstallmentBreakdown installmentBreakdown;
    private RefundableAmounts refundableAmounts;
    private ExitSettlementEstimate exitSettlementEstimate;
    private String currency;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivationAmount {
        private BigDecimal firstInstallment;
        private BigDecimal refundableDeposits;
        private BigDecimal oneTimeCharges;
        private BigDecimal totalActivationAmount;
        private List<ChargeBreakdown> chargeBreakdowns;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyAmount {
        private BigDecimal baseRent;
        private BigDecimal recurringCharges;
        private BigDecimal totalMonthlyAmount;
        private List<ChargeBreakdown> recurringChargeBreakdowns;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallmentBreakdown {
        private BigDecimal installmentAmount;
        private InstallmentDistribution distribution;
        private List<InstallmentSummary> installmentSummaries;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundableAmounts {
        private BigDecimal totalRefundable;
        private List<ChargeBreakdown> refundableCharges;
        private Integer averageRefundProcessingDays;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExitSettlementEstimate {
        private BigDecimal estimatedRefund;
        private BigDecimal potentialDeductions;
        private BigDecimal netRefundEstimate;
        private List<ChargeBreakdown> deductionCharges;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargeBreakdown {
        private String chargeName;
        private BigDecimal amount;
        private String category;
        private String timing;
        private Boolean refundable;
        private String description;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallmentSummary {
        private Integer installmentNumber;
        private BigDecimal amount;
        private List<Integer> coveredMonths;
        private String description;
    }
}