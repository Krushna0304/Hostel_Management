package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.EnhancedBillingBreakdown;
import com.krunity.HostelManagment.dto.InstallmentDistribution;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import com.krunity.HostelManagment.model.plan.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced billing calculation service with comprehensive breakdown
 */
@Service
public class EnhancedBillingCalculationService {

    @Autowired
    private InstallmentDistributionService installmentDistributionService;

    public EnhancedBillingBreakdown calculateComprehensiveBilling(RoomAgreementPlan plan) {
        if (plan == null) {
            return createEmptyBreakdown();
        }

        // Calculate activation amount
        EnhancedBillingBreakdown.ActivationAmount activationAmount = calculateActivationAmount(plan);
        
        // Calculate monthly amount
        EnhancedBillingBreakdown.MonthlyAmount monthlyAmount = calculateMonthlyAmount(plan);
        
        // Calculate installment breakdown
        EnhancedBillingBreakdown.InstallmentBreakdown installmentBreakdown = calculateInstallmentBreakdown(plan, monthlyAmount);
        
        // Calculate refundable amounts
        EnhancedBillingBreakdown.RefundableAmounts refundableAmounts = calculateRefundableAmounts(plan);
        
        // Calculate exit settlement estimate
        EnhancedBillingBreakdown.ExitSettlementEstimate exitSettlement = calculateExitSettlementEstimate(plan);

        return EnhancedBillingBreakdown.builder()
                .activationAmount(activationAmount)
                .monthlyAmount(monthlyAmount)
                .installmentBreakdown(installmentBreakdown)
                .refundableAmounts(refundableAmounts)
                .exitSettlementEstimate(exitSettlement)
                .currency(plan.getRentDetails() != null ? plan.getRentDetails().getCurrency() : "INR")
                .build();
    }

    private EnhancedBillingBreakdown.ActivationAmount calculateActivationAmount(RoomAgreementPlan plan) {
        List<EnhancedBillingBreakdown.ChargeBreakdown> chargeBreakdowns = new ArrayList<>();
        BigDecimal firstInstallment = BigDecimal.ZERO;
        BigDecimal refundableDeposits = BigDecimal.ZERO;
        BigDecimal oneTimeCharges = BigDecimal.ZERO;

        // Base rent for first installment
        if (plan.getRentDetails() != null && plan.getRentDetails().getMonthlyRent() != null) {
            firstInstallment = plan.getRentDetails().getMonthlyRent();
            chargeBreakdowns.add(createChargeBreakdown("Monthly Rent", firstInstallment, "RENT", "IN_INSTALLMENTS", false, "Base monthly rent"));
        }

        // Process charges
        if (plan.getCharges() != null) {
            // Security deposit
            if (plan.getCharges().getSecurityDeposit() != null && plan.getCharges().getSecurityDeposit().getAmount() != null) {
                BigDecimal depositAmount = plan.getCharges().getSecurityDeposit().getAmount();
                boolean isRefundable = plan.getCharges().getSecurityDeposit().getRefundable() != null ? 
                    plan.getCharges().getSecurityDeposit().getRefundable() : true;
                
                if (isRefundable) {
                    refundableDeposits = refundableDeposits.add(depositAmount);
                } else {
                    oneTimeCharges = oneTimeCharges.add(depositAmount);
                }
                
                chargeBreakdowns.add(createChargeBreakdown("Security Deposit", depositAmount, "REFUNDABLE_DEPOSIT", "AT_AGREEMENT", isRefundable, "Security deposit"));
            }

            // One-time maintenance charges
            if (plan.getCharges().getMaintenanceCharges() != null && 
                plan.getCharges().getMaintenanceCharges().getOneTimeMaintenanceCharge() != null &&
                plan.getCharges().getMaintenanceCharges().getOneTimeMaintenanceCharge().getApplicable() != null &&
                plan.getCharges().getMaintenanceCharges().getOneTimeMaintenanceCharge().getApplicable() &&
                plan.getCharges().getMaintenanceCharges().getOneTimeMaintenanceCharge().getAmount() != null) {
                
                BigDecimal maintenanceAmount = plan.getCharges().getMaintenanceCharges().getOneTimeMaintenanceCharge().getAmount();
                boolean isRefundable = plan.getCharges().getMaintenanceCharges().getOneTimeMaintenanceCharge().getRefundable() != null ?
                    plan.getCharges().getMaintenanceCharges().getOneTimeMaintenanceCharge().getRefundable() : false;
                
                if (isRefundable) {
                    refundableDeposits = refundableDeposits.add(maintenanceAmount);
                } else {
                    oneTimeCharges = oneTimeCharges.add(maintenanceAmount);
                }
                
                chargeBreakdowns.add(createChargeBreakdown("One-time Maintenance", maintenanceAmount, "ONE_TIME_CHARGE", "AT_AGREEMENT", isRefundable, "One-time maintenance charge"));
            }
        }

        // Process custom one-time charges
        if (plan.getCharges() != null && plan.getCharges().getCustomCharges() != null && 
            plan.getCharges().getCustomCharges().getOneTimeCharges() != null) {
            for (EnhancedCharge customCharge : plan.getCharges().getCustomCharges().getOneTimeCharges()) {
                if (customCharge.getApplicable() != null && customCharge.getApplicable() && customCharge.getAmount() != null) {
                    boolean isRefundable = customCharge.getRefundable() != null ? customCharge.getRefundable() : false;
                    
                    if (isRefundable) {
                        refundableDeposits = refundableDeposits.add(customCharge.getAmount());
                    } else {
                        oneTimeCharges = oneTimeCharges.add(customCharge.getAmount());
                    }
                    
                    chargeBreakdowns.add(createChargeBreakdown(
                        customCharge.getChargeName(), 
                        customCharge.getAmount(), 
                        "CUSTOM_ONE_TIME_CHARGE", 
                        "AT_AGREEMENT", 
                        isRefundable, 
                        customCharge.getDescription() != null ? customCharge.getDescription() : "Custom one-time charge"
                    ));
                }
            }
        }

        // Add recurring charges to first installment
        BigDecimal recurringCharges = calculateRecurringChargesAmount(plan);
        firstInstallment = firstInstallment.add(recurringCharges);

        BigDecimal totalActivationAmount = firstInstallment.add(refundableDeposits).add(oneTimeCharges);

        return EnhancedBillingBreakdown.ActivationAmount.builder()
                .firstInstallment(firstInstallment)
                .refundableDeposits(refundableDeposits)
                .oneTimeCharges(oneTimeCharges)
                .totalActivationAmount(totalActivationAmount)
                .chargeBreakdowns(chargeBreakdowns)
                .build();
    }

