package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import com.krunity.HostelManagment.repository.RoomOverbookingLogRepository;
import com.krunity.HostelManagment.repository.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Service for managing room allocation with overbooking support
 * Implements first-come-first-served allocation when overbooking occurs
 */
@Slf4j
@Service
public class RoomOverbookingManager {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomAllotmentRepository roomAllotmentRepository;

    @Autowired
    private RoomOverbookingLogRepository overbookingLogRepository;

    // Concurrent access control per room
    private final ConcurrentHashMap<UUID, ReentrantLock> roomLocks = new ConcurrentHashMap<>();

    /**
     * Result object for room allocation operations
     */
    public static class RoomAllocationResult {
        private final boolean successful;
        private final String message;
        private final Integer allocationOrder;
        private final boolean overbookingDetected;
        private final RoomAvailabilityInfo availabilityInfo;

        private RoomAllocationResult(boolean successful, String message, Integer allocationOrder, 
                                   boolean overbookingDetected, RoomAvailabilityInfo availabilityInfo) {
            this.successful = successful;
            this.message = message;
            this.allocationOrder = allocationOrder;
            this.overbookingDetected = overbookingDetected;
            this.availabilityInfo = availabilityInfo;
        }

        public static RoomAllocationResult success(String message, Integer allocationOrder, 
                                                 RoomAvailabilityInfo availabilityInfo) {
            return new RoomAllocationResult(true, message, allocationOrder, false, availabilityInfo);
        }

        public static RoomAllocationResult failure(String message, boolean overbookingDetected, 
                                                 RoomAvailabilityInfo availabilityInfo) {
            return new RoomAllocationResult(false, message, null, overbookingDetected, availabilityInfo);
        }

        // Getters
        public boolean isSuccessful() { return successful; }
        public String getMessage() { return message; }
        public Integer getAllocationOrder() { return allocationOrder; }
        public boolean isOverbookingDetected() { return overbookingDetected; }
        public RoomAvailabilityInfo getAvailabilityInfo() { return availabilityInfo; }
    }

    /**
     * Room availability information
     */
    public static class RoomAvailabilityInfo {
        private final UUID roomId;
        private final String roomNumber;
        private final Integer totalBedCapacity;
        private final Integer activeAllotments;
        private final Integer pendingActionCount;
        private final Integer actualAvailableBeds;
        private final boolean overbookingScenario;
        private final String allocationStatus;

        public RoomAvailabilityInfo(UUID roomId, String roomNumber, Integer totalBedCapacity,
                                  Integer activeAllotments, Integer pendingActionCount,
                                  Integer actualAvailableBeds, boolean overbookingScenario,
                                  String allocationStatus) {
            this.roomId = roomId;
            this.roomNumber = roomNumber;
            this.totalBedCapacity = totalBedCapacity;
            this.activeAllotments = activeAllotments;
            this.pendingActionCount = pendingActionCount;
            this.actualAvailableBeds = actualAvailableBeds;
            this.overbookingScenario = overbookingScenario;
            this.allocationStatus = allocationStatus;
        }

        // Getters
        public UUID getRoomId() { return roomId; }
        public String getRoomNumber() { return roomNumber; }
        public Integer getTotalBedCapacity() { return totalBedCapacity; }
        public Integer getActiveAllotments() { return activeAllotments; }
        public Integer getPendingActionCount() { return pendingActionCount; }
        public Integer getActualAvailableBeds() { return actualAvailableBeds; }
        public boolean isOverbookingScenario() { return overbookingScenario; }
        public String getAllocationStatus() { return allocationStatus; }
    }

