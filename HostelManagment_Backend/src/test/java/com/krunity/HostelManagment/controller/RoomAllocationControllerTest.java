package com.krunity.HostelManagment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.service.RoomOverbookingManager;
import com.krunity.HostelManagment.service.RoomService;
import com.krunity.HostelManagment.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for RoomAllocationController
 * Tests Task 5.2 - Room Allocation API endpoints
 */
@ExtendWith(MockitoExtension.class)
class RoomAllocationControllerTest {

    @Mock
    private RoomOverbookingManager roomOverbookingManager;

    @Mock
    private RoomService roomService;

    @Mock
    private UserService userService;

    @InjectMocks
    private RoomAllocationController roomAllocationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID roomId;
    private UUID hostelId;
    private UUID tenantId;
    private String agreementId;
    private User testTenant;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomAllocationController).build();
        objectMapper = new ObjectMapper();
        
        roomId = UUID.randomUUID();
        hostelId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        agreementId = "agreement_123";
        
        testTenant = User.builder()
                .userId(tenantId)
                .displayName("Test Tenant")
                .build();
    }

    @Test
    void testGetRoomAvailability_Success() throws Exception {
        // Arrange
        Map<String, Object> availabilityData = createMockAvailabilityData();
        when(roomOverbookingManager.getRoomAvailabilityDisplay(roomId))
                .thenReturn(availabilityData);

        // Act & Assert
        mockMvc.perform(get("/api/v1/rooms/{id}/availability", roomId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(roomOverbookingManager, times(1)).getRoomAvailabilityDisplay(roomId);
    }

    @Test
    void testGetRoomAvailability_RoomNotFound() throws Exception {
        // Arrange
        when(roomOverbookingManager.getRoomAvailabilityDisplay(roomId))
                .thenThrow(new IllegalArgumentException("Room not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/rooms/{id}/availability", roomId))
                .andExpect(status().isNotFound());

        verify(roomOverbookingManager, times(1)).getRoomAvailabilityDisplay(roomId);
    }

    @Test
    void testAllocateRoom_Success() throws Exception {
        // Arrange
        Map<String, Object> request = Map.of(
                "agreementId", agreementId,
                "tenantId", tenantId.toString()
        );

        RoomOverbookingManager.RoomAvailabilityInfo availabilityInfo = 
                createMockAvailabilityInfo();
        
        RoomOverbookingManager.RoomAllocationResult successResult = 
                RoomOverbookingManager.RoomAllocationResult.success(
                        "Room allocated successfully", 
                        5, 
                        availabilityInfo
                );

        when(userService.getUserById(tenantId)).thenReturn(testTenant);
        when(roomOverbookingManager.allocateRoom(roomId, agreementId, testTenant))
                .thenReturn(successResult);

        // Act & Assert
        mockMvc.perform(post("/api/v1/rooms/{id}/allocate", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Room allocated successfully"))
                .andExpect(jsonPath("$.allocationOrder").value(5))
                .andExpect(jsonPath("$.overbookingDetected").value(false))
                .andExpect(jsonPath("$.availabilityInfo").exists());

        verify(userService, times(1)).getUserById(tenantId);
        verify(roomOverbookingManager, times(1)).allocateRoom(roomId, agreementId, testTenant);
    }

    @Test
    void testAllocateRoom_AllocationFailed() throws Exception {
        // Arrange
        Map<String, Object> request = Map.of(
                "agreementId", agreementId,
                "tenantId", tenantId.toString()
        );

        RoomOverbookingManager.RoomAvailabilityInfo availabilityInfo = 
                createMockAvailabilityInfo();
        
        RoomOverbookingManager.RoomAllocationResult failureResult = 
                RoomOverbookingManager.RoomAllocationResult.failure(
                        "Room not available - capacity exceeded", 
                        true, 
                        availabilityInfo
                );

        when(userService.getUserById(tenantId)).thenReturn(testTenant);
        when(roomOverbookingManager.allocateRoom(roomId, agreementId, testTenant))
                .thenReturn(failureResult);

        // Act & Assert
        mockMvc.perform(post("/api/v1/rooms/{id}/allocate", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Room not available - capacity exceeded"))
                .andExpect(jsonPath("$.overbookingDetected").value(true));

        verify(userService, times(1)).getUserById(tenantId);
        verify(roomOverbookingManager, times(1)).allocateRoom(roomId, agreementId, testTenant);
    }

    @Test
    void testAllocateRoom_MissingParameters() throws Exception {
        // Arrange
        Map<String, Object> request = Map.of("agreementId", agreementId);
        // Missing tenantId

        // Act & Assert
        mockMvc.perform(post("/api/v1/rooms/{id}/allocate", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("agreementId and tenantId are required"));

        verify(userService, never()).getUserById(any());
        verify(roomOverbookingManager, never()).allocateRoom(any(), any(), any());
    }

    @Test
    void testAllocateRoom_TenantNotFound() throws Exception {
        // Arrange
        Map<String, Object> request = Map.of(
                "agreementId", agreementId,
                "tenantId", tenantId.toString()
        );

        when(userService.getUserById(tenantId)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/api/v1/rooms/{id}/allocate", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Tenant not found"));

        verify(userService, times(1)).getUserById(tenantId);
        verify(roomOverbookingManager, never()).allocateRoom(any(), any(), any());
    }

    @Test
    void testSearchAvailableRooms_Success() throws Exception {
        // Arrange
        List<Map<String, Object>> roomsList = createMockRoomsList();
        when(roomOverbookingManager.getRoomsSortedByPendingActions(hostelId))
                .thenReturn(roomsList);

        // Act & Assert
        mockMvc.perform(get("/api/v1/rooms/search/available")
                        .param("hostelId", hostelId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostelId").value(hostelId.toString()))
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms").isNotEmpty())
                .andExpect(jsonPath("$.totalRooms").value(roomsList.size()));

        verify(roomOverbookingManager, times(1)).getRoomsSortedByPendingActions(hostelId);
    }

    @Test
    void testGetRoomDropdownData_Success() throws Exception {
        // Arrange
        List<Map<String, Object>> roomsList = createMockRoomsList();
        when(roomOverbookingManager.getRoomsSortedByPendingActions(hostelId))
                .thenReturn(roomsList);

        // Act & Assert
        mockMvc.perform(get("/api/v1/rooms/dropdown")
                        .param("hostelId", hostelId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostelId").value(hostelId.toString()))
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms[0].roomId").exists())
                .andExpect(jsonPath("$.rooms[0].roomNumber").exists())
                .andExpect(jsonPath("$.rooms[0].displayText").exists());

        verify(roomOverbookingManager, times(1)).getRoomsSortedByPendingActions(hostelId);
    }

    @Test
    void testGetRoomsSortedByPendingActions_Success() throws Exception {
        // Arrange
        List<Map<String, Object>> sortedRooms = createMockRoomsList();
        when(roomOverbookingManager.getRoomsSortedByPendingActions(hostelId))
                .thenReturn(sortedRooms);

        // Act & Assert
        mockMvc.perform(get("/api/v1/rooms/sorted/by-pending-actions")
                        .param("hostelId", hostelId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostelId").value(hostelId.toString()))
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.sortedBy").value("tenantActionPendingCount"))
                .andExpect(jsonPath("$.sortOrder").value("ascending"));

        verify(roomOverbookingManager, times(1)).getRoomsSortedByPendingActions(hostelId);
    }

    @Test
    void testSearchAvailableRooms_InternalServerError() throws Exception {
        // Arrange
        when(roomOverbookingManager.getRoomsSortedByPendingActions(hostelId))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/rooms/search/available")
                        .param("hostelId", hostelId.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Failed to search available rooms")));

        verify(roomOverbookingManager, times(1)).getRoomsSortedByPendingActions(hostelId);
    }

    // Helper methods

    private Map<String, Object> createMockAvailabilityData() {
        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("roomNumber", "A-101");
        data.put("totalBedCapacity", 10);
        data.put("currentOccupancy", 7);
        data.put("tenantActionPendingCount", 2);
        data.put("actualAvailableBeds", 3);
        data.put("overbookingDetected", false);
        data.put("allocationStatus", "AVAILABLE");
        data.put("upcomingDepartures", new ArrayList<>());
        return data;
    }

    private RoomOverbookingManager.RoomAvailabilityInfo createMockAvailabilityInfo() {
        return new RoomOverbookingManager.RoomAvailabilityInfo(
                roomId,
                "A-101",
                10,
                7,
                2,
                3,
                false,
                "AVAILABLE"
        );
    }

    private List<Map<String, Object>> createMockRoomsList() {
        List<Map<String, Object>> rooms = new ArrayList<>();
        
        // Room 1
        Map<String, Object> room1 = new HashMap<>();
        room1.put("roomId", UUID.randomUUID());
        room1.put("roomNumber", "A-101");
        room1.put("totalBedCapacity", 10);
        room1.put("currentOccupancy", 7);
        room1.put("tenantActionPendingCount", 1);
        room1.put("actualAvailableBeds", 3);
        room1.put("overbookingDetected", false);
        room1.put("allocationStatus", "AVAILABLE");
        
        // Room 2
        Map<String, Object> room2 = new HashMap<>();
        room2.put("roomId", UUID.randomUUID());
        room2.put("roomNumber", "A-102");
        room2.put("totalBedCapacity", 8);
        room2.put("currentOccupancy", 6);
        room2.put("tenantActionPendingCount", 2);
        room2.put("actualAvailableBeds", 2);
        room2.put("overbookingDetected", false);
        room2.put("allocationStatus", "AVAILABLE_PENDING_LIMITED");
        
        rooms.add(room1);
        rooms.add(room2);
        
        return rooms;
    }
}