    private EnhancedBillingBreakdown.MonthlyAmount calculateMonthlyAmount(RoomAgreementPlan plan) {
        List<EnhancedBillingBreakdown.ChargeBreakdown> recurringChargeBreakdowns = new ArrayList<>();
        BigDecimal baseRent = BigDecimal.ZERO;
        BigDecimal recurringCharges = BigDecimal.ZERO;

        // Base rent
        if (plan.getRentDetails() != null && plan.getRentDetails().getMonthlyRent() != null) {
            baseRent = plan.getRentDetails().getMonthlyRent();
        }

        // Calculate recurring charges
        recurringCharges = calculateRecurringChargesAmount(plan);
        
        // Add recurring charge breakdowns
        if (plan.getCharges() != null) {
            addRecurringChargeBreakdowns(plan.getCharges(), recurringChargeBreakdowns);
        }

        // Add custom monthly recurring charges to breakdowns
        if (plan.getCharges() != null && plan.getCharges().getCustomCharges() != null && 
            plan.getCharges().getCustomCharges().getMonthlyRecurringCharges() != null) {
            for (EnhancedCharge customCharge : plan.getCharges().getCustomCharges().getMonthlyRecurringCharges()) {
                if (customCharge.getApplicable() != null && customCharge.getApplicable() && customCharge.getAmount() != null) {
                    recurringChargeBreakdowns.add(createChargeBreakdown(
                        customCharge.getChargeName(),
                        customCharge.getAmount(),
                        "CUSTOM_RECURRING_CHARGE",
                        "IN_INSTALLMENTS",
                        false,
                        customCharge.getDescription() != null ? customCharge.getDescription() : "Custom monthly recurring charge"
                    ));
                }
            }
        }

        BigDecimal totalMonthlyAmount = baseRent.add(recurringCharges);

        return EnhancedBillingBreakdown.MonthlyAmount.builder()
                .baseRent(baseRent)
                .recurringCharges(recurringCharges)
                .totalMonthlyAmount(totalMonthlyAmount)
                .recurringChargeBreakdowns(recurringChargeBreakdowns)
                .build();
    }

