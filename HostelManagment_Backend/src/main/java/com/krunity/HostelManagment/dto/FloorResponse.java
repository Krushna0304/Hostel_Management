package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.model.Hostel;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class FloorResponse {
    private String floorId;
    private String hostel;
    private Integer floorNumber;
    private Integer totalRooms;
}
