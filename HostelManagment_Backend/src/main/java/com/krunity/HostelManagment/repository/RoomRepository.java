package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
public interface RoomRepository extends JpaRepository<Room, UUID> {
    int deleteByRoomId(UUID roomId);
    List<Room> findByHostel_HostelIdAndFloor_FloorId(UUID hostelId, UUID floorId);
    List<Room> findByHostel_HostelIdAndFloor_FloorIdAndIsActive(UUID hostelId, UUID floorId,Boolean isActive);
    Boolean existsByRoomNumberAndFloor_FloorId(String roomNumber, UUID floorId);
}