    private EnhancedBillingBreakdown.InstallmentBreakdown calculateInstallmentBreakdown(RoomAgreementPlan plan, EnhancedBillingBreakdown.MonthlyAmount monthlyAmount) {
        BigDecimal installmentAmount = monthlyAmount.getTotalMonthlyAmount();
        
        // Get installment distribution
        int totalMonths = 12; // Default
        int numberOfInstallments = 12; // Default
        
        if (plan.getDuration() != null && plan.getDuration().getValue() != null) {
            totalMonths = plan.getDuration().getValue();
        }
        
        if (plan.getPaymentModel() != null && plan.getPaymentModel().getInstallments() != null) {
            numberOfInstallments = plan.getPaymentModel().getInstallments();
        }
        
        InstallmentDistribution distribution = installmentDistributionService.calculateDistribution(totalMonths, numberOfInstallments);
        
        // Create installment summaries
        List<EnhancedBillingBreakdown.InstallmentSummary> installmentSummaries = new ArrayList<>();
        for (InstallmentDistribution.InstallmentGroup group : distribution.getInstallmentGroups()) {
            BigDecimal groupAmount = installmentAmount.multiply(BigDecimal.valueOf(group.getMonthCount()));
            
            EnhancedBillingBreakdown.InstallmentSummary summary = EnhancedBillingBreakdown.InstallmentSummary.builder()
                    .installmentNumber(group.getInstallmentNumber())
                    .amount(groupAmount)
                    .coveredMonths(group.getMonthNumbers())
                    .description(group.getDescription())
                    .build();
            
            installmentSummaries.add(summary);
        }

        return EnhancedBillingBreakdown.InstallmentBreakdown.builder()
                .installmentAmount(installmentAmount)
                .distribution(distribution)
                .installmentSummaries(installmentSummaries)
                .build();
    }

    private EnhancedBillingBreakdown.RefundableAmounts calculateRefundableAmounts(RoomAgreementPlan plan) {
        List<EnhancedBillingBreakdown.ChargeBreakdown> refundableCharges = new ArrayList<>();
        BigDecimal totalRefundable = BigDecimal.ZERO;
        int totalProcessingDays = 0;
        int refundableChargeCount = 0;

        if (plan.getCharges() != null) {
            // Security deposit
            if (plan.getCharges().getSecurityDeposit() != null && 
                plan.getCharges().getSecurityDeposit().getAmount() != null &&
                plan.getCharges().getSecurityDeposit().getRefundable() != null &&
                plan.getCharges().getSecurityDeposit().getRefundable()) {
                
                BigDecimal amount = plan.getCharges().getSecurityDeposit().getAmount();
                totalRefundable = totalRefundable.add(amount);
                
                if (plan.getCharges().getSecurityDeposit().getRefundProcessingDays() != null) {
                    totalProcessingDays += plan.getCharges().getSecurityDeposit().getRefundProcessingDays();
                    refundableChargeCount++;
                }
                
                refundableCharges.add(createChargeBreakdown("Security Deposit", amount, "REFUNDABLE_DEPOSIT", "AT_AGREEMENT", true, "Refundable security deposit"));
            }

            // Check custom one-time charges for refundable amounts
            if (plan.getCharges() != null && plan.getCharges().getCustomCharges() != null && 
                plan.getCharges().getCustomCharges().getOneTimeCharges() != null) {
                for (EnhancedCharge customCharge : plan.getCharges().getCustomCharges().getOneTimeCharges()) {
                    if (customCharge.getApplicable() != null && customCharge.getApplicable() && 
                        customCharge.getAmount() != null && customCharge.getRefundable() != null && customCharge.getRefundable()) {
                        
                        totalRefundable = totalRefundable.add(customCharge.getAmount());
                        
                        if (customCharge.getRefundProcessingDays() != null) {
                            totalProcessingDays += customCharge.getRefundProcessingDays();
                            refundableChargeCount++;
                        }
                        
                        refundableCharges.add(createChargeBreakdown(
                            customCharge.getChargeName(), 
                            customCharge.getAmount(), 
                            "CUSTOM_REFUNDABLE_CHARGE", 
                            "AT_AGREEMENT", 
                            true, 
                            customCharge.getDescription() != null ? customCharge.getDescription() : "Custom refundable charge"
                        ));
                    }
                }
            }
        }

        int averageProcessingDays = refundableChargeCount > 0 ? totalProcessingDays / refundableChargeCount : 30;

        return EnhancedBillingBreakdown.RefundableAmounts.builder()
                .totalRefundable(totalRefundable)
                .refundableCharges(refundableCharges)
                .averageRefundProcessingDays(averageProcessingDays)
                .build();
    }

