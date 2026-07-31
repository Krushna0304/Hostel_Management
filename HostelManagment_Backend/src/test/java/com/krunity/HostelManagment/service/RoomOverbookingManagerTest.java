package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import com.krunity.HostelManagment.repository.RoomOverbookingLogRepository;
import com.krunity.HostelManagment.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for RoomOverbookingManager
 */
@ExtendWith(MockitoExtension.class)
public class RoomOverbookingManagerTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomAllotmentRepository roomAllotmentRepository;

    @Mock
    private RoomOverbookingLogRepository overbookingLogRepository;

    @InjectMocks
    private RoomOverbookingManager roomOverbookingManager;

    private Room testRoom;
    private User testTenant;
    private UUID roomId;
    private String agreementId;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        agreementId = "agreement_123";

        testRoom = Room.builder()
                .roomId(roomId)
                .roomNumber("A-101")
                .totalBeds(10)
                .availableBeds(5)
                .isActive(true)
                .build();

        testTenant = User.builder()
                .userId(UUID.randomUUID())
                .displayName("Test Tenant")
                .build();
    }

    @Test
    void testCalculateRoomAvailability_WithinCapacity() {
        // Arrange
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        
        List<RoomAllotment> activeAllotments = createMockAllotments(5, RoomAllotmentStatus.ACTIVE);
        List<RoomAllotment> pendingAllotments = createMockAllotments(2, RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING);
        
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(activeAllotments);
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(pendingAllotments);

        // Act
        RoomOverbookingManager.RoomAvailabilityInfo result = roomOverbookingManager.calculateRoomAvailability(roomId);

        // Assert
        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        assertEquals("A-101", result.getRoomNumber());
        assertEquals(10, result.getTotalBedCapacity());
        assertEquals(5, result.getActiveAllotments());
        assertEquals(2, result.getPendingActionCount());
        assertEquals(5, result.getActualAvailableBeds()); // 10 - 5 = 5
        assertFalse(result.isOverbookingScenario());
        assertEquals("AVAILABLE", result.getAllocationStatus());
    }

    @Test
    void testCalculateRoomAvailability_OverbookingScenario() {
        // Arrange
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        
        List<RoomAllotment> activeAllotments = createMockAllotments(15, RoomAllotmentStatus.ACTIVE); // More than capacity
        List<RoomAllotment> pendingAllotments = createMockAllotments(3, RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING);
        
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(activeAllotments);
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(pendingAllotments);

        // Act
        RoomOverbookingManager.RoomAvailabilityInfo result = roomOverbookingManager.calculateRoomAvailability(roomId);

        // Assert
        assertNotNull(result);
        assertEquals(15, result.getActiveAllotments());
        assertEquals(0, result.getActualAvailableBeds()); // Max(0, 10-15) = 0
        assertTrue(result.isOverbookingScenario());
        assertEquals("OVERBOOKED_WITH_PENDING", result.getAllocationStatus());
    }

    @Test
    void testAllocateRoom_Successful() {
        // Arrange
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        
        List<RoomAllotment> activeAllotments = createMockAllotments(8, RoomAllotmentStatus.ACTIVE);
        List<RoomAllotment> pendingAllotments = new ArrayList<>();
        
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(activeAllotments);
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(pendingAllotments);
        when(overbookingLogRepository.findMaxAllocationOrderForRoom(testRoom)).thenReturn(5);
        when(overbookingLogRepository.save(any(RoomOverbookingLog.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        RoomOverbookingManager.RoomAllocationResult result = 
                roomOverbookingManager.allocateRoom(roomId, agreementId, testTenant);

        // Assert
        assertTrue(result.isSuccessful());
        assertEquals(6, result.getAllocationOrder()); // 5 + 1
        assertFalse(result.isOverbookingDetected());
        assertNotNull(result.getAvailabilityInfo());
        verify(overbookingLogRepository, times(1)).save(any(RoomOverbookingLog.class));
    }

    @Test
    void testAllocateRoom_OverbookingScenario() {
        // Arrange
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        
        List<RoomAllotment> activeAllotments = createMockAllotments(12, RoomAllotmentStatus.ACTIVE); // Already overbooked
        List<RoomAllotment> pendingAllotments = new ArrayList<>();
        
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(activeAllotments);
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(pendingAllotments);
        when(overbookingLogRepository.findMaxAllocationOrderForRoom(testRoom)).thenReturn(10);
        when(overbookingLogRepository.save(any(RoomOverbookingLog.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        RoomOverbookingManager.RoomAllocationResult result = 
                roomOverbookingManager.allocateRoom(roomId, agreementId, testTenant);

        // Assert - In current implementation, overbooking is allowed but tracked
        assertTrue(result.isSuccessful()); // Because isStrictCapacityEnforced returns false
        assertEquals(11, result.getAllocationOrder());
        verify(overbookingLogRepository, times(2)).save(any(RoomOverbookingLog.class)); // Allocation + overbooking detection
    }

    @Test
    void testAllocateRoom_RoomNotFound() {
        // Arrange
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // Act
        RoomOverbookingManager.RoomAllocationResult result = 
                roomOverbookingManager.allocateRoom(roomId, agreementId, testTenant);

        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("Room not found", result.getMessage());
        assertNull(result.getAllocationOrder());
        assertFalse(result.isOverbookingDetected());
    }

    @Test
    void testHandleAllocationFailure() {
        // Arrange
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        
        List<RoomAllotment> activeAllotments = createMockAllotments(10, RoomAllotmentStatus.ACTIVE);
        List<RoomAllotment> pendingAllotments = new ArrayList<>();
        
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(activeAllotments);
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(pendingAllotments);
        when(overbookingLogRepository.save(any(RoomOverbookingLog.class))).thenAnswer(i -> i.getArguments()[0]);

        String failureReason = "Room capacity exceeded";

        // Act
        roomOverbookingManager.handleAllocationFailure(roomId, agreementId, failureReason);

        // Assert
        verify(overbookingLogRepository, times(1)).save(argThat(log -> 
            log.getEventType() == RoomOverbookingLog.EventType.ALLOCATION_FAILED &&
            log.getAgreementId().equals(agreementId) &&
            log.getFailureReason().equals(failureReason) &&
            !log.getAllocationSuccessful()
        ));
    }

    @Test
    void testGetRoomAvailabilityDisplay() {
        // Arrange
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        
        List<RoomAllotment> activeAllotments = createMockAllotments(7, RoomAllotmentStatus.ACTIVE);
        List<RoomAllotment> pendingAllotments = createMockAllotments(3, RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING);
        
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(activeAllotments);
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(pendingAllotments);

        // Act
        Map<String, Object> display = roomOverbookingManager.getRoomAvailabilityDisplay(roomId);

        // Assert
        assertNotNull(display);
        assertEquals(roomId, display.get("roomId"));
        assertEquals("A-101", display.get("roomNumber"));
        assertEquals(10, display.get("totalBedCapacity"));
        assertEquals(7, display.get("currentOccupancy"));
        assertEquals(3, display.get("tenantActionPendingCount")); // Updated to 3
        assertEquals(3, display.get("actualAvailableBeds"));
        assertEquals(false, display.get("overbookingDetected"));
        assertEquals("AVAILABLE_PENDING_LIMITED", display.get("allocationStatus")); // Now 7+3=10, so this will be correct
        assertNotNull(display.get("upcomingDepartures"));
    }

    @Test
    void testGetRoomsSortedByPendingActions() {
        // Arrange
        UUID hostelId = UUID.randomUUID();
        
        // Create rooms with different pending action counts
        Room room1 = Room.builder()
                .roomId(UUID.randomUUID())
                .roomNumber("A-101")
                .totalBeds(10)
                .isActive(true)
                .build();
        
        Room room2 = Room.builder()
                .roomId(UUID.randomUUID())
                .roomNumber("A-102") 
                .totalBeds(8)
                .isActive(true)
                .build();
        
        Room room3 = Room.builder()
                .roomId(UUID.randomUUID())
                .roomNumber("A-103")
                .totalBeds(12)
                .isActive(true)
                .build();
        
        when(roomRepository.findByHostel_HostelIdAndIsActive(hostelId, true))
                .thenReturn(Arrays.asList(room1, room2, room3));
        
        // Mock different allotment scenarios for each room
        // Room1: 5 active, 3 pending (high pending count)
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(room1), anyList()))
                .thenReturn(createMockAllotmentsForRoom(room1, 5, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(room1), anyList()))
                .thenReturn(createMockAllotmentsForRoom(room1, 3, RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING));
        
        // Room2: 4 active, 1 pending (low pending count) 
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(room2), anyList()))
                .thenReturn(createMockAllotmentsForRoom(room2, 4, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(room2), anyList()))
                .thenReturn(createMockAllotmentsForRoom(room2, 1, RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING));
        
        // Room3: 6 active, 2 pending (medium pending count)
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(room3), anyList()))
                .thenReturn(createMockAllotmentsForRoom(room3, 6, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(room3), anyList()))
                .thenReturn(createMockAllotmentsForRoom(room3, 2, RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING));

        // Act
        List<Map<String, Object>> sortedRooms = roomOverbookingManager.getRoomsSortedByPendingActions(hostelId);

        // Assert
        assertNotNull(sortedRooms);
        assertEquals(3, sortedRooms.size());
        
        // Verify sorting order (ascending by pending action count: 1, 2, 3)
        assertEquals("A-102", sortedRooms.get(0).get("roomNumber")); // 1 pending
        assertEquals(1, sortedRooms.get(0).get("tenantActionPendingCount"));
        
        assertEquals("A-103", sortedRooms.get(1).get("roomNumber")); // 2 pending
        assertEquals(2, sortedRooms.get(1).get("tenantActionPendingCount"));
        
        assertEquals("A-101", sortedRooms.get(2).get("roomNumber")); // 3 pending
        assertEquals(3, sortedRooms.get(2).get("tenantActionPendingCount"));
        
        // Verify other fields for first room
        Map<String, Object> firstRoom = sortedRooms.get(0);
        assertEquals(room2.getRoomId(), firstRoom.get("roomId"));
        assertEquals(8, firstRoom.get("totalBedCapacity"));
        assertEquals(4, firstRoom.get("currentOccupancy"));
        assertEquals(4, firstRoom.get("actualAvailableBeds")); // 8 - 4 = 4
        assertEquals(false, firstRoom.get("overbookingDetected"));
    }

    @Test
    void testGetRoomsSortedByPendingActions_EmptyHostel() {
        // Arrange
        UUID hostelId = UUID.randomUUID();
        when(roomRepository.findByHostel_HostelIdAndIsActive(hostelId, true))
                .thenReturn(Arrays.asList());

        // Act
        List<Map<String, Object>> sortedRooms = roomOverbookingManager.getRoomsSortedByPendingActions(hostelId);

        // Assert
        assertNotNull(sortedRooms);
        assertEquals(0, sortedRooms.size());
    }

    // Helper methods

    private List<RoomAllotment> createMockAllotments(int count, RoomAllotmentStatus status) {
        List<RoomAllotment> allotments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            RoomAllotment allotment = RoomAllotment.builder()
                    .allotmentId(UUID.randomUUID())
                    .room(testRoom)
                    .roomAllotmentStatus(status)
                    .agreementId("agreement_" + i)
                    .build();
            allotments.add(allotment);
        }
        return allotments;
    }

    private List<RoomAllotment> createMockAllotmentsForRoom(Room room, int count, RoomAllotmentStatus status) {
        List<RoomAllotment> allotments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            RoomAllotment allotment = RoomAllotment.builder()
                    .allotmentId(UUID.randomUUID())
                    .room(room)
                    .roomAllotmentStatus(status)
                    .agreementId("agreement_" + room.getRoomNumber() + "_" + i)
                    .build();
            allotments.add(allotment);
        }
        return allotments;
    }
}