package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.model.Floor;
import com.krunity.HostelManagment.model.Hostel;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateRoomRequest {
    private String roomNumber;
    private String roomDetails;
    private Integer totalBeds;
    private Integer availableBeds;
    private Boolean isActive;
}
