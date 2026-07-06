package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.model.Room;
import com.krunity.HostelManagment.model.RoomAllotment;
import com.krunity.HostelManagment.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
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

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE room_allotments
            SET room_allotment_status = 'LEFT'
            WHERE tenant_id = :tenantId
              AND room_id = :roomId
              AND agreement_id = :agreementId
            """,
            nativeQuery = true)
    int markAllotmentLeft(
            @Param("tenantId") UUID tenantId,
            @Param("roomId") UUID roomId,
            @Param("agreementId") String agreementId);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE room_allotments
            SET end_date = :endDate
            WHERE tenant_id = :tenantId
              AND agreement_id = :agreementId
              AND room_allotment_status IN ('ACTIVE', 'UPCOMING')
            """,
            nativeQuery = true)
    int updateEndDateByTenantAndAgreement(
            @Param("tenantId") UUID tenantId,
            @Param("agreementId") String agreementId,
            @Param("endDate") LocalDate endDate);

    Optional<RoomAllotment> findByTenant_UserIdAndAgreementIdAndRoomAllotmentStatusIn(
            UUID tenantId,
            String agreementId,
            Collection<RoomAllotmentStatus> statuses);

    List<RoomAllotment> findByRoomAndRoomAllotmentStatusIn(
            Room room,
            Collection<RoomAllotmentStatus> statuses);

    List<RoomAllotment> findByRoom(Room room);

    Optional<RoomAllotment> findByTenant_UserIdAndRoomAllotmentStatus(UUID tenantId, RoomAllotmentStatus status);

    List<RoomAllotment> findByTenant_UserIdAndRoomAllotmentStatusIn(UUID tenantId, Collection<RoomAllotmentStatus> statuses);

    List<RoomAllotment> findByRoom_Hostel_Owner_UserIdAndRoomAllotmentStatus(UUID ownerId, RoomAllotmentStatus status);

    List<RoomAllotment> findByRoom_Hostel_Owner_UserIdAndRoomAllotmentStatusIn(UUID ownerId, Collection<RoomAllotmentStatus> statuses);

    // Fixed: Use roomAllotmentStatus instead of active
    List<RoomAllotment> findByRoomAndRoomAllotmentStatus(Room room, RoomAllotmentStatus status);

    // Fixed: Use roomAllotmentStatus instead of active
    List<RoomAllotment> findByTenantAndRoomAllotmentStatus(User tenant, RoomAllotmentStatus status);

    // For cron job: fetch all allotments in specified statuses that have a notice period and end date set
    @Query("""
        SELECT a FROM RoomAllotment a
        WHERE a.roomAllotmentStatus IN :statuses
          AND a.endDate IS NOT NULL
          AND a.noticePeriodMonths IS NOT NULL
        """)
    List<RoomAllotment> findEligibleForSettlementPendingCheck(
            @Param("statuses") Collection<RoomAllotmentStatus> statuses);

    // For settlement service: find an active/upcoming/settlement-pending allotment by tenant + agreement
    Optional<RoomAllotment> findByTenant_UserIdAndAgreementId(UUID tenantId, String agreementId);

    // For left confirmation: find by allotment ID and verify owner via room->hostel->owner chain
    @Query("""
        SELECT a FROM RoomAllotment a
        WHERE a.allotmentId = :allotmentId
          AND a.room.hostel.owner.userId = :ownerId
        """)
    Optional<RoomAllotment> findByAllotmentIdAndOwner(
            @Param("allotmentId") UUID allotmentId,
            @Param("ownerId") UUID ownerId);
}
