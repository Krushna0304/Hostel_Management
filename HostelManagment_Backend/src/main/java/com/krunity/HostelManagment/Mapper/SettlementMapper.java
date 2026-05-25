package com.krunity.HostelManagment.Mapper;

import com.krunity.HostelManagment.dto.SettlementResponseDto;
import com.krunity.HostelManagment.model.SettlementRequest;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SettlementMapper {
    
    public static SettlementResponseDto toResponseDto(SettlementRequest settlement) {
        if (settlement == null) {
            return null;
        }
        
        return SettlementResponseDto.builder()
                .settlementId(settlement.getSettlementId())
                .agreementId(settlement.getAgreementId())
                .status(settlement.getStatus())
                
                // User information (safely accessing without triggering lazy loading issues)
                .tenantId(safeGetTenantId(settlement))
                .tenantName(safeGetTenantName(settlement))
                .ownerId(safeGetOwnerId(settlement))
                .ownerName(safeGetOwnerName(settlement))
                
                // Room information
                .roomId(safeGetRoomId(settlement))
                .roomNumber(safeGetRoomNumber(settlement))
                
                // Financial details
                .securityDeposit(settlement.getSecurityDeposit())
                .outstandingRent(settlement.getOutstandingRent())
                .outstandingCharges(settlement.getOutstandingCharges())
                .damageCharges(settlement.getDamageCharges())
                .cleaningCharges(settlement.getCleaningCharges())
                .otherDeductions(settlement.getOtherDeductions())
                .totalDeductions(settlement.getTotalDeductions())
                .finalSettlementAmount(settlement.getFinalSettlementAmount())
                .settlementType(settlement.getSettlementType())
                
                // Notes and descriptions
                .tenantNotes(settlement.getTenantNotes())
                .ownerNotes(settlement.getOwnerNotes())
                .damageDescription(settlement.getDamageDescription())
                
                // Payment information
                .paymentReference(settlement.getPaymentReference())
                
                // Timestamps
                .createdAt(settlement.getCreatedAt())
                .updatedAt(settlement.getUpdatedAt())
                .settledAt(settlement.getSettledAt())
                .build();
    }
    
    private static java.util.UUID safeGetTenantId(SettlementRequest settlement) {
        try {
            return settlement.getTenant() != null && Hibernate.isInitialized(settlement.getTenant()) 
                ? settlement.getTenant().getUserId() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String safeGetTenantName(SettlementRequest settlement) {
        try {
            return settlement.getTenant() != null && Hibernate.isInitialized(settlement.getTenant()) 
                ? settlement.getTenant().getDisplayName() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static java.util.UUID safeGetOwnerId(SettlementRequest settlement) {
        try {
            return settlement.getOwner() != null && Hibernate.isInitialized(settlement.getOwner()) 
                ? settlement.getOwner().getUserId() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String safeGetOwnerName(SettlementRequest settlement) {
        try {
            return settlement.getOwner() != null && Hibernate.isInitialized(settlement.getOwner()) 
                ? settlement.getOwner().getDisplayName() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static java.util.UUID safeGetRoomId(SettlementRequest settlement) {
        try {
            return settlement.getRoom() != null && Hibernate.isInitialized(settlement.getRoom()) 
                ? settlement.getRoom().getRoomId() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String safeGetRoomNumber(SettlementRequest settlement) {
        try {
            return settlement.getRoom() != null && Hibernate.isInitialized(settlement.getRoom()) 
                ? settlement.getRoom().getRoomNumber() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    public static List<SettlementResponseDto> toResponseDtoList(List<SettlementRequest> settlements) {
        if (settlements == null) {
            return null;
        }
        
        return settlements.stream()
                .map(SettlementMapper::toResponseDto)
                .collect(Collectors.toList());
    }
}