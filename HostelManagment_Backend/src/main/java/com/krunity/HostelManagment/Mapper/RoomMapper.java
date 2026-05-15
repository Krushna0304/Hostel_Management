package com.krunity.HostelManagment.Mapper;


import com.krunity.HostelManagment.dto.CreateRoomRequest;
import com.krunity.HostelManagment.dto.RoomResponse;
import com.krunity.HostelManagment.model.Floor;
import com.krunity.HostelManagment.model.Hostel;
import com.krunity.HostelManagment.model.Room;

public class RoomMapper {

    public RoomMapper(){}

    public static Room toEntity(CreateRoomRequest createRoomRequest,Hostel hostel, Floor floor){
        return  Room.builder()
                .hostel(hostel)
                .floor(floor)
                .roomDetails(createRoomRequest.getRoomDetails())
                .roomNumber(createRoomRequest.getRoomNumber())
                .totalBeds(createRoomRequest.getTotalBeds())
                .availableBeds(createRoomRequest.getAvailableBeds())
                .isActive(createRoomRequest.getIsActive())
                .roomType(createRoomRequest.getRoomType())
                .build();
    }

    public static RoomResponse toDto(Room room){
        return RoomResponse.builder()
                .roomId(room.getRoomId().toString())
                .hostelId(room.getHostel().getHostelId().toString())
                .floorId(room.getFloor().getFloorId().toString())
                .roomDetails(room.getRoomDetails())
                .roomNumber(room.getRoomNumber())
                .totalBeds(room.getTotalBeds())
                .availableBeds(room.getAvailableBeds())
                .isActive(room.getIsActive())
                .roomType(room.getRoomType() != null ? room.getRoomType().name() : "PG_ROOM")
                .build();
    }
}
