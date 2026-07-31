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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for room overbooking scenarios (Task 7.1.4).
 * Tests: allocation success with beds available, overbooking detection (20 for 10 beds),
 * graceful failure when full, availability calculation with pending actions,
 * and concurrent allocation thread safety.
 */
@ExtendWith(MockitoExtension.class)
class RoomOverbookingIntegrationTest {

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

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();

        testRoom = Room.builder()
                .roomId(roomId)
                .roomNumber("D-404")
                .totalBeds(10)
                .availableBeds(10)
                .isActive(true)
                .build();

        testTenant = User.builder()
                .userId(UUID.randomUUID())
                .displayName("Integration Test Tenant")
                .build();
    }

    // ─── Successful Allocation Tests ──────────────────────────────────────────

    @Test
    void testRoomAllocation_SucceedsWhenBedsAvailable() {
        // Given - 5 of 10 beds occupied
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(createMockAllotments(5, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(Collections.emptyList());
        when(overbookingLogRepository.findMaxAllocationOrderForRoom(testRoom)).thenReturn(5);
        when(overbookingLogRepository.save(any(RoomOverbookingLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        RoomOverbookingManager.RoomAllocationResult result =
                roomOverbookingManager.allocateRoom(roomId, "agreement-new-001", testTenant);

        // Then
        assertTrue(result.isSuccessful());
        assertEquals(6, result.getAllocationOrder()); // 5 + 1
        assertFalse(result.isOverbookingDetected());
        assertNotNull(result.getAvailabilityInfo());
        assertEquals(5, result.getAvailabilityInfo().getActualAvailableBeds());
    }

    @Test
    void testRoomAvailability_CorrectCalculation() {
        // Given - 8 active, 2 pending
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(createMockAllotments(8, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(createMockAllotments(2, RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING));

        // When
        RoomOverbookingManager.RoomAvailabilityInfo info =
                roomOverbookingManager.calculateRoomAvailability(roomId);

        // Then
        assertNotNull(info);
        assertEquals(10, info.getTotalBedCapacity());
        assertEquals(8, info.getActiveAllotments());
        assertEquals(2, info.getPendingActionCount());
        assertEquals(2, info.getActualAvailableBeds()); // 10 - 8 = 2
        assertFalse(info.isOverbookingScenario());
    }

    // ─── Overbooking Detection Tests ──────────────────────────────────────────

    @Test
    void testOverbookingDetection_20AllocationsFor10BedRoom() {
        // Given - 20 active allotments in a 10-bed room
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(createMockAllotments(20, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(Collections.emptyList());

        // When
        RoomOverbookingManager.RoomAvailabilityInfo info =
                roomOverbookingManager.calculateRoomAvailability(roomId);

        // Then - Overbooking is detected
        assertTrue(info.isOverbookingScenario());
        assertEquals(20, info.getActiveAllotments());
        assertEquals(0, info.getActualAvailableBeds()); // Max(0, 10-20) = 0
        assertEquals("FULLY_OCCUPIED", info.getAllocationStatus());
    }

    @Test
    void testOverbookingDetection_AllocationStillAllowed_ForFirstComeFirstServed() {
        // Given - Already overbooked (system allows overbooking but tracks it)
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(createMockAllotments(12, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(Collections.emptyList());
        when(overbookingLogRepository.findMaxAllocationOrderForRoom(testRoom)).thenReturn(12);
        when(overbookingLogRepository.save(any(RoomOverbookingLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        RoomOverbookingManager.RoomAllocationResult result =
                roomOverbookingManager.allocateRoom(roomId, "agreement-overbook-001", testTenant);

        // Then - System records overbooking but still processes (first-come-first-served)
        assertTrue(result.isSuccessful());
        assertTrue(result.getMessage().contains("overbooking"));
        // Two saves: allocation log + overbooking detection log
        verify(overbookingLogRepository, times(2)).save(any(RoomOverbookingLog.class));
    }

    // ─── Allocation Failure Tests ─────────────────────────────────────────────

    @Test
    void testAllocationFails_RoomNotFound() {
        // Given
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When
        RoomOverbookingManager.RoomAllocationResult result =
                roomOverbookingManager.allocateRoom(roomId, "agreement-001", testTenant);

        // Then
        assertFalse(result.isSuccessful());
        assertEquals("Room not found", result.getMessage());
        assertNull(result.getAllocationOrder());
        verify(overbookingLogRepository, never()).save(any());
    }

    @Test
    void testHandleAllocationFailure_LogsWithCorrectDetails() {
        // Given
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(createMockAllotments(10, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(Collections.emptyList());
        when(overbookingLogRepository.save(any(RoomOverbookingLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        roomOverbookingManager.handleAllocationFailure(roomId, "failed-agreement-001", "Capacity exceeded");

        // Then
        verify(overbookingLogRepository).save(argThat(log ->
                log.getEventType() == RoomOverbookingLog.EventType.ALLOCATION_FAILED &&
                log.getFailureReason().equals("Capacity exceeded") &&
                !log.getAllocationSuccessful()));
    }

    // ─── Pending Actions Consideration Tests ──────────────────────────────────

    @Test
    void testAvailabilityCalculation_ConsidersPendingActions() {
        // Given - 7 active + 3 pending in a 10-bed room
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(createMockAllotments(7, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(createMockAllotments(3, RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING));

        // When
        RoomOverbookingManager.RoomAvailabilityInfo info =
                roomOverbookingManager.calculateRoomAvailability(roomId);

        // Then - Availability considers active only (pending are being processed)
        assertEquals(7, info.getActiveAllotments());
        assertEquals(3, info.getPendingActionCount());
        assertEquals(3, info.getActualAvailableBeds()); // 10 - 7 = 3 (pending not counted)
        assertEquals("AVAILABLE_PENDING_LIMITED", info.getAllocationStatus());
    }

    @Test
    void testGetRoomAvailabilityDisplay_IncludesAllRelevantFields() {
        // Given
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(createMockAllotments(6, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(createMockAllotments(2, RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING));

        // When
        Map<String, Object> display = roomOverbookingManager.getRoomAvailabilityDisplay(roomId);

        // Then
        assertNotNull(display);
        assertEquals(roomId, display.get("roomId"));
        assertEquals("D-404", display.get("roomNumber"));
        assertEquals(10, display.get("totalBedCapacity"));
        assertEquals(6, display.get("currentOccupancy"));
        assertEquals(2, display.get("tenantActionPendingCount"));
        assertEquals(4, display.get("actualAvailableBeds")); // 10 - 6 = 4
        assertEquals(false, display.get("overbookingDetected"));
    }

    // ─── Concurrent Allocation Safety Tests ───────────────────────────────────

    @Test
    void testConcurrentAllocation_ThreadSafety() throws InterruptedException {
        // Given - Room with 5 available beds, 10 concurrent allocation attempts
        int concurrentRequests = 10;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger allocationOrder = new AtomicInteger(0);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(testRoom), anyList()))
                .thenReturn(createMockAllotments(5, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(testRoom), anyList()))
                .thenReturn(Collections.emptyList());
        when(overbookingLogRepository.findMaxAllocationOrderForRoom(testRoom))
                .thenAnswer(inv -> allocationOrder.get());
        when(overbookingLogRepository.save(any(RoomOverbookingLog.class))).thenAnswer(inv -> {
            allocationOrder.incrementAndGet();
            return inv.getArgument(0);
        });

        // When - Multiple concurrent allocations
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        List<RoomOverbookingManager.RoomAllocationResult> results =
                Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < concurrentRequests; i++) {
            String agreementId = "concurrent-agreement-" + i;
            executor.submit(() -> {
                try {
                    RoomOverbookingManager.RoomAllocationResult result =
                            roomOverbookingManager.allocateRoom(roomId, agreementId, testTenant);
                    results.add(result);
                    if (result.isSuccessful()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then - All requests processed without exception, results collected
        assertEquals(concurrentRequests, results.size());
        // All succeed because overbooking is not strictly enforced
        assertEquals(concurrentRequests, successCount.get());
    }

    @Test
    void testGetRoomsSortedByPendingActions_AscendingOrder() {
        // Given
        UUID hostelId = UUID.randomUUID();

        Room room1 = Room.builder().roomId(UUID.randomUUID()).roomNumber("E-101").totalBeds(10).isActive(true).build();
        Room room2 = Room.builder().roomId(UUID.randomUUID()).roomNumber("E-102").totalBeds(10).isActive(true).build();
        Room room3 = Room.builder().roomId(UUID.randomUUID()).roomNumber("E-103").totalBeds(10).isActive(true).build();

        when(roomRepository.findByHostel_HostelIdAndIsActive(hostelId, true))
                .thenReturn(Arrays.asList(room1, room2, room3));

        // room1 → 5 active, 4 pending
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(room1), anyList()))
                .thenReturn(createMockAllotmentsForRoom(room1, 5, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(room1), anyList()))
                .thenReturn(createMockAllotmentsForRoom(room1, 4, RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING));

        // room2 → 3 active, 1 pending
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(room2), anyList()))
                .thenReturn(createMockAllotmentsForRoom(room2, 3, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(room2), anyList()))
                .thenReturn(createMockAllotmentsForRoom(room2, 1, RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING));

        // room3 → 6 active, 2 pending
        when(roomAllotmentRepository.findActiveAllotmentsByRoom(eq(room3), anyList()))
                .thenReturn(createMockAllotmentsForRoom(room3, 6, RoomAllotmentStatus.ACTIVE));
        when(roomAllotmentRepository.findByRoomAndRoomAllotmentStatusIn(eq(room3), anyList()))
                .thenReturn(createMockAllotmentsForRoom(room3, 2, RoomAllotmentStatus.ALLOTMENT_ACTION_PENDING));

        // When
        List<Map<String, Object>> sorted = roomOverbookingManager.getRoomsSortedByPendingActions(hostelId);

        // Then - Sorted ascending by pending count: 1 (room2), 2 (room3), 4 (room1)
        assertEquals(3, sorted.size());
        assertEquals("E-102", sorted.get(0).get("roomNumber")); // 1 pending
        assertEquals("E-103", sorted.get(1).get("roomNumber")); // 2 pending
        assertEquals("E-101", sorted.get(2).get("roomNumber")); // 4 pending
    }

    // ─── Helper Methods ────────────────────────────────────────────────────────

    private List<RoomAllotment> createMockAllotments(int count, RoomAllotmentStatus status) {
        List<RoomAllotment> allotments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            allotments.add(RoomAllotment.builder()
                    .allotmentId(UUID.randomUUID())
                    .room(testRoom)
                    .roomAllotmentStatus(status)
                    .agreementId("agreement_" + i)
                    .build());
        }
        return allotments;
    }

    private List<RoomAllotment> createMockAllotmentsForRoom(Room room, int count, RoomAllotmentStatus status) {
        List<RoomAllotment> allotments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            allotments.add(RoomAllotment.builder()
                    .allotmentId(UUID.randomUUID())
                    .room(room)
                    .roomAllotmentStatus(status)
                    .agreementId("agreement_" + room.getRoomNumber() + "_" + i)
                    .build());
        }
        return allotments;
    }
}