    private EnhancedBillingBreakdown.ExitSettlementEstimate calculateExitSettlementEstimate(RoomAgreementPlan plan) {
        List<EnhancedBillingBreakdown.ChargeBreakdown> deductionCharges = new ArrayList<>();
        BigDecimal estimatedRefund = BigDecimal.ZERO;
        BigDecimal potentialDeductions = BigDecimal.ZERO;

        // Calculate total refundable amount
        if (plan.getCharges() != null && plan.getCharges().getSecurityDeposit() != null && 
            plan.getCharges().getSecurityDeposit().getAmount() != null &&
            plan.getCharges().getSecurityDeposit().getRefundable() != null &&
            plan.getCharges().getSecurityDeposit().getRefundable()) {
            estimatedRefund = plan.getCharges().getSecurityDeposit().getAmount();
        }

        // Calculate potential deductions
        if (plan.getCharges() != null && plan.getCharges().getCleaningCharges() != null &&
            plan.getCharges().getCleaningCharges().getDeepCleaningOnExit() != null &&
            plan.getCharges().getCleaningCharges().getDeepCleaningOnExit().getApplicable() != null &&
            plan.getCharges().getCleaningCharges().getDeepCleaningOnExit().getApplicable() &&
            plan.getCharges().getCleaningCharges().getDeepCleaningOnExit().getAmount() != null) {
            
            BigDecimal cleaningAmount = plan.getCharges().getCleaningCharges().getDeepCleaningOnExit().getAmount();
            boolean isRefundable = plan.getCharges().getCleaningCharges().getDeepCleaningOnExit().getRefundable() != null ?
                plan.getCharges().getCleaningCharges().getDeepCleaningOnExit().getRefundable() : false;
            
            if (isRefundable) {
                potentialDeductions = potentialDeductions.add(cleaningAmount);
                deductionCharges.add(createChargeBreakdown("Deep Cleaning", cleaningAmount, "DEDUCTION_CHARGE", "AT_END", true, "Deep cleaning on exit"));
            }
        }

        BigDecimal netRefundEstimate = estimatedRefund.subtract(potentialDeductions);

        return EnhancedBillingBreakdown.ExitSettlementEstimate.builder()
                .estimatedRefund(estimatedRefund)
                .potentialDeductions(potentialDeductions)
                .netRefundEstimate(netRefundEstimate)
                .deductionCharges(deductionCharges)
                .build();
    }

    private BigDecimal calculateRecurringChargesAmount(RoomAgreementPlan plan) {
        BigDecimal total = BigDecimal.ZERO;

        if (plan.getCharges() != null) {
            // Monthly cleaning charges
            if (plan.getCharges().getCleaningCharges() != null &&
                plan.getCharges().getCleaningCharges().getMonthlyCleaningCharge() != null &&
                plan.getCharges().getCleaningCharges().getMonthlyCleaningCharge().getApplicable() != null &&
                plan.getCharges().getCleaningCharges().getMonthlyCleaningCharge().getApplicable() &&
                plan.getCharges().getCleaningCharges().getMonthlyCleaningCharge().getAmount() != null) {
                total = total.add(plan.getCharges().getCleaningCharges().getMonthlyCleaningCharge().getAmount());
            }

            // Monthly maintenance charges
            if (plan.getCharges().getMaintenanceCharges() != null &&
                plan.getCharges().getMaintenanceCharges().getMonthlyMaintenanceCharge() != null &&
                plan.getCharges().getMaintenanceCharges().getMonthlyMaintenanceCharge().getApplicable() != null &&
                plan.getCharges().getMaintenanceCharges().getMonthlyMaintenanceCharge().getApplicable() &&
                plan.getCharges().getMaintenanceCharges().getMonthlyMaintenanceCharge().getAmount() != null) {
                total = total.add(plan.getCharges().getMaintenanceCharges().getMonthlyMaintenanceCharge().getAmount());
            }

            // Utility charges
            if (plan.getCharges().getUtilityCharges() != null) {
                if (plan.getCharges().getUtilityCharges().getElectricity() != null &&
                    plan.getCharges().getUtilityCharges().getElectricity().getFixedAmount() != null) {
                    total = total.add(plan.getCharges().getUtilityCharges().getElectricity().getFixedAmount());
                }

                if (plan.getCharges().getUtilityCharges().getWater() != null &&
                    plan.getCharges().getUtilityCharges().getWater().getMonthlyAmount() != null) {
                    total = total.add(plan.getCharges().getUtilityCharges().getWater().getMonthlyAmount());
                }
            }
        }

        // Add custom monthly recurring charges
        if (plan.getCharges() != null && plan.getCharges().getCustomCharges() != null && 
            plan.getCharges().getCustomCharges().getMonthlyRecurringCharges() != null) {
            for (EnhancedCharge customCharge : plan.getCharges().getCustomCharges().getMonthlyRecurringCharges()) {
                if (customCharge.getApplicable() != null && customCharge.getApplicable() && customCharge.getAmount() != null) {
                    total = total.add(customCharge.getAmount());
                }
            }
        }

        return total;
    }

