package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.PlanType;
import com.krunity.HostelManagment.model.plan.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CreatePlanRequest {

    @NotBlank(message = "Plan name is required")
    private String planName;

    @NotNull(message = "Plan type is required")
    private PlanType planType;

    @NotNull(message = "Rent details are required")
    private RentDetails rentDetails;

    private Duration duration;
    private PaymentModel paymentModel;
    private Charges charges;
    private FreeFacilities freeFacilities;
    private LatePaymentPolicy latePaymentPolicy;
    private RulesAndRegulations rulesAndRegulations;
    private Restrictions restrictions;
    private AgreementCancellationRules agreementCancellationRules;
    private Legal legal;

    // Owner can add any custom key-value fields
    private Map<String, Object> customFields;
}
