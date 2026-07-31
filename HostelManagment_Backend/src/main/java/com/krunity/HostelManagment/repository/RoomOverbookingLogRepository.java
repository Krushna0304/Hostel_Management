package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.Room;
import com.krunity.HostelManagment.model.RoomOverbookingLog;
import com.krunity.HostelManagment.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RoomOverbookingLog operations
 */
@Repository
public interface RoomOverbookingLogRepository extends JpaRepository<RoomOverbookingLog, UUID> {

    /**
     * Find overbooking events by room and event type
     */
    List<RoomOverbookingLog> findByRoomAndEventTypeOrderByEventTimestampDesc(
            Room room, RoomOverbookingLog.EventType eventType);

    /**
     * Find overbooking events by agreement ID
     */
    List<RoomOverbookingLog> findByAgreementIdOrderByEventTimestampDesc(String agreementId);

    /**
     * Find recent overbooking events for a room
     */
    @Query("""
        SELECT log FROM RoomOverbookingLog log
        WHERE log.room = :room
          AND log.eventTimestamp >= :since
        ORDER BY log.eventTimestamp DESC
        """)
    List<RoomOverbookingLog> findRecentEventsByRoom(@Param("room") Room room, 
                                                   @Param("since") LocalDateTime since);

    /**
     * Find all overbooking scenarios (where agreements > capacity)
     */
    @Query("""
        SELECT log FROM RoomOverbookingLog log
        WHERE log.agreementsCount > log.totalBedCapacity
        ORDER BY log.eventTimestamp DESC
        """)
    List<RoomOverbookingLog> findAllOverbookingScenarios();

    /**
     * Find overbooking scenarios for a specific room
     */
    @Query("""
        SELECT log FROM RoomOverbookingLog log
        WHERE log.room = :room
          AND log.agreementsCount > log.totalBedCapacity
        ORDER BY log.eventTimestamp DESC
        """)
    List<RoomOverbookingLog> findOverbookingScenariosForRoom(@Param("room") Room room);

    /**
     * Get the last allocation attempt for a room
     */
    Optional<RoomOverbookingLog> findFirstByRoomAndEventTypeInOrderByEventTimestampDesc(
            Room room, List<RoomOverbookingLog.EventType> eventTypes);

    /**
     * Find failed allocations for a tenant
     */
    List<RoomOverbookingLog> findByTenantAndAllocationSuccessfulOrderByEventTimestampDesc(
            User tenant, Boolean allocationSuccessful);

    /**
     * Get allocation statistics for a room
     */
    @Query("""
        SELECT 
            COUNT(*) as totalAttempts,
            SUM(CASE WHEN log.allocationSuccessful = true THEN 1 ELSE 0 END) as successfulAllocations,
            SUM(CASE WHEN log.allocationSuccessful = false THEN 1 ELSE 0 END) as failedAllocations
        FROM RoomOverbookingLog log
        WHERE log.room = :room
          AND log.eventType IN ('ALLOCATION_SUCCESSFUL', 'ALLOCATION_FAILED')
        """)
    Object[] getAllocationStatisticsForRoom(@Param("room") Room room);

    /**
     * Find events by room within a date range
     */
    @Query("""
        SELECT log FROM RoomOverbookingLog log
        WHERE log.room = :room
          AND log.eventTimestamp BETWEEN :startTime AND :endTime
        ORDER BY log.eventTimestamp DESC
        """)
    List<RoomOverbookingLog> findByRoomAndEventTimestampBetween(
            @Param("room") Room room,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count recent failed allocations for a room
     */
    @Query("""
        SELECT COUNT(log) FROM RoomOverbookingLog log
        WHERE log.room = :room
          AND log.eventType = 'ALLOCATION_FAILED'
          AND log.eventTimestamp >= :since
        """)
    Long countRecentFailedAllocations(@Param("room") Room room, @Param("since") LocalDateTime since);

    /**
     * Find the highest allocation order for a room (for first-come-first-served)
     */
    @Query("""
        SELECT COALESCE(MAX(log.allocationOrder), 0) FROM RoomOverbookingLog log
        WHERE log.room = :room
          AND log.eventType = 'ALLOCATION_SUCCESSFUL'
        """)
    Integer findMaxAllocationOrderForRoom(@Param("room") Room room);

    /**
     * Delete old logs beyond retention period
     */
    @Query("""
        DELETE FROM RoomOverbookingLog log
        WHERE log.eventTimestamp < :cutoffTime
        """)
    int deleteOldLogs(@Param("cutoffTime") LocalDateTime cutoffTime);
}