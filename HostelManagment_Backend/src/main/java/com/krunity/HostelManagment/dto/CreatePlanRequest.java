package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.PlanType;
import com.krunity.HostelManagment.model.plan.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
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

    // Custom charges (for backward compatibility with frontend)
    private List<EnhancedCharge> oneTimeCharges;
    private List<EnhancedCharge> monthlyRecurringCharges;

    // Owner can add any custom key-value fields
    private Map<String, Object> customFields;
}
