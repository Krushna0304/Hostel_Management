package com.krunity.HostelManagment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO representing a settlement transaction
 * Used for JSON serialization/deserialization of settlement transaction data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettlementTransaction {

    // ─── Transaction Identity ────────────────────────────────────────────────

    private String transactionId;
    private String agreementId;
    private String allotmentId;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate calculationDate;

    // ─── Financial Breakdown ─────────────────────────────────────────────────

    private BigDecimal securityDeposit;
    private BigDecimal outstandingRent;
    private BigDecimal outstandingCharges;
    private BigDecimal damageCharges;
    private BigDecimal cleaningCharges;
    private BigDecimal otherDeductions;
    private BigDecimal earlyExitPenalty;
    private BigDecimal totalDeductions;
    private BigDecimal finalSettlementAmount;
    private String settlementType; // "OWNER_PAYABLE" or "TENANT_PAYABLE"

    // ─── Transaction Metadata ────────────────────────────────────────────────

    private String createdBy;
    private String notes;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private String status; // "PENDING", "PROCESSED", "COMPLETED", "CANCELLED"

    // ─── Early Exit Information ──────────────────────────────────────────────

    private Integer earlyExitDays;
    
    @Builder.Default
    private boolean isEarlyExit = false;

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
     * Gets settlement amount with proper sign
     */
    public BigDecimal getSignedAmount() {
        if (finalSettlementAmount == null) {
            return BigDecimal.ZERO;
        }
        return isOwnerPayable() ? finalSettlementAmount : finalSettlementAmount.negate();
    }

    /**
     * Validates transaction data completeness
     */
    public boolean isValid() {
        return transactionId != null && !transactionId.trim().isEmpty() &&
               agreementId != null && !agreementId.trim().isEmpty() &&
               calculationDate != null &&
               finalSettlementAmount != null &&
               settlementType != null && !settlementType.trim().isEmpty();
    }

    /**
     * Gets transaction summary
     */
    public String getSummary() {
        if (!isValid()) {
            return "Invalid transaction";
        }
        
        return String.format("Transaction %s: ₹%.2f (%s) for Agreement %s", 
                transactionId.substring(0, Math.min(8, transactionId.length())),
                finalSettlementAmount,
                settlementType,
                agreementId.substring(0, Math.min(8, agreementId.length())));
    }

    /**
     * Creates a copy with updated status
     */
    public SettlementTransaction withStatus(String newStatus) {
        return SettlementTransaction.builder()
                .transactionId(this.transactionId)
                .agreementId(this.agreementId)
                .allotmentId(this.allotmentId)
                .calculationDate(this.calculationDate)
                .securityDeposit(this.securityDeposit)
                .outstandingRent(this.outstandingRent)
                .outstandingCharges(this.outstandingCharges)
                .damageCharges(this.damageCharges)
                .cleaningCharges(this.cleaningCharges)
                .otherDeductions(this.otherDeductions)
                .earlyExitPenalty(this.earlyExitPenalty)
                .totalDeductions(this.totalDeductions)
                .finalSettlementAmount(this.finalSettlementAmount)
                .settlementType(this.settlementType)
                .createdBy(this.createdBy)
                .notes(this.notes)
                .createdAt(this.createdAt)
                .status(newStatus)
                .earlyExitDays(this.earlyExitDays)
                .isEarlyExit(this.isEarlyExit)
                .build();
    }

    /**
     * Checks if transaction has any deductions
     */
    public boolean hasDeductions() {
        return totalDeductions != null && totalDeductions.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if transaction involves early exit
     */
    public boolean isEarlyExitTransaction() {
        return isEarlyExit && earlyExitPenalty != null && earlyExitPenalty.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Gets detailed breakdown as formatted string
     */
    public String getDetailedBreakdown() {
        StringBuilder breakdown = new StringBuilder();
        breakdown.append("Settlement Transaction Details:\n");
        breakdown.append(String.format("Transaction ID: %s\n", transactionId));
        breakdown.append(String.format("Agreement ID: %s\n", agreementId));
        breakdown.append(String.format("Calculation Date: %s\n", calculationDate));
        breakdown.append(String.format("Security Deposit: ₹%.2f\n", 
            securityDeposit != null ? securityDeposit : BigDecimal.ZERO));
        
        if (outstandingRent != null && outstandingRent.compareTo(BigDecimal.ZERO) > 0) {
            breakdown.append(String.format("Outstanding Rent: ₹%.2f\n", outstandingRent));
        }
        if (outstandingCharges != null && outstandingCharges.compareTo(BigDecimal.ZERO) > 0) {
            breakdown.append(String.format("Outstanding Charges: ₹%.2f\n", outstandingCharges));
        }
        if (damageCharges != null && damageCharges.compareTo(BigDecimal.ZERO) > 0) {
            breakdown.append(String.format("Damage Charges: ₹%.2f\n", damageCharges));
        }
        if (cleaningCharges != null && cleaningCharges.compareTo(BigDecimal.ZERO) > 0) {
            breakdown.append(String.format("Cleaning Charges: ₹%.2f\n", cleaningCharges));
        }
        if (earlyExitPenalty != null && earlyExitPenalty.compareTo(BigDecimal.ZERO) > 0) {
            breakdown.append(String.format("Early Exit Penalty: ₹%.2f (%d days)\n", 
                earlyExitPenalty, earlyExitDays != null ? earlyExitDays : 0));
        }
        if (otherDeductions != null && otherDeductions.compareTo(BigDecimal.ZERO) > 0) {
            breakdown.append(String.format("Other Deductions: ₹%.2f\n", otherDeductions));
        }
        
        breakdown.append(String.format("Total Deductions: ₹%.2f\n", 
            totalDeductions != null ? totalDeductions : BigDecimal.ZERO));
        breakdown.append(String.format("Final Amount: ₹%.2f (%s)\n", 
            finalSettlementAmount, 
            isOwnerPayable() ? "Owner pays to Tenant" : "Tenant pays to Owner"));
        
        if (notes != null && !notes.trim().isEmpty()) {
            breakdown.append(String.format("Notes: %s\n", notes));
        }
        
        breakdown.append(String.format("Created by: %s at %s\n", createdBy, createdAt));
        
        return breakdown.toString();
    }

    // ─── Static Factory Methods ──────────────────────────────────────────────

    /**
     * Creates a settlement transaction from calculation result
     */
    public static SettlementTransaction fromCalculationResult(SettlementCalculationResult result, 
                                                            String createdBy, 
                                                            String notes) {
        return SettlementTransaction.builder()
                .transactionId(java.util.UUID.randomUUID().toString())
                .agreementId(result.getAgreementId())
                .allotmentId(result.getAllotmentId() != null ? result.getAllotmentId().toString() : null)
                .calculationDate(result.getCalculationDate())
                .securityDeposit(result.getSecurityDeposit())
                .outstandingRent(result.getOutstandingRent())
                .outstandingCharges(result.getOutstandingCharges())
                .damageCharges(result.getDamageCharges())
                .cleaningCharges(result.getCleaningCharges())
                .otherDeductions(result.getOtherDeductions())
                .earlyExitPenalty(result.getEarlyExitPenalty())
                .totalDeductions(result.getTotalDeductions())
                .finalSettlementAmount(result.getFinalSettlementAmount())
                .settlementType(result.getSettlementType())
                .createdBy(createdBy)
                .notes(notes)
                .createdAt(LocalDateTime.now())
                .status("PENDING")
                .earlyExitDays(result.getEarlyExitDays())
                .isEarlyExit(result.isEarlyExit())
                .build();
    }

    // ─── Override equals and hashCode for proper comparison ──────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        SettlementTransaction that = (SettlementTransaction) o;
        
        return Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(agreementId, that.agreementId) &&
               Objects.equals(allotmentId, that.allotmentId) &&
               Objects.equals(calculationDate, that.calculationDate) &&
               Objects.equals(securityDeposit, that.securityDeposit) &&
               Objects.equals(outstandingRent, that.outstandingRent) &&
               Objects.equals(outstandingCharges, that.outstandingCharges) &&
               Objects.equals(damageCharges, that.damageCharges) &&
               Objects.equals(cleaningCharges, that.cleaningCharges) &&
               Objects.equals(otherDeductions, that.otherDeductions) &&
               Objects.equals(earlyExitPenalty, that.earlyExitPenalty) &&
               Objects.equals(totalDeductions, that.totalDeductions) &&
               Objects.equals(finalSettlementAmount, that.finalSettlementAmount) &&
               Objects.equals(settlementType, that.settlementType) &&
               Objects.equals(createdBy, that.createdBy) &&
               Objects.equals(status, that.status) &&
               Objects.equals(earlyExitDays, that.earlyExitDays) &&
               isEarlyExit == that.isEarlyExit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, agreementId, allotmentId, calculationDate,
                securityDeposit, outstandingRent, outstandingCharges, damageCharges,
                cleaningCharges, otherDeductions, earlyExitPenalty, totalDeductions,
                finalSettlementAmount, settlementType, createdBy, status,
                earlyExitDays, isEarlyExit);
    }
}