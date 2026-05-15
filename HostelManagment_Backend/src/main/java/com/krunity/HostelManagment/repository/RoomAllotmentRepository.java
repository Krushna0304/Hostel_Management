package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.model.Room;
import com.krunity.HostelManagment.model.RoomAllotment;
import com.krunity.HostelManagment.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomAllotmentRepository extends JpaRepository<RoomAllotment, UUID> {

    boolean existsByTenant_UserId(UUID userId);

    List<RoomAllotment> findByRoom(Room room);

    Optional<RoomAllotment> findByTenant_UserIdAndRoomAllotmentStatus(UUID tenantId, RoomAllotmentStatus status);

    List<RoomAllotment> findByRoom_Hostel_Owner_UserIdAndRoomAllotmentStatus(UUID ownerId, RoomAllotmentStatus status);

    // Fixed: Use roomAllotmentStatus instead of active
    List<RoomAllotment> findByRoomAndRoomAllotmentStatus(Room room, RoomAllotmentStatus status);

    // Fixed: Use roomAllotmentStatus instead of active
    List<RoomAllotment> findByTenantAndRoomAllotmentStatus(User tenant, RoomAllotmentStatus status);
}
