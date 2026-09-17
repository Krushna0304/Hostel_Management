package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.AgreementStatus;
import com.krunity.HostelManagment.enums.AgreementType;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class AgreementResponse {
    private String id;
    private AgreementType type;
    private AgreementStatus status;
    private UUID userId;
    private UUID roomId;
    private BigDecimal rent;
    private BigDecimal deposit;
    private BigDecimal refundableAmount;
    // Present only for an extension; the backend-calculated amount payable now.
    private BigDecimal activationPayableAmount;
    private BigDecimal previousAgreementRefundableCredit;
    private BigDecimal cleaningCharges;
    private BigDecimal maintenanceCharges;
    private String lightBillPolicy;
    private List<String> facilities;
    private Boolean parkingAllowed;
    private LocalDate startDate;
    private LocalDate endDate;
    private String qrToken;
    private Instant qrExpiry;
    private Boolean qrUsed;
    private Boolean isAccepted;
    private Instant createdAt;
    private Instant activatedAt;
    
    // Complete plan snapshot with all details
    private RoomAgreementPlan planSnapshot;
    
    // Tenant details
    private String tenantName;
    private String tenantMobileNumber;
    
    // Location details
    private String hostelName;
    private String roomNumber;
    private Integer floorNumber;
}