    /**
     * Handles room allocation with overbooking support
     * Implements first-come-first-served allocation when capacity is exceeded
     */
    @Transactional
    public RoomAllocationResult allocateRoom(UUID roomId, String agreementId, User tenant) {
        log.info("Attempting room allocation for room {} with agreement {} for tenant {}", 
                roomId, agreementId, tenant.getUserId());

        // Get room-specific lock for concurrent safety
        ReentrantLock roomLock = roomLocks.computeIfAbsent(roomId, k -> new ReentrantLock());
        
        roomLock.lock();
        try {
            // Get room and verify it exists
            Optional<Room> roomOptional = roomRepository.findById(roomId);
            if (roomOptional.isEmpty()) {
                log.warn("Room not found for ID: {}", roomId);
                return RoomAllocationResult.failure("Room not found", false, null);
            }

            Room room = roomOptional.get();

            // Calculate current availability
            RoomAvailabilityInfo availabilityInfo = calculateRoomAvailability(room);

            // Log the allocation attempt
            RoomOverbookingLog allocationLog = RoomOverbookingLog.forAllocationAttempt(room, agreementId, tenant)
                    .eventType(RoomOverbookingLog.EventType.AGREEMENT_CREATED)
                    .agreementsCount(availabilityInfo.getActiveAllotments() + 1) // Including new agreement
                    .pendingActionCount(availabilityInfo.getPendingActionCount())
                    .availableBeds(availabilityInfo.getActualAvailableBeds())
                    .build();

            // Check if allocation is possible (first-come-first-served)
            boolean canAllocate = availabilityInfo.getActualAvailableBeds() > 0 || 
                                 !isStrictCapacityEnforced(room);

            if (canAllocate) {
                // Successful allocation
                Integer nextAllocationOrder = overbookingLogRepository.findMaxAllocationOrderForRoom(room) + 1;
                
                allocationLog.setEventType(RoomOverbookingLog.EventType.ALLOCATION_SUCCESSFUL);
                allocationLog.setAllocationOrder(nextAllocationOrder);
                allocationLog.setAllocationSuccessful(true);
                
                overbookingLogRepository.save(allocationLog);

                // Check if this creates an overbooking scenario
                boolean isOverbooking = (availabilityInfo.getActiveAllotments() + 1) > room.getTotalBeds();
                if (isOverbooking) {
                    logOverbookingDetection(room, availabilityInfo.getActiveAllotments() + 1);
                }

                log.info("Room allocation successful for room {} with agreement {} (allocation order: {})", 
                        roomId, agreementId, nextAllocationOrder);

                return RoomAllocationResult.success(
                        "Room allocated successfully" + (isOverbooking ? " (overbooking scenario)" : ""),
                        nextAllocationOrder,
                        availabilityInfo
                );

            } else {
                // Allocation failed - room not available
                allocationLog.setEventType(RoomOverbookingLog.EventType.ALLOCATION_FAILED);
                allocationLog.setAllocationSuccessful(false);
                allocationLog.setFailureReason("Room capacity exceeded - no beds available");
                
                overbookingLogRepository.save(allocationLog);

                log.warn("Room allocation failed for room {} with agreement {} - capacity exceeded", 
                        roomId, agreementId);

                return RoomAllocationResult.failure(
                        "Room not available - capacity exceeded",
                        true,
                        availabilityInfo
                );
            }

        } catch (Exception e) {
            log.error("Error during room allocation for room {} with agreement {}: {}", 
                     roomId, agreementId, e.getMessage(), e);
            throw new RuntimeException("Room allocation failed: " + e.getMessage(), e);
        } finally {
            roomLock.unlock();
        }
    }

    /**
     * Calculates room availability with pending actions consideration
     */
    public RoomAvailabilityInfo calculateRoomAvailability(Room room) {
        return calculateRoomAvailability(room.getRoomId());
    }

    /**
     * Calculates room availability by room ID
     */
    public RoomAvailabilityInfo calculateRoomAvailability(UUID roomId) {
        Optional<Room> roomOptional = roomRepository.findById(roomId);
        if (roomOptional.isEmpty()) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }

        Room room = roomOptional.get();

        // Get active allotments (UPCOMING, ACTIVE)
        Collection<RoomAllotmentStatus> activeStatuses = Arrays.asList(
                RoomAllotmentStatus.UPCOMING, 
                RoomAllotmentStatus.ACTIVE
        );
        List<RoomAllotment> activeAllotments = roomAllotmentRepository.findActiveAllotmentsByRoom(room, activeStatuses);

        // Count agreements with "TenantActionPending" status  
        Collection<RoomAllotmentStatus> pendingStatuses = Arrays.asList(
                RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING
        );
        List<RoomAllotment> pendingAllotments = roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(room, pendingStatuses);

        Integer totalBeds = room.getTotalBeds();
        Integer activeCount = activeAllotments.size();
        Integer pendingCount = pendingAllotments.size();
        Integer actualAvailable = Math.max(0, totalBeds - activeCount);

