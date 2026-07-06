package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.dto.RoomAvailabilityResponse;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.krunity.HostelManagment.model.Room;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RoomAvailabilityRepository extends JpaRepository<Room, UUID> {

    @Query("""
            SELECT new com.krunity.HostelManagment.dto.RoomAvailabilityResponse(
                r.roomId,
                r.roomNumber,
                r.totalBeds,
                COUNT(ra)
            )
            FROM Room r
            LEFT JOIN RoomAllotment ra ON ra.room = r
                AND ra.startDate <= :endDate
                AND (ra.endDate IS NULL OR ra.endDate >= :startDate)
                AND ra.roomAllotmentStatus IN :occupyingStatuses
            WHERE r.floor.floorId = :floorId
                AND r.isActive = true
                AND (:roomType IS NULL OR r.roomType = :roomType OR (:roomType = com.krunity.HostelManagment.enums.RoomType.PG_ROOM AND r.roomType IS NULL))
            GROUP BY r.roomId, r.roomNumber, r.totalBeds
            HAVING r.totalBeds - COUNT(ra) > 0
            ORDER BY r.roomNumber
            """)
    List<RoomAvailabilityResponse> findAvailableRoomsByFloor(
            @Param("floorId") UUID floorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("occupyingStatuses") Collection<RoomAllotmentStatus> occupyingStatuses,
            @Param("roomType") RoomType roomType);

    @Query("""
            SELECT COUNT(ra)
            FROM RoomAllotment ra
            WHERE ra.room.roomId = :roomId
                AND ra.startDate <= :endDate
                AND (ra.endDate IS NULL OR ra.endDate >= :startDate)
                AND ra.roomAllotmentStatus IN :occupyingStatuses
            """)
    long countOccupiedBeds(
            @Param("roomId") UUID roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("occupyingStatuses") Collection<RoomAllotmentStatus> occupyingStatuses);
}
