package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.AgreementStatus;
import com.krunity.HostelManagment.enums.AgreementType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AgreementCardResponse {
    private String id;
    private AgreementType type;
    private AgreementStatus status;
    
    // Plan information for card display
    private String planName;
    private Integer planDurationValue;
    private String planDurationUnit;
    
    // Financial information for card display
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private BigDecimal installmentAmount;
    private Integer numberOfInstallments;
    private String paymentTiming;
    
    // Dates for card display
    private LocalDate startDate;
    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
    
    // QR information for pending agreements
    private String qrToken;
    private LocalDateTime qrExpiry;
    
    // Tenant and location information for card display
    private String tenantName;
    private String tenantMobileNumber;
    private String hostelName;
    private String roomNumber;
    private Integer floorNumber;
}