package com.krunity.HostelManagment.Mapper;

import com.krunity.HostelManagment.dto.AgreementCardResponse;
import com.krunity.HostelManagment.dto.AgreementResponse;
import com.krunity.HostelManagment.dto.CreateRoomAgreementRequest;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.model.Room;
import com.krunity.HostelManagment.model.Floor;
import com.krunity.HostelManagment.model.Hostel;
import com.krunity.HostelManagment.enums.AgreementType;
import com.krunity.HostelManagment.model.RoomAgreementPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        
        // Include complete plan snapshot
        response.setPlanSnapshot(agreement.getPlanSnapshot());
        
        return response;
    }
    
    public static AgreementResponse toResponseWithDetails(Agreement agreement, User tenant, Room room, Floor floor, Hostel hostel) {
        AgreementResponse response = toResponse(agreement);
        
        // Set tenant details
        if (tenant != null) {
            response.setTenantName(tenant.getDisplayName());
            response.setTenantMobileNumber(tenant.getPhoneNumber());
        }
        
        // Set location details
        if (room != null) {
            response.setRoomNumber(room.getRoomNumber());
        }
        if (floor != null) {
            response.setFloorNumber(floor.getFloorNumber());
        }
        if (hostel != null) {
            response.setHostelName(hostel.getHostelName());
        }
        
        return response;
    }
    
    public static AgreementCardResponse toCardResponse(Agreement agreement, User tenant, Room room, Floor floor, Hostel hostel) {
        AgreementCardResponse response = new AgreementCardResponse();
        
        // Basic agreement info
        response.setId(agreement.getId());
        response.setType(agreement.getType());
        response.setStatus(agreement.getStatus());
        response.setStartDate(agreement.getStartDate());
        response.setCreatedAt(agreement.getCreatedAt() != null ? 
            LocalDateTime.ofInstant(agreement.getCreatedAt(), java.time.ZoneId.systemDefault()) : null);
        response.setActivatedAt(agreement.getActivatedAt() != null ? 
            LocalDateTime.ofInstant(agreement.getActivatedAt(), java.time.ZoneId.systemDefault()) : null);
        
        // QR info for pending agreements
        if (agreement.getStatus() == com.krunity.HostelManagment.enums.AgreementStatus.PENDING_TENANT_ACTION) {
            response.setQrToken(agreement.getQrToken());
            response.setQrExpiry(agreement.getQrExpiry() != null ? 
                LocalDateTime.ofInstant(agreement.getQrExpiry(), java.time.ZoneId.systemDefault()) : null);
        }
        
        // Tenant details
        if (tenant != null) {
            response.setTenantName(tenant.getDisplayName());
            response.setTenantMobileNumber(tenant.getPhoneNumber());
        }
        
        // Location details
        if (room != null) {
            response.setRoomNumber(room.getRoomNumber());
        }
        if (floor != null) {
            response.setFloorNumber(floor.getFloorNumber());
        }
        if (hostel != null) {
            response.setHostelName(hostel.getHostelName());
        }
        
        // Plan and financial details
        RoomAgreementPlan plan = agreement.getPlanSnapshot();
        if (plan != null) {
            response.setPlanName(plan.getPlanName());
            
            // Duration
            if (plan.getDuration() != null) {
                response.setPlanDurationValue(plan.getDuration().getValue());
                response.setPlanDurationUnit(plan.getDuration().getUnit().toString());
            }
            
            // Calculate financial details
            BigDecimal baseRent = plan.getRentDetails() != null ? plan.getRentDetails().getMonthlyRent() : BigDecimal.ZERO;
            BigDecimal securityDeposit = BigDecimal.ZERO;
            
            if (plan.getCharges() != null && plan.getCharges().getSecurityDeposit() != null) {
                securityDeposit = plan.getCharges().getSecurityDeposit().getAmount();
            }
            
            // Calculate recurring charges
            BigDecimal recurringCharges = BigDecimal.ZERO;
            if (plan.getCharges() != null) {
                if (plan.getCharges().getCleaningCharges() != null && plan.getCharges().getCleaningCharges().getMonthlyCleaningCharge() != null) {
                    recurringCharges = recurringCharges.add(plan.getCharges().getCleaningCharges().getMonthlyCleaningCharge().getAmount());
                }
                if (plan.getCharges().getMaintenanceCharges() != null && plan.getCharges().getMaintenanceCharges().getMonthlyMaintenanceCharge() != null) {
                    recurringCharges = recurringCharges.add(plan.getCharges().getMaintenanceCharges().getMonthlyMaintenanceCharge().getAmount());
                }
                if (plan.getCharges().getUtilityCharges() != null) {
                    if (plan.getCharges().getUtilityCharges().getElectricity() != null) {
                        recurringCharges = recurringCharges.add(plan.getCharges().getUtilityCharges().getElectricity().getFixedAmount());
                    }
                    if (plan.getCharges().getUtilityCharges().getWater() != null) {
                        recurringCharges = recurringCharges.add(plan.getCharges().getUtilityCharges().getWater().getMonthlyAmount());
                    }
                }
                if (plan.getCharges().getCustomCharges() != null && plan.getCharges().getCustomCharges().getMonthlyRecurringCharges() != null) {
                    for (var customCharge : plan.getCharges().getCustomCharges().getMonthlyRecurringCharges()) {
                        recurringCharges = recurringCharges.add(customCharge.getAmount());
                    }
                }
            }
            
            BigDecimal monthlyTotal = baseRent.add(recurringCharges);
            response.setMonthlyRent(monthlyTotal);
            response.setSecurityDeposit(securityDeposit);
            
            // Calculate installment details
            if (plan.getPaymentModel() != null) {
                Integer numberOfInstallments = plan.getPaymentModel().getInstallments();
                String paymentTiming = plan.getPaymentModel().getPaymentTiming() != null ? 
                    plan.getPaymentModel().getPaymentTiming().toString() : "PREPAID";
                
                response.setNumberOfInstallments(numberOfInstallments);
                response.setPaymentTiming(paymentTiming);
                
                if (numberOfInstallments != null && numberOfInstallments > 1) {
                    Integer totalDuration = plan.getDuration() != null ? plan.getDuration().getValue() : 12;
                    Integer monthsPerInstallment = (int) Math.ceil((double) totalDuration / numberOfInstallments);
                    BigDecimal installmentAmount = monthlyTotal.multiply(BigDecimal.valueOf(monthsPerInstallment));
                    response.setInstallmentAmount(installmentAmount);
                } else {
                    response.setInstallmentAmount(monthlyTotal);
                }
            } else {
                response.setNumberOfInstallments(1);
                response.setPaymentTiming("PREPAID");
                response.setInstallmentAmount(monthlyTotal);
            }
        } else {
            // Fallback to legacy fields
            response.setPlanName("Legacy Plan");
            response.setMonthlyRent(agreement.getRent() != null ? agreement.getRent() : BigDecimal.ZERO);
            response.setSecurityDeposit(agreement.getDeposit() != null ? agreement.getDeposit() : BigDecimal.ZERO);
            response.setInstallmentAmount(agreement.getRent() != null ? agreement.getRent() : BigDecimal.ZERO);
            response.setNumberOfInstallments(1);
            response.setPaymentTiming("PREPAID");
        }
        
        return response;
    }
}

