package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.RoomType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRoomRequest {
    private String roomNumber;
    private String roomDetails;
    private Integer totalBeds;
    private Integer availableBeds;
    private Boolean isActive;

    @NotNull(message = "Room type is required")
    private RoomType roomType;
}