        boolean isOverbooking = activeCount > totalBeds;
        String status = determineAllocationStatus(totalBeds, activeCount, pendingCount);

        return new RoomAvailabilityInfo(
                room.getRoomId(),
                room.getRoomNumber(),
                totalBeds,
                activeCount,
                pendingCount,
                actualAvailable,
                isOverbooking,
                status
        );
    }

    /**
     * Processes room allocation failures due to overbooking
     */
    public void handleAllocationFailure(UUID roomId, String agreementId, String reason) {
        log.info("Handling allocation failure for room {} with agreement {}: {}", roomId, agreementId, reason);

        try {
            Optional<Room> roomOptional = roomRepository.findById(roomId);
            if (roomOptional.isEmpty()) {
                log.warn("Cannot handle allocation failure - room not found: {}", roomId);
                return;
            }

            Room room = roomOptional.get();
            RoomAvailabilityInfo availabilityInfo = calculateRoomAvailability(room);

            // Log the failure
            RoomOverbookingLog failureLog = RoomOverbookingLog.forAllocationAttempt(room, agreementId, null)
                    .eventType(RoomOverbookingLog.EventType.ALLOCATION_FAILED)
                    .agreementsCount(availabilityInfo.getActiveAllotments())
                    .pendingActionCount(availabilityInfo.getPendingActionCount())
                    .availableBeds(availabilityInfo.getActualAvailableBeds())
                    .allocationSuccessful(false)
                    .failureReason(reason)
                    .build();

            overbookingLogRepository.save(failureLog);

            // Suggest alternative rooms if available
            List<Room> alternativeRooms = findAlternativeRooms(room);
            if (!alternativeRooms.isEmpty()) {
                log.info("Found {} alternative rooms for failed allocation of agreement {}", 
                        alternativeRooms.size(), agreementId);
                // In a real implementation, this could trigger notifications to tenant/owner
            }

        } catch (Exception e) {
            log.error("Error handling allocation failure for room {} with agreement {}: {}", 
                     roomId, agreementId, e.getMessage(), e);
        }
    }

    /**
     * Get room availability for display with pending action counts
     */
    public Map<String, Object> getRoomAvailabilityDisplay(UUID roomId) {
        RoomAvailabilityInfo info = calculateRoomAvailability(roomId);

        Map<String, Object> display = new HashMap<>();
        display.put("roomId", info.getRoomId());
        display.put("roomNumber", info.getRoomNumber());
        display.put("totalBedCapacity", info.getTotalBedCapacity());
        display.put("currentOccupancy", info.getActiveAllotments());
        display.put("tenantActionPendingCount", info.getPendingActionCount());
        display.put("actualAvailableBeds", info.getActualAvailableBeds());
        display.put("overbookingDetected", info.isOverbookingScenario());
        display.put("allocationStatus", info.getAllocationStatus());

        // Add upcoming departures information
        List<Map<String, Object>> upcomingDepartures = getUpcomingDepartures(roomId);
        display.put("upcomingDepartures", upcomingDepartures);

        return display;
    }

    /**
     * Get rooms sorted by TenantActionPending count (ascending order)
     */
    public List<Map<String, Object>> getRoomsSortedByPendingActions(UUID hostelId) {
        log.info("Getting rooms sorted by pending actions for hostel: {}", hostelId);
        
        try {
            // Get all active rooms for the hostel
            List<Room> hostelRooms = roomRepository.findByHostel_HostelIdAndIsActive(hostelId, true);
            
            // Calculate availability info for each room and sort by pending action count
            return hostelRooms.stream()
                    .map(room -> {
                        try {
                            // Get active allotments (UPCOMING, ACTIVE)
                            Collection<RoomAllotmentStatus> activeStatuses = Arrays.asList(
                                    RoomAllotmentStatus.UPCOMING, 
                                    RoomAllotmentStatus.ACTIVE
                            );
                            List<RoomAllotment> activeAllotments = roomAllotmentRepository.findActiveAllotmentsByRoom(room, activeStatuses);

                            // Count agreements with "TenantActionPending" status  
                            Collection<RoomAllotmentStatus> pendingStatuses = Arrays.asList(
                                    RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING
                            );
                            List<RoomAllotment> pendingAllotments = roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(room, pendingStatuses);

                            Integer totalBeds = room.getTotalBeds();
                            Integer activeCount = activeAllotments.size();
                            Integer pendingCount = pendingAllotments.size();
                            Integer actualAvailable = Math.max(0, totalBeds - activeCount);

                            boolean isOverbooking = activeCount > totalBeds;
                            String status = determineAllocationStatus(totalBeds, activeCount, pendingCount);
                            
                            Map<String, Object> roomInfo = new HashMap<>();
                            roomInfo.put("roomId", room.getRoomId());
                            roomInfo.put("roomNumber", room.getRoomNumber());
                            roomInfo.put("totalBedCapacity", totalBeds);
                            roomInfo.put("currentOccupancy", activeCount);
                            roomInfo.put("tenantActionPendingCount", pendingCount);
                            roomInfo.put("actualAvailableBeds", actualAvailable);
                            roomInfo.put("overbookingDetected", isOverbooking);
                            roomInfo.put("allocationStatus", status);
                            
                            return roomInfo;
                        } catch (Exception e) {
                            log.warn("Error calculating availability for room {}: {}", room.getRoomId(), e.getMessage());
                            // Return default info for problematic rooms
                            Map<String, Object> roomInfo = new HashMap<>();
                            roomInfo.put("roomId", room.getRoomId());
                            roomInfo.put("roomNumber", room.getRoomNumber());
                            roomInfo.put("totalBedCapacity", room.getTotalBeds());
                            roomInfo.put("currentOccupancy", 0);
                            roomInfo.put("tenantActionPendingCount", 0);
                            roomInfo.put("actualAvailableBeds", room.getTotalBeds());
                            roomInfo.put("overbookingDetected", false);
                            roomInfo.put("allocationStatus", "AVAILABLE");
                            return roomInfo;
                        }
                    })
                    .sorted((room1, room2) -> {
                        // Sort by TenantActionPending count in ascending order (lowest first)
                        Integer pending1 = (Integer) room1.get("tenantActionPendingCount");
                        Integer pending2 = (Integer) room2.get("tenantActionPendingCount");
                        return pending1.compareTo(pending2);
                    })
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting rooms sorted by pending actions for hostel {}: {}", hostelId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // Helper Methods

    private void logOverbookingDetection(Room room, Integer agreementCount) {
        RoomOverbookingLog overbookingLog = RoomOverbookingLog.forOverbookingDetection(room)
                .agreementsCount(agreementCount)
                .pendingActionCount(0)
                .availableBeds(Math.max(0, room.getTotalBeds() - agreementCount))
                .build();

        overbookingLogRepository.save(overbookingLog);
        
        log.warn("Overbooking detected in room {} ({}) - {} agreements for {} bed capacity", 
                room.getRoomId(), room.getRoomNumber(), agreementCount, room.getTotalBeds());
    }

    private boolean isStrictCapacityEnforced(Room room) {
        // For this implementation, we allow overbooking but track it
        // In a stricter system, this could return true to enforce hard capacity limits
        return false;
    }

    private String determineAllocationStatus(Integer totalBeds, Integer activeCount, Integer pendingCount) {
        if (activeCount >= totalBeds) {
            return pendingCount > 0 ? "OVERBOOKED_WITH_PENDING" : "FULLY_OCCUPIED";
        } else if (activeCount + pendingCount >= totalBeds) {
            return "AVAILABLE_PENDING_LIMITED";
        } else {
            return "AVAILABLE";
        }
    }

    private List<Room> findAlternativeRooms(Room originalRoom) {
        // Find rooms in same hostel with availability
        // This is a simplified implementation
        try {
            return roomRepository.findByHostelAndIsActive(originalRoom.getHostel(), true)
                    .stream()
                    .filter(room -> !room.getRoomId().equals(originalRoom.getRoomId()))
                    .filter(room -> {
                        RoomAvailabilityInfo info = calculateRoomAvailability(room);
                        return info.getActualAvailableBeds() > 0;
                    })
                    .limit(3) // Maximum 3 alternatives
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error finding alternative rooms: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getUpcomingDepartures(UUID roomId) {
        // Get allotments ending soon
        // This is a placeholder implementation
        return new ArrayList<>();
    }
}