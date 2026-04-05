package com.krunity.HostelManagment.Mapper;

import com.krunity.HostelManagment.dto.AgreementResponse;
import com.krunity.HostelManagment.dto.CreateRoomAgreementRequest;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.enums.AgreementType;
import com.krunity.HostelManagment.model.RoomAgreementPlan;

public class AgreementMapper {
    
    public static Agreement toEntity(CreateRoomAgreementRequest request, RoomAgreementPlan planSnapshot) {
        Agreement.AgreementBuilder builder = Agreement.builder()
                .type(AgreementType.ROOM)
                .userId(request.getUserId())
                .roomId(request.getRoomId())
                .planId(request.getPlanId())
                .planSnapshot(planSnapshot)
                .startDate(request.getStartDate());
        
        // Populate legacy fields from plan snapshot for backward compatibility
        if (planSnapshot != null && planSnapshot.getRentDetails() != null) {
            builder.rent(planSnapshot.getRentDetails().getMonthlyRent());
        }
        if (planSnapshot != null && planSnapshot.getCharges() != null) {
            if (planSnapshot.getCharges().getSecurityDeposit() != null) {
                builder.deposit(planSnapshot.getCharges().getSecurityDeposit().getAmount());
            }
            if (planSnapshot.getCharges().getCleaningCharges() != null && planSnapshot.getCharges().getCleaningCharges().getDeepCleaningOnExit() != null) {
                builder.cleaningCharges(planSnapshot.getCharges().getCleaningCharges().getDeepCleaningOnExit().getAmount());
            }
        }
        
        return builder.build();
    }
    
    public static AgreementResponse toResponse(Agreement agreement) {
        AgreementResponse response = new AgreementResponse();
        response.setId(agreement.getId());
        response.setType(agreement.getType());
        response.setStatus(agreement.getStatus());
        response.setUserId(agreement.getUserId());
        response.setRoomId(agreement.getRoomId());
        response.setRent(agreement.getRent());
        response.setDeposit(agreement.getDeposit());
        response.setCleaningCharges(agreement.getCleaningCharges());
        response.setMaintenanceCharges(agreement.getMaintenanceCharges());
        response.setLightBillPolicy(agreement.getLightBillPolicy());
        response.setFacilities(agreement.getFacilities());
        response.setParkingAllowed(agreement.getParkingAllowed());
        response.setStartDate(agreement.getStartDate());
        response.setQrToken(agreement.getQrToken());
        response.setQrExpiry(agreement.getQrExpiry());
        response.setQrUsed(agreement.getQrUsed());
        response.setCreatedAt(agreement.getCreatedAt());
        response.setActivatedAt(agreement.getActivatedAt());
        return response;
    }
}