    private void addRecurringChargeBreakdowns(Charges charges, List<EnhancedBillingBreakdown.ChargeBreakdown> breakdowns) {
        // Monthly cleaning charges
        if (charges.getCleaningCharges() != null &&
            charges.getCleaningCharges().getMonthlyCleaningCharge() != null &&
            charges.getCleaningCharges().getMonthlyCleaningCharge().getApplicable() != null &&
            charges.getCleaningCharges().getMonthlyCleaningCharge().getApplicable() &&
            charges.getCleaningCharges().getMonthlyCleaningCharge().getAmount() != null) {
            
            breakdowns.add(createChargeBreakdown("Monthly Cleaning", 
                charges.getCleaningCharges().getMonthlyCleaningCharge().getAmount(),
                "RECURRING_CHARGE", "IN_INSTALLMENTS", false, "Monthly cleaning service"));
        }

        // Monthly maintenance charges
        if (charges.getMaintenanceCharges() != null &&
            charges.getMaintenanceCharges().getMonthlyMaintenanceCharge() != null &&
            charges.getMaintenanceCharges().getMonthlyMaintenanceCharge().getApplicable() != null &&
            charges.getMaintenanceCharges().getMonthlyMaintenanceCharge().getApplicable() &&
            charges.getMaintenanceCharges().getMonthlyMaintenanceCharge().getAmount() != null) {
            
            breakdowns.add(createChargeBreakdown("Monthly Maintenance", 
                charges.getMaintenanceCharges().getMonthlyMaintenanceCharge().getAmount(),
                "RECURRING_CHARGE", "IN_INSTALLMENTS", false, "Monthly maintenance service"));
        }

        // Utility charges
        if (charges.getUtilityCharges() != null) {
            if (charges.getUtilityCharges().getElectricity() != null &&
                charges.getUtilityCharges().getElectricity().getFixedAmount() != null) {
                
                breakdowns.add(createChargeBreakdown("Electricity", 
                    charges.getUtilityCharges().getElectricity().getFixedAmount(),
                    "RECURRING_CHARGE", "IN_INSTALLMENTS", false, "Fixed electricity charges"));
            }

            if (charges.getUtilityCharges().getWater() != null &&
                charges.getUtilityCharges().getWater().getMonthlyAmount() != null) {
                
                breakdowns.add(createChargeBreakdown("Water", 
                    charges.getUtilityCharges().getWater().getMonthlyAmount(),
                    "RECURRING_CHARGE", "IN_INSTALLMENTS", false, "Water charges"));
            }
        }
    }

    private EnhancedBillingBreakdown.ChargeBreakdown createChargeBreakdown(String name, BigDecimal amount, String category, String timing, Boolean refundable, String description) {
        return EnhancedBillingBreakdown.ChargeBreakdown.builder()
                .chargeName(name)
                .amount(amount)
                .category(category)
                .timing(timing)
                .refundable(refundable)
                .description(description)
                .build();
    }

    private EnhancedBillingBreakdown createEmptyBreakdown() {
        return EnhancedBillingBreakdown.builder()
                .activationAmount(EnhancedBillingBreakdown.ActivationAmount.builder()
                    .firstInstallment(BigDecimal.ZERO)
                    .refundableDeposits(BigDecimal.ZERO)
                    .oneTimeCharges(BigDecimal.ZERO)
                    .totalActivationAmount(BigDecimal.ZERO)
                    .chargeBreakdowns(new ArrayList<>())
                    .build())
                .monthlyAmount(EnhancedBillingBreakdown.MonthlyAmount.builder()
                    .baseRent(BigDecimal.ZERO)
                    .recurringCharges(BigDecimal.ZERO)
                    .totalMonthlyAmount(BigDecimal.ZERO)
                    .recurringChargeBreakdowns(new ArrayList<>())
                    .build())
                .currency("INR")
                .build();
    }
}