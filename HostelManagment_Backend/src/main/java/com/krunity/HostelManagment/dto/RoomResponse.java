package com.krunity.HostelManagment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomResponse {
    private String roomId;
    private String hostelId;
    private String floorId;
    private String roomNumber;
    private String roomDetails;
    private Integer totalBeds;
    private Integer availableBeds;
    private Boolean isActive;
    private String roomType;
}
