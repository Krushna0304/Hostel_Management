package com.krunity.HostelManagment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO representing the result of a settlement calculation
 * Contains detailed breakdown of settlement components and final amounts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementCalculationResult {

    // ─── Identification ──────────────────────────────────────────────────────

    private String agreementId;
    private UUID allotmentId;
    private LocalDate calculationDate;

    // ─── Financial Components ────────────────────────────────────────────────

    /**
     * Original security deposit amount
     */
    private BigDecimal securityDeposit;

    /**
     * Outstanding rent amount due
     */
    private BigDecimal outstandingRent;

    /**
     * Outstanding other charges (electricity, maintenance, etc.)
     */
    private BigDecimal outstandingCharges;

    /**
     * Damage charges assessed by owner
     */
    private BigDecimal damageCharges;

    /**
     * Cleaning charges assessed by owner
     */
    private BigDecimal cleaningCharges;

    /**
     * Other miscellaneous deductions
     */
    private BigDecimal otherDeductions;

    /**
     * Early exit penalty (if applicable)
     */
    private BigDecimal earlyExitPenalty;

    /**
     * Total of all deductions
     */
    private BigDecimal totalDeductions;

    /**
     * Final settlement amount (absolute value)
     */
    private BigDecimal finalSettlementAmount;

    /**
     * Type of settlement: "OWNER_PAYABLE" or "TENANT_PAYABLE"
     */
    private String settlementType;

    // ─── Early Exit Details ──────────────────────────────────────────────────

    /**
     * Number of days tenant is exiting early (if applicable)
     */
    private Integer earlyExitDays;

    /**
     * Flag indicating if this is an early exit scenario
     */
    @Builder.Default
    private boolean isEarlyExit = false;

    // ─── Calculation Metadata ────────────────────────────────────────────────

    /**
     * Any calculation notes or warnings
     */
    private String calculationNotes;

    /**
     * Flag indicating if calculation was successful
     */
    @Builder.Default
    private boolean calculationSuccessful = true;

    /**
     * Error message if calculation failed
     */
    private String errorMessage;

    // ─── Business Logic Methods ──────────────────────────────────────────────

    /**
     * Checks if owner owes money to tenant
     */
    public boolean isOwnerPayable() {
        return "OWNER_PAYABLE".equals(settlementType);
    }

    /**
     * Checks if tenant owes money to owner
     */
    public boolean isTenantPayable() {
        return "TENANT_PAYABLE".equals(settlementType);
    }

    /**
     * Gets the settlement amount with proper sign (positive for owner payable, negative for tenant payable)
     */
    public BigDecimal getSignedSettlementAmount() {
        if (finalSettlementAmount == null) {
            return BigDecimal.ZERO;
        }
        return isOwnerPayable() ? finalSettlementAmount : finalSettlementAmount.negate();
    }

    /**
     * Checks if calculation result is valid
     */
    public boolean isValid() {
        return calculationSuccessful &&
               agreementId != null && !agreementId.trim().isEmpty() &&
               calculationDate != null &&
               finalSettlementAmount != null &&
               settlementType != null;
    }

    /**
     * Gets a human-readable summary of the calculation
     */
    public String getSummary() {
        if (!isValid()) {
            return "Invalid calculation result";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Settlement Calculation Summary:\n");
        summary.append(String.format("Agreement: %s\n", agreementId));
        summary.append(String.format("Calculation Date: %s\n", calculationDate));
        summary.append(String.format("Security Deposit: ₹%.2f\n", securityDeposit != null ? securityDeposit : BigDecimal.ZERO));
        
        if (outstandingRent != null && outstandingRent.compareTo(BigDecimal.ZERO) > 0) {
            summary.append(String.format("Outstanding Rent: ₹%.2f\n", outstandingRent));
        }
        if (outstandingCharges != null && outstandingCharges.compareTo(BigDecimal.ZERO) > 0) {
            summary.append(String.format("Outstanding Charges: ₹%.2f\n", outstandingCharges));
        }
        if (damageCharges != null && damageCharges.compareTo(BigDecimal.ZERO) > 0) {
            summary.append(String.format("Damage Charges: ₹%.2f\n", damageCharges));
        }
        if (cleaningCharges != null && cleaningCharges.compareTo(BigDecimal.ZERO) > 0) {
            summary.append(String.format("Cleaning Charges: ₹%.2f\n", cleaningCharges));
        }
        if (earlyExitPenalty != null && earlyExitPenalty.compareTo(BigDecimal.ZERO) > 0) {
            summary.append(String.format("Early Exit Penalty: ₹%.2f (%d days early)\n", earlyExitPenalty, earlyExitDays));
        }
        if (otherDeductions != null && otherDeductions.compareTo(BigDecimal.ZERO) > 0) {
            summary.append(String.format("Other Deductions: ₹%.2f\n", otherDeductions));
        }
        
        summary.append(String.format("Total Deductions: ₹%.2f\n", totalDeductions != null ? totalDeductions : BigDecimal.ZERO));
        summary.append(String.format("Final Amount: ₹%.2f (%s)\n", finalSettlementAmount, 
            isOwnerPayable() ? "Owner pays to Tenant" : "Tenant pays to Owner"));
        
        if (calculationNotes != null && !calculationNotes.trim().isEmpty()) {
            summary.append(String.format("Notes: %s\n", calculationNotes));
        }
        
        return summary.toString();
    }

    /**
     * Creates a copy of this result with updated damage and cleaning charges
     */
    public SettlementCalculationResult withOwnerAdjustments(BigDecimal damageCharges, 
                                                           BigDecimal cleaningCharges, 
                                                           BigDecimal otherDeductions) {
        return SettlementCalculationResult.builder()
                .agreementId(this.agreementId)
                .allotmentId(this.allotmentId)
                .calculationDate(this.calculationDate)
                .securityDeposit(this.securityDeposit)
                .outstandingRent(this.outstandingRent)
                .outstandingCharges(this.outstandingCharges)
                .damageCharges(damageCharges)
                .cleaningCharges(cleaningCharges)
                .otherDeductions(otherDeductions)
                .earlyExitPenalty(this.earlyExitPenalty)
                .earlyExitDays(this.earlyExitDays)
                .isEarlyExit(this.isEarlyExit)
                .calculationNotes(this.calculationNotes)
                .calculationSuccessful(this.calculationSuccessful)
                .errorMessage(this.errorMessage)
                .build()
                .recalculateFinalAmount();
    }

    /**
     * Recalculates the total deductions and final settlement amount
     */
    public SettlementCalculationResult recalculateFinalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        
        if (outstandingRent != null) total = total.add(outstandingRent);
        if (outstandingCharges != null) total = total.add(outstandingCharges);
        if (damageCharges != null) total = total.add(damageCharges);
        if (cleaningCharges != null) total = total.add(cleaningCharges);
        if (otherDeductions != null) total = total.add(otherDeductions);
        if (earlyExitPenalty != null) total = total.add(earlyExitPenalty);
        
        this.totalDeductions = total;
        
        BigDecimal deposit = securityDeposit != null ? securityDeposit : BigDecimal.ZERO;
        BigDecimal finalAmount = deposit.subtract(total);
        
        this.finalSettlementAmount = finalAmount.abs();
        
        if (finalAmount.compareTo(BigDecimal.ZERO) >= 0) {
            this.settlementType = "OWNER_PAYABLE";
        } else {
            this.settlementType = "TENANT_PAYABLE";
        }
        
        return this;
    }

    /**
     * Adds a note to the calculation notes
     */
    public void addNote(String note) {
        if (calculationNotes == null || calculationNotes.trim().isEmpty()) {
            calculationNotes = note;
        } else {
            calculationNotes += "; " + note;
        }
    }

    /**
     * Marks calculation as failed with error message
     */
    public void markAsFailed(String errorMessage) {
        this.calculationSuccessful = false;
        this.errorMessage = errorMessage;
        this.finalSettlementAmount = BigDecimal.ZERO;
        this.settlementType = "CALCULATION_FAILED";
    }
}