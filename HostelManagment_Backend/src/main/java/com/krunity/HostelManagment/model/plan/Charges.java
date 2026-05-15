package com.krunity.HostelManagment.model.plan;

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
public class Charges {
    private SecurityDeposit securityDeposit;
    private CleaningCharges cleaningCharges;
    private MaintenanceCharges maintenanceCharges;
    private UtilityCharges utilityCharges;
    
    // Custom charges
    private CustomCharges customCharges;
    
    // Pre-calculated payment plan
    private PaymentPlan paymentPlan;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomCharges {
        private List<EnhancedCharge> oneTimeCharges;
        private List<EnhancedCharge> monthlyRecurringCharges;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentPlan {
        private AgreementActivationAmount agreementActivationAmount;
        private InstallmentDetails installmentDetails;
        private ExitSettlement exitSettlement;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AgreementActivationAmount {
            private BigDecimal firstInstallment;
            private BigDecimal refundableDeposits;
            private BigDecimal oneTimeCharges;
            private BigDecimal totalActivationAmount;
        }
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class InstallmentDetails {
            private BigDecimal installmentAmount;
            private Integer numberOfInstallments;
            private Integer monthsPerInstallment;
            private BigDecimal totalMonthlyAmount;
            private String paymentFrequency;
        }
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ExitSettlement {
            private BigDecimal estimatedRefund;
            private BigDecimal potentialDeductions;
            private BigDecimal netRefundEstimate;
        }
    }
}

