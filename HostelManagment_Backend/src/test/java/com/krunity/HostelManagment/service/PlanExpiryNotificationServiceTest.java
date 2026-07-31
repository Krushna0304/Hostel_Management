package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.enums.NotificationType;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.PlanExpiryNotificationRepository;
import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlanExpiryNotificationService
 */
@ExtendWith(MockitoExtension.class)
class PlanExpiryNotificationServiceTest {

    @Mock
    private PlanExpiryNotificationRepository notificationRepository;

    @Mock
    private RoomAllotmentRepository roomAllotmentRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AgreementService agreementService;

    @InjectMocks
    private PlanExpiryNotificationService planExpiryNotificationService;

    private User testTenant;
    private Room testRoom;
    private RoomAllotment testAllotment;
    private Agreement testAgreement;

    @BeforeEach
    void setUp() {
        // Set up configuration values
        ReflectionTestUtils.setField(planExpiryNotificationService, "defaultLeadTimes", 
            new String[]{"30", "15", "7", "3", "1"});
        ReflectionTestUtils.setField(planExpiryNotificationService, "planExpiryNotificationsEnabled", true);
        ReflectionTestUtils.setField(planExpiryNotificationService, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(planExpiryNotificationService, "cleanupDaysThreshold", 30);
        ReflectionTestUtils.setField(planExpiryNotificationService, "retryDelayMinutes", 5);
        ReflectionTestUtils.setField(planExpiryNotificationService, "deliveryConfirmationTimeoutMinutes", 5);

        // Create test data
        testTenant = createTestTenant();
        testRoom = createTestRoom();
        testAllotment = createTestAllotment();
        testAgreement = createTestAgreement();
    }

    @Test
    void testProcessDueNotifications_SuccessfulDelivery() {
        // Arrange
        LocalDate currentDate = LocalDate.now();
        PlanExpiryNotification dueNotification = createTestNotification();
        when(notificationRepository.findDueNotifications(currentDate))
            .thenReturn(List.of(dueNotification));
        doNothing().when(notificationService).sendNotification(any(), any(), any(), any(), any());

        // Act
        int result = planExpiryNotificationService.processDueNotifications();

        // Assert
        assertEquals(1, result);
        verify(notificationRepository, atLeastOnce()).save(any(PlanExpiryNotification.class));
    }

    @Test
    void testScheduleNotificationsForAllotment() {
        // Arrange
        when(agreementService.getAgreementById(anyString()))
            .thenReturn(Optional.of(testAgreement));
        when(notificationRepository.existsByAgreementIdAndNotificationType(anyString(), any()))
            .thenReturn(false);
        when(notificationRepository.save(any(PlanExpiryNotification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int result = planExpiryNotificationService.scheduleNotificationsForAllotment(testAllotment);

        // Assert
        assertTrue(result > 0);
        verify(notificationRepository, atLeast(1)).save(any(PlanExpiryNotification.class));
    }

    @Test
    void testGetNotificationTypeForLeadTime() {
        // Test different lead times
        assertEquals(NotificationType.PLAN_EXPIRY_REMINDER, 
            invokeGetNotificationTypeForLeadTime(30));
        assertEquals(NotificationType.SETTLEMENT_WINDOW_OPEN, 
            invokeGetNotificationTypeForLeadTime(15));
        assertEquals(NotificationType.URGENT_ACTION_REQUIRED, 
            invokeGetNotificationTypeForLeadTime(3));
        assertEquals(NotificationType.FINAL_NOTICE, 
            invokeGetNotificationTypeForLeadTime(1));
    }

    @Test
    void testProcessRetryNotifications_WithExponentialBackoff() {
        // Arrange
        PlanExpiryNotification failedNotification = createTestNotification();
        failedNotification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.FAILED);
        failedNotification.setRetryCount(1);
        
        when(notificationRepository.findNotificationsReadyForRetry(any(LocalDateTime.class)))
            .thenReturn(List.of(failedNotification));
        doNothing().when(notificationService).sendNotification(any(), any(), any(), any(), any());

        // Act
        int result = planExpiryNotificationService.processRetryNotifications();

        // Assert
        assertEquals(1, result);
        verify(notificationRepository, atLeastOnce()).save(any(PlanExpiryNotification.class));
    }

    @Test
    void testScheduleNewNotifications() {
        // Arrange
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(90);
        
        when(roomAllotmentRepository.findActiveAllotmentsExpiringBetween(startDate, endDate))
            .thenReturn(List.of(testAllotment));
        when(agreementService.getAgreementById(anyString()))
            .thenReturn(Optional.of(testAgreement));
        when(notificationRepository.existsByAgreementIdAndNotificationType(anyString(), any()))
            .thenReturn(false);

        // Act
        int result = planExpiryNotificationService.scheduleNewNotifications();

        // Assert
        assertTrue(result >= 0);
    }

    @Test
    void testGetDeliveryStatistics() {
        // Arrange
        List<Object[]> statusCountsList = Arrays.asList(
            new Object[]{"DELIVERED", 5L}, 
            new Object[]{"FAILED", 2L}
        );
        when(notificationRepository.countByDeliveryStatus())
            .thenReturn(statusCountsList);
            
        List<Object[]> typeCountsList = Arrays.<Object[]>asList(
    new Object[] { "PLAN_EXPIRY_REMINDER", 3L }
);
        when(notificationRepository.countByNotificationType())
            .thenReturn(typeCountsList);
        when(notificationRepository.getDeliverySuccessRate())
            .thenReturn(85.5);
        when(notificationRepository.findTodaysPendingNotifications())
            .thenReturn(List.of());

        // Act
        Map<String, Object> stats = planExpiryNotificationService.getDeliveryStatistics();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("deliveryStatusCounts"));
        assertTrue(stats.containsKey("notificationTypeCounts"));
        assertTrue(stats.containsKey("deliverySuccessRate"));
        assertEquals(85.5, stats.get("deliverySuccessRate"));
    }

    @Test
    void testNotificationSystemHealth() {
        // Arrange
        when(notificationRepository.count()).thenReturn(100L);
        when(notificationRepository.findByDeliveryStatusOrderByScheduledDateAsc(any()))
            .thenReturn(List.of());
        when(notificationRepository.findExhaustedRetryNotifications())
            .thenReturn(List.of());

        // Act
        Map<String, Object> health = planExpiryNotificationService.getNotificationSystemHealth();

        // Assert
        assertNotNull(health);
        assertEquals("HEALTHY", health.get("status"));
        assertEquals(100L, health.get("totalNotifications"));
        assertTrue(health.containsKey("lastCheck"));
    }

    @Test
    void testCancelNotificationsForAgreement() {
        // Arrange
        String agreementId = "test-agreement-123";
        when(notificationRepository.cancelNotificationsForAgreement(agreementId))
            .thenReturn(3);

        // Act
        planExpiryNotificationService.cancelNotificationsForAgreement(agreementId);

        // Assert
        verify(notificationRepository).cancelNotificationsForAgreement(agreementId);
    }

    @Test
    void testGetNotificationConfiguration() {
        // Act
        Map<String, Object> config = planExpiryNotificationService.getNotificationConfiguration();

        // Assert
        assertNotNull(config);
        assertTrue(config.containsKey("enabled"));
        assertTrue(config.containsKey("leadTimes"));
        assertTrue(config.containsKey("maxRetryAttempts"));
        assertEquals(true, config.get("enabled"));
        assertEquals(3, config.get("maxRetryAttempts"));
    }

    // Helper methods

    private User createTestTenant() {
        User tenant = new User();
        tenant.setUserId(UUID.randomUUID());
        tenant.setUsername("testtenant");
        tenant.setDisplayName("Test Tenant");
        tenant.setPhoneNumber("+1234567890");
        return tenant;
    }

    private Room createTestRoom() {
        Room room = new Room();
        room.setRoomId(UUID.randomUUID());
        room.setRoomNumber("101");
        room.setTotalBeds(4);
        return room;
    }

    private RoomAllotment createTestAllotment() {
        RoomAllotment allotment = new RoomAllotment();
        allotment.setAllotmentId(UUID.randomUUID());
        allotment.setTenant(testTenant);
        allotment.setRoom(testRoom);
        allotment.setAgreementId("test-agreement-123");
        allotment.setStartDate(LocalDate.now().minusDays(30));
        allotment.setEndDate(LocalDate.now().plusDays(15)); // Expires in 15 days
        allotment.setRoomAllotmentStatus(RoomAllotmentStatus.ACTIVE);
        return allotment;
    }

    private Agreement createTestAgreement() {
        Agreement agreement = new Agreement();
        agreement.setId("test-agreement-123");
        agreement.setUserId(testTenant.getUserId());
        agreement.setStartDate(LocalDate.now().minusDays(30));
        agreement.setEndDate(LocalDate.now().plusDays(15));
        
        // Create plan snapshot
        RoomAgreementPlan planSnapshot = new RoomAgreementPlan();
        planSnapshot.setPlanName("Standard Monthly Plan");
        agreement.setPlanSnapshot(planSnapshot);
        
        return agreement;
    }

    private PlanExpiryNotification createTestNotification() {
        return PlanExpiryNotification.builder()
            .notificationId(UUID.randomUUID())
            .tenant(testTenant)
            .roomAllotment(testAllotment)
            .agreementId(testAllotment.getAgreementId())
            .notificationType(NotificationType.PLAN_EXPIRY_REMINDER)
            .scheduledDate(LocalDate.now())
            .messageTemplate("plan_expiry_reminder")
            .leadTimeDays(15)
            .maxRetries(3)
            .build();
    }

    // Use reflection to test private method
    private NotificationType invokeGetNotificationTypeForLeadTime(int leadTimeDays) {
        try {
            java.lang.reflect.Method method = planExpiryNotificationService.getClass()
                .getDeclaredMethod("getNotificationTypeForLeadTime", int.class);
            method.setAccessible(true);
            return (NotificationType) method.invoke(planExpiryNotificationService, leadTimeDays);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method", e);
        }
    }
}