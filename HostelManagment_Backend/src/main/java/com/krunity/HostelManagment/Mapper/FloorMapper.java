package com.krunity.HostelManagment.Mapper;

import com.krunity.HostelManagment.dto.CreateFloorRequest;
import com.krunity.HostelManagment.dto.FloorResponse;
import com.krunity.HostelManagment.model.Floor;
import com.krunity.HostelManagment.model.Hostel;

public class FloorMapper {

    public static Floor toEntity(CreateFloorRequest createFloorRequest,Hostel hostel) {
        return Floor.builder()
                .hostel(hostel)
                .floorNumber(createFloorRequest.getFloorNumber())
                .build();
    }

    public static FloorResponse toDto(Floor floor) {
        return FloorResponse.builder()
                .hostel(floor.getHostel().getHostelId().toString())
                .floorId(floor.getFloorId().toString())
                .floorNumber(floor.getFloorNumber())
                .totalRooms(floor.getTotalRooms())
                .build();
    }
}
