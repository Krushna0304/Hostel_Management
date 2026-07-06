package com.krunity.HostelManagment.Mapper;


import com.krunity.HostelManagment.dto.RoomTenantResponse;
import com.krunity.HostelManagment.model.RoomAllotment;

public class RoomAllotmentMapper {

    public static RoomTenantResponse mapToRoomTenantResponse(RoomAllotment roomAllotment){
        return RoomTenantResponse.builder()
                .roomId(roomAllotment.getRoom().getRoomId().toString())
                .allotmentId(roomAllotment.getAllotmentId().toString())
                .tenantId(roomAllotment.getTenant().getUserId().toString())
                .tenantName(roomAllotment.getTenant().getDisplayName())
                .phoneNumber(roomAllotment.getTenant().getPhoneNumber())
                .planName(null) // Plan info not directly available in RoomAllotment
                .allotmentDate(roomAllotment.getStartDate() != null
                        ? java.sql.Date.valueOf(roomAllotment.getStartDate())
                        : null)
                .agreementEndDate(roomAllotment.getEndDate()) // End date from room_allotments table
                .roomAllotmentStatus(roomAllotment.getRoomAllotmentStatus().name())
                .coTenantNames(null) // PG rooms don't have co-tenants
                .agreementType("ROOM") // This will be overridden in the service
                .build();
    }
}
