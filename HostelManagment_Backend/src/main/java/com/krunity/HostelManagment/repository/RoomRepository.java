package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.RoomType;
import com.krunity.HostelManagment.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.List;
public interface RoomRepository extends JpaRepository<Room, UUID> {
    int deleteByRoomId(UUID roomId);
    List<Room> findByHostel_HostelIdAndFloor_FloorId(UUID hostelId, UUID floorId);
    List<Room> findByHostel_HostelIdAndFloor_FloorIdAndIsActive(UUID hostelId, UUID floorId,Boolean isActive);
    List<Room> findByHostel_HostelId(UUID hostelId);
    Boolean existsByRoomNumberAndFloor_FloorId(String roomNumber, UUID floorId);

    /**
     * Returns active rooms where room_type = 'PG_ROOM' OR room_type IS NULL (backward compat).
     */
    @Query("SELECT r FROM Room r WHERE r.hostel.hostelId = :hostelId AND r.floor.floorId = :floorId AND r.isActive = true AND (r.roomType = :roomType OR r.roomType IS NULL)")
    List<Room> findActiveRoomsByHostelAndFloorAndRoomTypeOrNull(
            @Param("hostelId") UUID hostelId,
            @Param("floorId") UUID floorId,
            @Param("roomType") RoomType roomType);

    /**
     * Returns active rooms where room_type = :roomType (exact match, no NULL fallback).
     */
    @Query("SELECT r FROM Room r WHERE r.hostel.hostelId = :hostelId AND r.floor.floorId = :floorId AND r.isActive = true AND r.roomType = :roomType")
    List<Room> findActiveRoomsByHostelAndFloorAndRoomType(
            @Param("hostelId") UUID hostelId,
            @Param("floorId") UUID floorId,
            @Param("roomType") RoomType roomType);
}
