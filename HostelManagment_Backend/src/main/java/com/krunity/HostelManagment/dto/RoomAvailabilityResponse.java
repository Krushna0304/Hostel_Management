package com.krunity.HostelManagment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomAvailabilityResponse {

    private UUID roomId;
    private String roomName;
    private Integer totalBeds;
    private Integer availableBeds;

    public RoomAvailabilityResponse(UUID roomId, String roomName, Integer totalBeds, Long occupiedCount) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.totalBeds = totalBeds;
        this.availableBeds = totalBeds - occupiedCount.intValue();
    }
}
