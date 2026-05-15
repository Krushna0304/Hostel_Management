package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.model.RoomAgreementPlan;
import com.krunity.HostelManagment.model.plan.*;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service to calculate different payment amounts based on the plan structure
 */
@Service
public class PaymentCalculationService {

    @Data
    public static class PaymentBreakdown {
        // Amounts collected at agreement acceptance
        private BigDecimal agreementTimeRefundable = BigDecimal.ZERO;     // Security deposit
        private BigDecimal agreementTimeNonRefundable = BigDecimal.ZERO;  // One-time charges
        
        // Amount included in each installment
        private BigDecimal installmentAmount = BigDecimal.ZERO;           // Base rent + monthly charges
        
        // Amounts collected at agreement end
        private BigDecimal endTimeRefundable = BigDecimal.ZERO;           // Deducted from deposit
        private BigDecimal endTimeNonRefundable = BigDecimal.ZERO;        // Additional charges
        
        // Total amounts for reference
        private BigDecimal totalAgreementTime = BigDecimal.ZERO;
        private BigDecimal totalEndTime = BigDecimal.ZERO;
    }

    /**
     * Calculate payment breakdown based on the plan
     */
    public PaymentBreakdown calculatePaymentBreakdown(RoomAgreementPlan plan) {
        PaymentBreakdown breakdown = new PaymentBreakdown();
        
        if (plan == null) {
            return breakdown;
        }

        // Base rent (always in installments)
        if (plan.getRentDetails() != null && plan.getRentDetails().getMonthlyRent() != null) {
            breakdown.setInstallmentAmount(breakdown.getInstallmentAmount().add(plan.getRentDetails().getMonthlyRent()));
        }

        // Process charges
        if (plan.getCharges() != null) {
            processSecurityDeposit(plan.getCharges().getSecurityDeposit(), breakdown);
            processCleaningCharges(plan.getCharges().getCleaningCharges(), breakdown);
            processMaintenanceCharges(plan.getCharges().getMaintenanceCharges(), breakdown);
            processUtilityCharges(plan.getCharges().getUtilityCharges(), breakdown);
        }

        // Process custom charges
        if (plan.getCharges() != null && plan.getCharges().getCustomCharges() != null) {
            processCustomOneTimeCharges(plan.getCharges().getCustomCharges().getOneTimeCharges(), breakdown);
            processCustomRecurringCharges(plan.getCharges().getCustomCharges().getMonthlyRecurringCharges(), breakdown);
        }

        // Calculate totals
        breakdown.setTotalAgreementTime(
            breakdown.getAgreementTimeRefundable().add(breakdown.getAgreementTimeNonRefundable())
        );
        breakdown.setTotalEndTime(
            breakdown.getEndTimeRefundable().add(breakdown.getEndTimeNonRefundable())
        );

        return breakdown;
    }

    private void processSecurityDeposit(SecurityDeposit securityDeposit, PaymentBreakdown breakdown) {
        if (securityDeposit == null || securityDeposit.getAmount() == null) {
            return;
        }

        PaymentTiming timing = securityDeposit.getPaymentTiming() != null ? 
            securityDeposit.getPaymentTiming() : PaymentTiming.AT_AGREEMENT;

        boolean refundable = securityDeposit.getRefundable() != null ? 
            securityDeposit.getRefundable() : true;

        switch (timing) {
            case AT_AGREEMENT:
                if (refundable) {
                    breakdown.setAgreementTimeRefundable(
                        breakdown.getAgreementTimeRefundable().add(securityDeposit.getAmount())
                    );
                } else {
                    breakdown.setAgreementTimeNonRefundable(
                        breakdown.getAgreementTimeNonRefundable().add(securityDeposit.getAmount())
                    );
                }
                break;
            case IN_INSTALLMENTS:
                breakdown.setInstallmentAmount(
                    breakdown.getInstallmentAmount().add(securityDeposit.getAmount())
                );
                break;
            case AT_END:
                if (refundable) {
                    breakdown.setEndTimeRefundable(
                        breakdown.getEndTimeRefundable().add(securityDeposit.getAmount())
                    );
                } else {
                    breakdown.setEndTimeNonRefundable(
                        breakdown.getEndTimeNonRefundable().add(securityDeposit.getAmount())
                    );
                }
                break;
        }
    }

