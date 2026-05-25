package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.PlanStatus;
import com.krunity.HostelManagment.model.plan.*;
import lombok.Data;

@Data
public class PlanResponse {
    private String id;
    private String planType;
    private String planName;
    private PlanStatus status;
    private Integer inUseFlag;
    private Boolean isActive;
    private Duration duration;
    private PaymentModel paymentModel;
    private RentDetails rentDetails;
    private Charges charges;
    private FreeFacilities freeFacilities;
    private LatePaymentPolicy latePaymentPolicy;
    private RulesAndRegulations rulesAndRegulations;
    private Restrictions restrictions;
    private AgreementCancellationRules agreementCancellationRules;
    private Legal legal;
    private PlanAudit audit;
    
    // Custom fields
    private java.util.Map<String, Object> customFields;
}

