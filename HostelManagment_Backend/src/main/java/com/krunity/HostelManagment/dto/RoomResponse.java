package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.model.Floor;
import com.krunity.HostelManagment.model.Hostel;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

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
}