    private void processCleaningCharges(CleaningCharges cleaningCharges, PaymentBreakdown breakdown) {
        if (cleaningCharges == null) {
            return;
        }

        // Process monthly cleaning charges (in installments)
        if (cleaningCharges.getMonthlyCleaningCharge() != null && 
            cleaningCharges.getMonthlyCleaningCharge().getApplicable() != null &&
            cleaningCharges.getMonthlyCleaningCharge().getApplicable() &&
            cleaningCharges.getMonthlyCleaningCharge().getAmount() != null) {
            
            breakdown.setInstallmentAmount(
                breakdown.getInstallmentAmount().add(cleaningCharges.getMonthlyCleaningCharge().getAmount())
            );
        }

        // Process deep cleaning on exit
        if (cleaningCharges.getDeepCleaningOnExit() != null &&
            cleaningCharges.getDeepCleaningOnExit().getApplicable() != null &&
            cleaningCharges.getDeepCleaningOnExit().getApplicable() &&
            cleaningCharges.getDeepCleaningOnExit().getAmount() != null) {
            
            PaymentTiming timing = cleaningCharges.getDeepCleaningOnExit().getPaymentTiming() != null ?
                cleaningCharges.getDeepCleaningOnExit().getPaymentTiming() : PaymentTiming.AT_END;
            
            boolean refundable = cleaningCharges.getDeepCleaningOnExit().getRefundable() != null ?
                cleaningCharges.getDeepCleaningOnExit().getRefundable() : false;

            switch (timing) {
                case AT_AGREEMENT:
                    if (refundable) {
                        breakdown.setAgreementTimeRefundable(
                            breakdown.getAgreementTimeRefundable().add(cleaningCharges.getDeepCleaningOnExit().getAmount())
                        );
                    } else {
                        breakdown.setAgreementTimeNonRefundable(
                            breakdown.getAgreementTimeNonRefundable().add(cleaningCharges.getDeepCleaningOnExit().getAmount())
                        );
                    }
                    break;
                case IN_INSTALLMENTS:
                    breakdown.setInstallmentAmount(
                        breakdown.getInstallmentAmount().add(cleaningCharges.getDeepCleaningOnExit().getAmount())
                    );
                    break;
                case AT_END:
                    if (refundable) {
                        breakdown.setEndTimeRefundable(
                            breakdown.getEndTimeRefundable().add(cleaningCharges.getDeepCleaningOnExit().getAmount())
                        );
                    } else {
                        breakdown.setEndTimeNonRefundable(
                            breakdown.getEndTimeNonRefundable().add(cleaningCharges.getDeepCleaningOnExit().getAmount())
                        );
                    }
                    break;
            }
        }
    }

