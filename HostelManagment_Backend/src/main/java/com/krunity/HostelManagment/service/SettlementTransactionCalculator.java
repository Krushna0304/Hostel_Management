package com.krunity.HostelManagment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krunity.HostelManagment.dto.SettlementCalculationResult;
import com.krunity.HostelManagment.dto.SettlementTransaction;
import com.krunity.HostelManagment.exception.SettlementCalculationException;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import com.krunity.HostelManagment.model.RoomAllotment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Service component for calculating settlement transactions
 * Handles complex settlement calculations based on plan snapshots and payment schedules
 */
@Slf4j
@Component
public class SettlementTransactionCalculator {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgreementService agreementService;

    @Autowired
    private PaymentScheduleService paymentScheduleService;

    // ─── Main Settlement Calculation Methods ──────────────────────────────────

    /**
     * Calculates settlement based on agreement, allotment, and calculation date
     */
    public SettlementCalculationResult calculateSettlement(
            Agreement agreement,
            RoomAllotment allotment,
            LocalDate calculationDate) {
        
        log.info("Calculating settlement for agreement {} on date {}", agreement.getId(), calculationDate);
        
        try {
            // Validate inputs
            validateCalculationInputs(agreement, allotment, calculationDate);
            
            // Get plan snapshot from agreement
            var planSnapshot = agreement.getPlanSnapshot();
            if (planSnapshot == null) {
                throw new SettlementCalculationException("Agreement plan snapshot is null or invalid");
            }
            
            // Calculate various components
            SettlementCalculationResult result = SettlementCalculationResult.builder()
                    .agreementId(agreement.getId())
                    .calculationDate(calculationDate)
                    .allotmentId(allotment.getAllotmentId())
                    .build();
            
            // Calculate security deposit component
            calculateSecurityDepositComponent(result, planSnapshot);
            
            // Calculate outstanding rent
            calculateOutstandingRent(result, agreement, allotment, calculationDate);
            
            // Calculate outstanding other charges
            calculateOutstandingCharges(result, agreement, calculationDate);
            
            // Apply early exit penalties if applicable
            if (allotment.isEarlyExit()) {
                calculateEarlyExitPenalties(result, agreement, allotment, calculationDate);
            }
            
            // Calculate deposit adjustments
            calculateDepositAdjustments(result, planSnapshot);
            
            // Calculate final settlement amount
            calculateFinalSettlementAmount(result);
            
            log.info("Settlement calculation completed for agreement {}. Final amount: {} ({})", 
                    agreement.getId(), result.getFinalSettlementAmount(), result.getSettlementType());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error calculating settlement for agreement {}: {}", agreement.getId(), e.getMessage(), e);
            throw new SettlementCalculationException("Settlement calculation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parses settlement transaction data from JSON string
     */
    public SettlementTransaction parseSettlementData(String transactionData) {
        if (transactionData == null || transactionData.trim().isEmpty()) {
            throw new SettlementCalculationException("Settlement transaction data is null or empty");
        }
        
        try {
            log.debug("Parsing settlement transaction data: {}", transactionData);
            return objectMapper.readValue(transactionData, SettlementTransaction.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse settlement transaction data: {}", e.getMessage());
            throw new SettlementCalculationException("Invalid settlement transaction data format", e);
        }
    }

    /**
     * Formats SettlementTransaction object back to JSON string
     */
    public String formatSettlementTransaction(SettlementTransaction transaction) {
        if (transaction == null) {
            throw new SettlementCalculationException("Settlement transaction is null");
        }
        
        try {
            String json = objectMapper.writeValueAsString(transaction);
            log.debug("Formatted settlement transaction to JSON: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            log.error("Failed to format settlement transaction: {}", e.getMessage());
            throw new SettlementCalculationException("Failed to format settlement transaction", e);
        }
    }

    /**
     * Validates round-trip consistency (parse -> format -> parse)
     */
    public boolean validateRoundTripConsistency(SettlementTransaction originalTransaction) {
        try {
            String formatted = formatSettlementTransaction(originalTransaction);
            SettlementTransaction parsed = parseSettlementData(formatted);
            
            boolean isConsistent = originalTransaction.equals(parsed);
            log.debug("Round-trip validation for settlement transaction: {}", isConsistent ? "PASSED" : "FAILED");
            
            return isConsistent;
        } catch (Exception e) {
            log.warn("Round-trip validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ─── Private Calculation Helper Methods ───────────────────────────────────

    private void validateCalculationInputs(Agreement agreement, RoomAllotment allotment, LocalDate calculationDate) {
        if (agreement == null) {
            throw new SettlementCalculationException("Agreement cannot be null");
        }
        if (allotment == null) {
            throw new SettlementCalculationException("Room allotment cannot be null");
        }
        if (calculationDate == null) {
            throw new SettlementCalculationException("Calculation date cannot be null");
        }
        if (!agreement.getId().equals(allotment.getAgreementId())) {
            throw new SettlementCalculationException("Agreement ID mismatch between agreement and allotment");
        }
    }

    private void calculateSecurityDepositComponent(SettlementCalculationResult result, 
                                                 RoomAgreementPlan planSnapshot) {
        BigDecimal securityDeposit = BigDecimal.ZERO;
        
        // Extract security deposit from plan charges if available
        if (planSnapshot.getCharges() != null 
                && planSnapshot.getCharges().getPaymentPlan() != null
                && planSnapshot.getCharges().getPaymentPlan().getExitSettlement() != null
                && planSnapshot.getCharges().getPaymentPlan().getExitSettlement().getEstimatedRefund() != null) {
            securityDeposit = planSnapshot.getCharges().getPaymentPlan().getExitSettlement().getEstimatedRefund();
        }
        
        result.setSecurityDeposit(securityDeposit);
        log.debug("Security deposit component: {}", securityDeposit);
    }

    private void calculateOutstandingRent(SettlementCalculationResult result, 
                                        Agreement agreement, 
                                        RoomAllotment allotment, 
                                        LocalDate calculationDate) {
        try {
            // Calculate outstanding rent based on plan snapshot
            BigDecimal outstandingRent = BigDecimal.ZERO;
            
            // For now, use a simple calculation until PaymentScheduleService method is available
            // This would typically query the payment schedule service
            if (agreement.getPlanSnapshot() != null && agreement.getPlanSnapshot().getRentDetails() != null) {
                BigDecimal monthlyRent = agreement.getPlanSnapshot().getRentDetails().getMonthlyRent();
                if (monthlyRent != null) {
                    // Simple calculation - could be enhanced with actual payment schedule lookup
                    outstandingRent = monthlyRent; // Default to one month outstanding
                }
            }
            
            result.setOutstandingRent(outstandingRent);
            log.debug("Outstanding rent: {}", outstandingRent);
            
        } catch (Exception e) {
            log.warn("Failed to calculate outstanding rent, defaulting to zero: {}", e.getMessage());
            result.setOutstandingRent(BigDecimal.ZERO);
        }
    }

    private void calculateOutstandingCharges(SettlementCalculationResult result, 
                                           Agreement agreement, 
                                           LocalDate calculationDate) {
        try {
            // Calculate outstanding other charges (electricity, maintenance, etc.)
            BigDecimal outstandingCharges = calculateOtherCharges(agreement.getId(), calculationDate);
            result.setOutstandingCharges(outstandingCharges);
            log.debug("Outstanding charges: {}", outstandingCharges);
            
        } catch (Exception e) {
            log.warn("Failed to calculate outstanding charges, defaulting to zero: {}", e.getMessage());
            result.setOutstandingCharges(BigDecimal.ZERO);
        }
    }

    private BigDecimal calculateOtherCharges(String agreementId, LocalDate calculationDate) {
        // This would integrate with other charge services
        // For now, return zero - to be implemented based on actual other charge system
        return BigDecimal.ZERO;
    }

    private void calculateEarlyExitPenalties(SettlementCalculationResult result, 
                                           Agreement agreement, 
                                           RoomAllotment allotment, 
                                           LocalDate calculationDate) {
        
        LocalDate originalEndDate = allotment.getEndDate();
        LocalDate actualEndDate = calculationDate;
        
        if (originalEndDate != null && actualEndDate.isBefore(originalEndDate)) {
            long daysEarly = ChronoUnit.DAYS.between(actualEndDate, originalEndDate);
            
            // Calculate penalty based on plan configuration
            BigDecimal monthlyRent = BigDecimal.ZERO;
            if (agreement.getPlanSnapshot() != null && agreement.getPlanSnapshot().getRentDetails() != null) {
                monthlyRent = agreement.getPlanSnapshot().getRentDetails().getMonthlyRent();
            }
            if (monthlyRent == null) {
                monthlyRent = BigDecimal.ZERO;
            }
            
            BigDecimal dailyRent = monthlyRent.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
            
            // Default penalty: 50% of remaining rent (configurable per plan)
            BigDecimal penaltyRate = BigDecimal.valueOf(0.5); // 50%
            BigDecimal earlyExitPenalty = dailyRent.multiply(BigDecimal.valueOf(daysEarly)).multiply(penaltyRate);
            
            result.setEarlyExitPenalty(earlyExitPenalty);
            result.setEarlyExitDays((int) daysEarly);
            
            log.debug("Early exit penalty: {} for {} days early exit", earlyExitPenalty, daysEarly);
        }
    }

    private void calculateDepositAdjustments(SettlementCalculationResult result, 
                                           RoomAgreementPlan planSnapshot) {
        // Calculate any deposit adjustments (damage charges, cleaning charges, etc.)
        // These would typically be input by the owner during settlement approval
        
        // For now, initialize to zero - these will be set by the settlement approval process
        result.setDamageCharges(BigDecimal.ZERO);
        result.setCleaningCharges(BigDecimal.ZERO);
        result.setOtherDeductions(BigDecimal.ZERO);
        
        log.debug("Deposit adjustments initialized to zero (to be set during approval)");
    }

    private void calculateFinalSettlementAmount(SettlementCalculationResult result) {
        BigDecimal totalDeductions = BigDecimal.ZERO;
        
        // Add all deduction components
        if (result.getOutstandingRent() != null) {
            totalDeductions = totalDeductions.add(result.getOutstandingRent());
        }
        if (result.getOutstandingCharges() != null) {
            totalDeductions = totalDeductions.add(result.getOutstandingCharges());
        }
        if (result.getDamageCharges() != null) {
            totalDeductions = totalDeductions.add(result.getDamageCharges());
        }
        if (result.getCleaningCharges() != null) {
            totalDeductions = totalDeductions.add(result.getCleaningCharges());
        }
        if (result.getOtherDeductions() != null) {
            totalDeductions = totalDeductions.add(result.getOtherDeductions());
        }
        if (result.getEarlyExitPenalty() != null) {
            totalDeductions = totalDeductions.add(result.getEarlyExitPenalty());
        }
        
        result.setTotalDeductions(totalDeductions);
        
        // Calculate final amount (deposit - deductions)
        BigDecimal securityDeposit = result.getSecurityDeposit() != null ? 
                result.getSecurityDeposit() : BigDecimal.ZERO;
        
        BigDecimal finalAmount = securityDeposit.subtract(totalDeductions);
        result.setFinalSettlementAmount(finalAmount.abs());
        
        // Determine settlement type
        if (finalAmount.compareTo(BigDecimal.ZERO) >= 0) {
            result.setSettlementType("OWNER_PAYABLE");
        } else {
            result.setSettlementType("TENANT_PAYABLE");
        }
        
        log.debug("Final settlement calculation - Deposit: {}, Deductions: {}, Final Amount: {} ({})",
                securityDeposit, totalDeductions, result.getFinalSettlementAmount(), result.getSettlementType());
    }

    // ─── Utility Methods ─────────────────────────────────────────────────────

    /**
     * Creates a settlement transaction object from calculation result
     */
    public SettlementTransaction createSettlementTransaction(SettlementCalculationResult calculationResult, 
                                                           String createdBy, 
                                                           String notes) {
        return SettlementTransaction.builder()
                .transactionId(java.util.UUID.randomUUID().toString())
                .agreementId(calculationResult.getAgreementId())
                .allotmentId(calculationResult.getAllotmentId().toString())
                .calculationDate(calculationResult.getCalculationDate())
                .securityDeposit(calculationResult.getSecurityDeposit())
                .outstandingRent(calculationResult.getOutstandingRent())
                .outstandingCharges(calculationResult.getOutstandingCharges())
                .damageCharges(calculationResult.getDamageCharges())
                .cleaningCharges(calculationResult.getCleaningCharges())
                .otherDeductions(calculationResult.getOtherDeductions())
                .earlyExitPenalty(calculationResult.getEarlyExitPenalty())
                .totalDeductions(calculationResult.getTotalDeductions())
                .finalSettlementAmount(calculationResult.getFinalSettlementAmount())
                .settlementType(calculationResult.getSettlementType())
                .createdBy(createdBy)
                .notes(notes)
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }

    /**
     * Validates settlement calculation result
     */
    public boolean isValidCalculationResult(SettlementCalculationResult result) {
        if (result == null) return false;
        if (result.getAgreementId() == null || result.getAgreementId().trim().isEmpty()) return false;
        if (result.getCalculationDate() == null) return false;
        if (result.getFinalSettlementAmount() == null) return false;
        if (result.getSettlementType() == null || result.getSettlementType().trim().isEmpty()) return false;
        
        return true;
    }

    /**
     * Gets calculation summary as human-readable string
     */
    public String getCalculationSummary(SettlementCalculationResult result) {
        if (!isValidCalculationResult(result)) {
            return "Invalid calculation result";
        }
        
        return String.format(
                "Settlement Summary - Agreement: %s, Date: %s, Deposit: ₹%.2f, Deductions: ₹%.2f, Final: ₹%.2f (%s)",
                result.getAgreementId().substring(0, Math.min(8, result.getAgreementId().length())),
                result.getCalculationDate(),
                result.getSecurityDeposit() != null ? result.getSecurityDeposit() : BigDecimal.ZERO,
                result.getTotalDeductions() != null ? result.getTotalDeductions() : BigDecimal.ZERO,
                result.getFinalSettlementAmount(),
                result.getSettlementType()
        );
    }
}