package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.model.Room;
import com.krunity.HostelManagment.model.RoomAllotment;
import com.krunity.HostelManagment.model.User;
import jakarta.transaction.Transactional;
import org.hibernate.annotations.processing.SQL;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomAllotmentRepository extends JpaRepository<RoomAllotment, UUID> {

    boolean existsByTenant_UserId(UUID userId);

    @Modifying
    @Transactional
    @Query(
            value = """
            UPDATE room_allotments
            SET room_allotment_status = 'LEFT'
            WHERE tenant_id = :tenantId and
            room_id = :roomId
            """,
            nativeQuery = true
    )
    int removeTenant(@Param("tenantId") UUID tenantId,@Param("roomId") UUID roomId);

    List<RoomAllotment> findByRoom(Room room);

    Optional<RoomAllotment> findByTenant_UserIdAndRoomAllotmentStatus(UUID tenantId, RoomAllotmentStatus status);

    List<RoomAllotment> findByRoom_Hostel_Owner_UserIdAndRoomAllotmentStatus(UUID ownerId, RoomAllotmentStatus status);

    // Fixed: Use roomAllotmentStatus instead of active
    List<RoomAllotment> findByRoomAndRoomAllotmentStatus(Room room, RoomAllotmentStatus status);

    // Fixed: Use roomAllotmentStatus instead of active
    List<RoomAllotment> findByTenantAndRoomAllotmentStatus(User tenant, RoomAllotmentStatus status);
}