    private void processMaintenanceCharges(MaintenanceCharges maintenanceCharges, PaymentBreakdown breakdown) {
        if (maintenanceCharges == null) {
            return;
        }

        // Process one-time maintenance charges
        if (maintenanceCharges.getOneTimeMaintenanceCharge() != null &&
            maintenanceCharges.getOneTimeMaintenanceCharge().getApplicable() != null &&
            maintenanceCharges.getOneTimeMaintenanceCharge().getApplicable() &&
            maintenanceCharges.getOneTimeMaintenanceCharge().getAmount() != null) {
            
            boolean refundable = maintenanceCharges.getOneTimeMaintenanceCharge().getRefundable() != null ?
                maintenanceCharges.getOneTimeMaintenanceCharge().getRefundable() : false;

            if (refundable) {
                breakdown.setAgreementTimeRefundable(
                    breakdown.getAgreementTimeRefundable().add(maintenanceCharges.getOneTimeMaintenanceCharge().getAmount())
                );
            } else {
                breakdown.setAgreementTimeNonRefundable(
                    breakdown.getAgreementTimeNonRefundable().add(maintenanceCharges.getOneTimeMaintenanceCharge().getAmount())
                );
            }
        }

        // Process monthly maintenance charges
        if (maintenanceCharges.getMonthlyMaintenanceCharge() != null &&
            maintenanceCharges.getMonthlyMaintenanceCharge().getApplicable() != null &&
            maintenanceCharges.getMonthlyMaintenanceCharge().getApplicable() &&
            maintenanceCharges.getMonthlyMaintenanceCharge().getAmount() != null) {
            
            breakdown.setInstallmentAmount(
                breakdown.getInstallmentAmount().add(maintenanceCharges.getMonthlyMaintenanceCharge().getAmount())
            );
        }
    }

    private void processUtilityCharges(UtilityCharges utilityCharges, PaymentBreakdown breakdown) {
        if (utilityCharges == null) {
            return;
        }

        // Process electricity charges
        if (utilityCharges.getElectricity() != null &&
            utilityCharges.getElectricity().getFixedAmount() != null) {
            
            breakdown.setInstallmentAmount(
                breakdown.getInstallmentAmount().add(utilityCharges.getElectricity().getFixedAmount())
            );
        }

        // Process water charges
        if (utilityCharges.getWater() != null &&
            utilityCharges.getWater().getMonthlyAmount() != null) {
            
            breakdown.setInstallmentAmount(
                breakdown.getInstallmentAmount().add(utilityCharges.getWater().getMonthlyAmount())
            );
        }
    }

    private void processCustomOneTimeCharges(java.util.List<EnhancedCharge> oneTimeCharges, PaymentBreakdown breakdown) {
        if (oneTimeCharges == null) {
            return;
        }

        for (EnhancedCharge charge : oneTimeCharges) {
            if (charge.getApplicable() != null && charge.getApplicable() && charge.getAmount() != null) {
                PaymentTiming timing = charge.getTiming() != null ? charge.getTiming() : PaymentTiming.AT_AGREEMENT;
                boolean refundable = charge.getRefundable() != null ? charge.getRefundable() : false;

                switch (timing) {
                    case AT_AGREEMENT:
                        if (refundable) {
                            breakdown.setAgreementTimeRefundable(
                                breakdown.getAgreementTimeRefundable().add(charge.getAmount())
                            );
                        } else {
                            breakdown.setAgreementTimeNonRefundable(
                                breakdown.getAgreementTimeNonRefundable().add(charge.getAmount())
                            );
                        }
                        break;
                    case IN_INSTALLMENTS:
                        breakdown.setInstallmentAmount(
                            breakdown.getInstallmentAmount().add(charge.getAmount())
                        );
                        break;
                    case AT_END:
                        if (refundable) {
                            breakdown.setEndTimeRefundable(
                                breakdown.getEndTimeRefundable().add(charge.getAmount())
                            );
                        } else {
                            breakdown.setEndTimeNonRefundable(
                                breakdown.getEndTimeNonRefundable().add(charge.getAmount())
                            );
                        }
                        break;
                }
            }
        }
    }

    private void processCustomRecurringCharges(java.util.List<EnhancedCharge> recurringCharges, PaymentBreakdown breakdown) {
        if (recurringCharges == null) {
            return;
        }

        for (EnhancedCharge charge : recurringCharges) {
            if (charge.getApplicable() != null && charge.getApplicable() && charge.getAmount() != null) {
                // Custom recurring charges are always added to installment amount
                breakdown.setInstallmentAmount(
                    breakdown.getInstallmentAmount().add(charge.getAmount())
                );
            }
        }
    }
}