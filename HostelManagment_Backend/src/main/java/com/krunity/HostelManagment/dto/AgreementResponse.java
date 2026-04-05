package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.AgreementStatus;
import com.krunity.HostelManagment.enums.AgreementType;
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
    private BigDecimal cleaningCharges;
    private BigDecimal maintenanceCharges;
    private String lightBillPolicy;
    private List<String> facilities;
    private Boolean parkingAllowed;
    private LocalDate startDate;
    private String qrToken;
    private Instant qrExpiry;
    private Boolean qrUsed;
    private Instant createdAt;
    private Instant activatedAt;
}

