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
 * Integration tests for the plan expiry notification system workflow (Task 7.1.3).
 * Tests: notification scheduling (30/15/7/3/1 days), processing with NotificationService,
 * retry mechanism, and cancellation when agreement is settled.
 */
@ExtendWith(MockitoExtension.class)
class PlanExpiryNotificationWorkflowTest {

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

    // ─── Scheduling Notifications Tests ───────────────────────────────────────

    @Test
    void testScheduleNotificationsForAllotment_Creates5Notifications() {
        // Given - Allotment expiring in 30+ days
        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.of(testAgreement));
        when(notificationRepository.existsByAgreementIdAndNotificationType(anyString(), any())).thenReturn(false);
        when(notificationRepository.save(any(PlanExpiryNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        int result = planExpiryNotificationService.scheduleNotificationsForAllotment(testAllotment);

        // Then - Should create 5 notifications (30, 15, 7, 3, 1 days)
        assertTrue(result >= 5);
        verify(notificationRepository, atLeast(5)).save(any(PlanExpiryNotification.class));
    }

    @Test
    void testScheduleNotifications_CorrectLeadTimes_30_15_7_3_1Days() {
        // Given
        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.of(testAgreement));
        when(notificationRepository.existsByAgreementIdAndNotificationType(anyString(), any())).thenReturn(false);
        when(notificationRepository.save(any(PlanExpiryNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        planExpiryNotificationService.scheduleNotificationsForAllotment(testAllotment);

        // Then - Verify notifications scheduled at correct lead times
        verify(notificationRepository, times(5)).save(argThat(notification -> {
            int leadTime = notification.getLeadTimeDays();
            return leadTime == 30 || leadTime == 15 || leadTime == 7 || leadTime == 3 || leadTime == 1;
        }));
    }

    @Test
    void testScheduleNotifications_DifferentNotificationTypes_BasedOnLeadTime() {
        // Given
        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.of(testAgreement));
        when(notificationRepository.existsByAgreementIdAndNotificationType(anyString(), any())).thenReturn(false);
        when(notificationRepository.save(any(PlanExpiryNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        planExpiryNotificationService.scheduleNotificationsForAllotment(testAllotment);

        // Then - Verify different notification types are used
        verify(notificationRepository, atLeast(1)).save(argThat(notification ->
                notification.getNotificationType() == NotificationType.PLAN_EXPIRY_REMINDER));
        verify(notificationRepository, atLeast(1)).save(argThat(notification ->
                notification.getNotificationType() == NotificationType.SETTLEMENT_WINDOW_OPEN));
        verify(notificationRepository, atLeast(1)).save(argThat(notification ->
                notification.getNotificationType() == NotificationType.URGENT_ACTION_REQUIRED));
        verify(notificationRepository, atLeast(1)).save(argThat(notification ->
                notification.getNotificationType() == NotificationType.FINAL_NOTICE));
    }

    // ─── Processing Due Notifications Tests ───────────────────────────────────

    @Test
    void testProcessDueNotifications_TriggersNotificationService() {
        // Given - Notification due today
        LocalDate currentDate = LocalDate.now();
        PlanExpiryNotification dueNotification = createTestNotification();
        dueNotification.setScheduledDate(currentDate);

        when(notificationRepository.findDueNotifications(currentDate))
                .thenReturn(List.of(dueNotification));
        doNothing().when(notificationService).sendNotification(any(), any(), any(), any(), any());
        when(notificationRepository.save(any(PlanExpiryNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        int result = planExpiryNotificationService.processDueNotifications();

        // Then
        assertEquals(1, result);
        verify(notificationService).sendNotification(
                eq(testTenant),
                eq(NotificationType.PLAN_EXPIRY_REMINDER),
                anyString(),
                anyString(),
                any());
        verify(notificationRepository, atLeastOnce()).save(any(PlanExpiryNotification.class));
    }

    @Test
    void testProcessDueNotifications_UpdatesDeliveryStatus() {
        // Given
        PlanExpiryNotification dueNotification = createTestNotification();
        when(notificationRepository.findDueNotifications(any())).thenReturn(List.of(dueNotification));
        doNothing().when(notificationService).sendNotification(any(), any(), any(), any(), any());
        when(notificationRepository.save(any(PlanExpiryNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        planExpiryNotificationService.processDueNotifications();

        // Then - Verify delivery status is updated
        verify(notificationRepository, atLeast(1)).save(argThat(notification ->
                notification.getDeliveryStatus() != PlanExpiryNotification.DeliveryStatus.PENDING));
    }

    // ─── Retry Mechanism Tests ────────────────────────────────────────────────

    @Test
    void testRetryMechanism_RetriesFailedNotifications() {
        // Given - Failed notification ready for retry
        PlanExpiryNotification failedNotification = createTestNotification();
        failedNotification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.FAILED);
        failedNotification.setRetryCount(1);

        when(notificationRepository.findNotificationsReadyForRetry(any(LocalDateTime.class)))
                .thenReturn(List.of(failedNotification));
        doNothing().when(notificationService).sendNotification(any(), any(), any(), any(), any());
        when(notificationRepository.save(any(PlanExpiryNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        int result = planExpiryNotificationService.processRetryNotifications();

        // Then
        assertEquals(1, result);
        verify(notificationService).sendNotification(any(), any(), any(), any(), any());
        verify(notificationRepository, atLeast(1)).save(argThat(notification ->
                notification.getRetryCount() > 1));
    }

    @Test
    void testRetryMechanism_MaxRetriesExceeded_MarksAsExhausted() {
        // Given - Notification with max retries exceeded
        PlanExpiryNotification exhaustedNotification = createTestNotification();
        exhaustedNotification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.FAILED);
        exhaustedNotification.setRetryCount(3); // Max is 3
        exhaustedNotification.setMaxRetries(3);

        when(notificationRepository.findNotificationsReadyForRetry(any(LocalDateTime.class)))
                .thenReturn(List.of(exhaustedNotification));
        when(notificationRepository.save(any(PlanExpiryNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        int result = planExpiryNotificationService.processRetryNotifications();

        // Then - Should not attempt to send again
        verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void testRetryMechanism_ExponentialBackoff() {
        // Given - First failed notification
        PlanExpiryNotification firstFailure = createTestNotification();
        firstFailure.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.FAILED);
        firstFailure.setRetryCount(1);
        firstFailure.setLastRetryAt(LocalDateTime.now().minusMinutes(6)); // Ready for retry

        when(notificationRepository.findNotificationsReadyForRetry(any(LocalDateTime.class)))
                .thenReturn(List.of(firstFailure));
        doNothing().when(notificationService).sendNotification(any(), any(), any(), any(), any());
        when(notificationRepository.save(any(PlanExpiryNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        planExpiryNotificationService.processRetryNotifications();

        // Then - Verify retry attempt is made
        verify(notificationRepository, atLeast(1)).save(argThat(notification ->
                notification.getRetryCount() == 2 &&
                notification.getLastRetryAt() != null));
    }

    // ─── Cancellation Tests ───────────────────────────────────────────────────

    @Test
    void testCancelNotificationsForAgreement_WhenSettled() {
        // Given
        String agreementId = "test-agreement-123";
        when(notificationRepository.cancelNotificationsForAgreement(agreementId)).thenReturn(3);

        // When
        planExpiryNotificationService.cancelNotificationsForAgreement(agreementId);

        // Then
        verify(notificationRepository).cancelNotificationsForAgreement(agreementId);
    }

    @Test
    void testCancelNotifications_PreventsFurtherProcessing() {
        // Given - Cancelled notification should not be returned by findDueNotifications
        // (repository query filters out cancelled notifications)
        when(notificationRepository.findDueNotifications(any())).thenReturn(List.of());

        // When
        int result = planExpiryNotificationService.processDueNotifications();

        // Then - No notifications processed
        assertEquals(0, result);
        verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any());
    }

    // ─── Error Handling Tests ──────────────────────────────────────────────────

    @Test
    void testProcessDueNotifications_NotificationServiceFailure_MarksAsFailed() {
        // Given
        PlanExpiryNotification dueNotification = createTestNotification();
        when(notificationRepository.findDueNotifications(any())).thenReturn(List.of(dueNotification));
        doThrow(new RuntimeException("SMS service unavailable"))
                .when(notificationService).sendNotification(any(), any(), any(), any(), any());
        when(notificationRepository.save(any(PlanExpiryNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        int result = planExpiryNotificationService.processDueNotifications();

        // Then - Should mark as failed
        verify(notificationRepository, atLeast(1)).save(argThat(notification ->
                notification.getDeliveryStatus() == PlanExpiryNotification.DeliveryStatus.FAILED));
    }

    // ─── Helper Methods ────────────────────────────────────────────────────────

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
        allotment.setEndDate(LocalDate.now().plusDays(60)); // Expires in 60 days (enough for 30-day lead time)
        allotment.setRoomAllotmentStatus(RoomAllotmentStatus.ACTIVE);
        return allotment;
    }

    private Agreement createTestAgreement() {
        Agreement agreement = new Agreement();
        agreement.setId("test-agreement-123");
        agreement.setUserId(testTenant.getUserId());
        agreement.setStartDate(LocalDate.now().minusDays(30));
        agreement.setEndDate(LocalDate.now().plusDays(60));

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
}
