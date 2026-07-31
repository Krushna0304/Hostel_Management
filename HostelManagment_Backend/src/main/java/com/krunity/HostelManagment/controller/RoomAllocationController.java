package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.service.RoomOverbookingManager;
import com.krunity.HostelManagment.service.RoomService;
import com.krunity.HostelManagment.service.UserService;
import com.krunity.HostelManagment.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Room Allocation API endpoints
 * Implements Task 5.2 - Room Allocation API endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomAllocationController {

    @Autowired
    private RoomOverbookingManager roomOverbookingManager;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService;

    /**
     * Task 5.2.1: GET /api/v1/rooms/{id}/availability
     * Get room availability information with pending action counts
     */
    @GetMapping("/{id}/availability")
    public ResponseEntity<?> getRoomAvailability(@PathVariable("id") UUID roomId) {
        try {
            log.info("Getting availability for room: {}", roomId);
            
            Map<String, Object> availability = roomOverbookingManager.getRoomAvailabilityDisplay(roomId);
            
            return ResponseEntity.ok(availability);
            
        } catch (IllegalArgumentException e) {
            log.warn("Room not found: {}", roomId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting room availability for room {}: {}", roomId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get room availability: " + e.getMessage()));
        }
    }

    /**
     * Task 5.2.2: POST /api/v1/rooms/{id}/allocate
     * Allocate room to tenant with overbooking support
     */
    @PostMapping("/{id}/allocate")
    public ResponseEntity<?> allocateRoom(
            @PathVariable("id") UUID roomId,
            @RequestBody Map<String, Object> allocationRequest) {
        
        try {
            String agreementId = (String) allocationRequest.get("agreementId");
            String tenantId = (String) allocationRequest.get("tenantId");
            
            if (agreementId == null || tenantId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "agreementId and tenantId are required"));
            }
            
            log.info("Allocating room {} to tenant {} with agreement {}", roomId, tenantId, agreementId);
            
            // Get tenant user
            User tenant = userService.getUserById(UUID.fromString(tenantId));
            if (tenant == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Tenant not found"));
            }
            
            // Perform allocation
            RoomOverbookingManager.RoomAllocationResult result = 
                    roomOverbookingManager.allocateRoom(roomId, agreementId, tenant);
            
            if (result.isSuccessful()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", result.getMessage(),
                        "allocationOrder", result.getAllocationOrder(),
                        "overbookingDetected", result.isOverbookingDetected(),
                        "availabilityInfo", result.getAvailabilityInfo()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", result.getMessage(),
                        "overbookingDetected", result.isOverbookingDetected(),
                        "availabilityInfo", result.getAvailabilityInfo()
                ));
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for room allocation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error allocating room {} to agreement {}: {}", roomId, 
                     allocationRequest.get("agreementId"), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to allocate room: " + e.getMessage()));
        }
    }

    /**
     * Task 5.2.3: GET /api/v1/rooms/search/available
     * Search for available rooms in hostel sorted by pending actions
     */
    @GetMapping("/search/available")
    public ResponseEntity<?> searchAvailableRooms(@RequestParam UUID hostelId) {
        try {
            log.info("Searching available rooms for hostel: {}", hostelId);
            
            List<Map<String, Object>> availableRooms = 
                    roomOverbookingManager.getRoomsSortedByPendingActions(hostelId);
            
            return ResponseEntity.ok(Map.of(
                    "hostelId", hostelId,
                    "rooms", availableRooms,
                    "totalRooms", availableRooms.size()
            ));
            
        } catch (Exception e) {
            log.error("Error searching available rooms for hostel {}: {}", hostelId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to search available rooms: " + e.getMessage()));
        }
    }

    /**
     * Task 5.2.4: Room dropdown with pending action counts
     * Get simplified room list for dropdown with pending action counts
     */
    @GetMapping("/dropdown")
    public ResponseEntity<?> getRoomDropdownData(@RequestParam UUID hostelId) {
        try {
            log.info("Getting room dropdown data for hostel: {}", hostelId);
            
            List<Map<String, Object>> rooms = 
                    roomOverbookingManager.getRoomsSortedByPendingActions(hostelId);
            
            // Transform to dropdown format
            List<Map<String, Object>> dropdownData = rooms.stream()
                    .map(room -> Map.of(
                            "roomId", room.get("roomId"),
                            "roomNumber", room.get("roomNumber"),
                            "totalBeds", room.get("totalBedCapacity"),
                            "available", room.get("actualAvailableBeds"),
                            "pendingActions", room.get("tenantActionPendingCount"),
                            "status", room.get("allocationStatus"),
                            "displayText", room.get("roomNumber") + " (" + 
                                    room.get("actualAvailableBeds") + "/" + 
                                    room.get("totalBedCapacity") + " beds, " +
                                    room.get("tenantActionPendingCount") + " pending)"
                    ))
                    .toList();
            
            return ResponseEntity.ok(Map.of(
                    "hostelId", hostelId,
                    "rooms", dropdownData
            ));
            
        } catch (Exception e) {
            log.error("Error getting room dropdown data for hostel {}: {}", hostelId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get room dropdown data: " + e.getMessage()));
        }
    }

    /**
     * Task 5.2.5: Additional endpoint for sorting by TenantActionPending count
     * Get rooms sorted specifically by pending action count (already implemented in search endpoint)
     */
    @GetMapping("/sorted/by-pending-actions")
    public ResponseEntity<?> getRoomsSortedByPendingActions(@RequestParam UUID hostelId) {
        try {
            log.info("Getting rooms sorted by pending actions for hostel: {}", hostelId);
            
            List<Map<String, Object>> sortedRooms = 
                    roomOverbookingManager.getRoomsSortedByPendingActions(hostelId);
            
            return ResponseEntity.ok(Map.of(
                    "hostelId", hostelId,
                    "rooms", sortedRooms,
                    "sortedBy", "tenantActionPendingCount",
                    "sortOrder", "ascending"
            ));
            
        } catch (Exception e) {
            log.error("Error getting rooms sorted by pending actions for hostel {}: {}", 
                     hostelId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get sorted rooms: " + e.getMessage()));
        }
    }
}