package com.krunity.HostelManagment.Mapper;

import com.krunity.HostelManagment.dto.PlanResponse;
import com.krunity.HostelManagment.model.RoomAgreementPlan;

public class RoomAgreementPlanMapper {
    public static PlanResponse toDto(RoomAgreementPlan plan) {
        PlanResponse response = new PlanResponse();
        response.setId(plan.getId());
        response.setPlanType(plan.getPlanType());
        response.setPlanName(plan.getPlanName());
        response.setStatus(plan.getStatus());
        response.setDuration(plan.getDuration());
        response.setPaymentModel(plan.getPaymentModel());
        response.setRentDetails(plan.getRentDetails());
        response.setCharges(plan.getCharges());
        response.setFreeFacilities(plan.getFreeFacilities());
        response.setLatePaymentPolicy(plan.getLatePaymentPolicy());
        response.setRulesAndRegulations(plan.getRulesAndRegulations());
        response.setRestrictions(plan.getRestrictions());
        response.setAgreementCancellationRules(plan.getAgreementCancellationRules());
        response.setLegal(plan.getLegal());
        response.setAudit(plan.getAudit());
        return response;
    }
}
