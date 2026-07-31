package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlanExpiryNotification entity
 * Tests business logic methods, validation, and state transitions
 */
@DisplayName("PlanExpiryNotification Entity Tests")
class PlanExpiryNotificationTest {

    private PlanExpiryNotification notification;
    private User mockTenant;
    private RoomAllotment mockRoomAllotment;

    @BeforeEach
    void setUp() {
        // Create mock tenant
        mockTenant = new User();
        mockTenant.setUserId(UUID.randomUUID());
        mockTenant.setDisplayName("John Doe");

        // Create mock room allotment
        mockRoomAllotment = new RoomAllotment();
        mockRoomAllotment.setAllotmentId(UUID.randomUUID());

        // Create notification with basic data
        notification = PlanExpiryNotification.builder()
                .tenant(mockTenant)
                .roomAllotment(mockRoomAllotment)
                .agreementId("agreement_123")
                .notificationType(NotificationType.PLAN_EXPIRY_REMINDER)
                .scheduledDate(LocalDate.now().plusDays(7))
                .messageTemplate("plan_expiry_reminder")
                .leadTimeDays(7)
                .maxRetries(3)
                .build();
    }

    @Test
    @DisplayName("Should create notification with valid builder")
    void testNotificationBuilder() {
        // Arrange & Act - done in setUp

        // Assert
        assertNotNull(notification);
        assertEquals(mockTenant, notification.getTenant());
        assertEquals(mockRoomAllotment, notification.getRoomAllotment());
        assertEquals("agreement_123", notification.getAgreementId());
        assertEquals(NotificationType.PLAN_EXPIRY_REMINDER, notification.getNotificationType());
        assertEquals(LocalDate.now().plusDays(7), notification.getScheduledDate());
        assertEquals("plan_expiry_reminder", notification.getMessageTemplate());
        assertEquals(7, notification.getLeadTimeDays());
        assertEquals(3, notification.getMaxRetries());
        assertEquals(PlanExpiryNotification.DeliveryStatus.PENDING, notification.getDeliveryStatus());
        assertEquals(0, notification.getRetryCount());
    }

    @Test
    @DisplayName("Should correctly identify when notification is due")
    void testIsDue() {
        // Test due notification (today)
        notification.setScheduledDate(LocalDate.now());
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.PENDING);
        assertTrue(notification.isDue());

        // Test due notification (past date)
        notification.setScheduledDate(LocalDate.now().minusDays(1));
        assertTrue(notification.isDue());

        // Test not due (future date)
        notification.setScheduledDate(LocalDate.now().plusDays(1));
        assertFalse(notification.isDue());

        // Test not due (already sent)
        notification.setScheduledDate(LocalDate.now());
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.DELIVERED);
        assertFalse(notification.isDue());
    }

    @Test
    @DisplayName("Should correctly identify overdue notifications")
    void testIsOverdue() {
        // Test overdue notification
        notification.setScheduledDate(LocalDate.now().minusDays(1));
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.PENDING);
        assertTrue(notification.isOverdue());

        // Test not overdue (today)
        notification.setScheduledDate(LocalDate.now());
        assertFalse(notification.isOverdue());

        // Test not overdue (future)
        notification.setScheduledDate(LocalDate.now().plusDays(1));
        assertFalse(notification.isOverdue());
    }

    @Test
    @DisplayName("Should determine retry eligibility correctly")
    void testCanRetry() {
        // Setup failed notification
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.FAILED);
        notification.setRetryCount(1);
        notification.setMaxRetries(3);

        // Test can retry (within limit)
        assertTrue(notification.canRetry());

        // Test cannot retry (exceeded limit)
        notification.setRetryCount(3);
        assertFalse(notification.canRetry());

        // Test cannot retry (not failed)
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.PENDING);
        notification.setRetryCount(1);
        assertFalse(notification.canRetry());

        // Test notification type doesn't allow retry
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.FAILED);
        notification.setNotificationType(NotificationType.AUDIT_EVENT);
        notification.setRetryCount(0);
        assertFalse(notification.canRetry());
    }

    @Test
    @DisplayName("Should increment retry count correctly")
    void testIncrementRetryCount() {
        // Initial state
        assertEquals(0, notification.getRetryCount());
        assertNull(notification.getLastRetryAt());

        // First increment
        LocalDateTime beforeIncrement = LocalDateTime.now();
        notification.incrementRetryCount();
        LocalDateTime afterIncrement = LocalDateTime.now();

        assertEquals(1, notification.getRetryCount());
        assertNotNull(notification.getLastRetryAt());
        assertTrue(notification.getLastRetryAt().isAfter(beforeIncrement) || 
                  notification.getLastRetryAt().equals(beforeIncrement));
        assertTrue(notification.getLastRetryAt().isBefore(afterIncrement) || 
                  notification.getLastRetryAt().equals(afterIncrement));
    }

    @Test
    @DisplayName("Should mark notification states correctly")
    void testStatusTransitions() {
        // Test mark as sent
        LocalDateTime beforeSent = LocalDateTime.now();
        notification.markAsSent();
        LocalDateTime afterSent = LocalDateTime.now();

        assertEquals(PlanExpiryNotification.DeliveryStatus.SENT, notification.getDeliveryStatus());
        assertNotNull(notification.getSentAt());
        assertTrue(notification.getSentAt().isAfter(beforeSent) || 
                  notification.getSentAt().equals(beforeSent));

        // Test mark as delivered
        notification.markAsDelivered();
        assertEquals(PlanExpiryNotification.DeliveryStatus.DELIVERED, notification.getDeliveryStatus());

        // Test mark as failed (should increment retry count)
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.PENDING);
        notification.setRetryCount(0);
        notification.markAsFailed();
        assertEquals(PlanExpiryNotification.DeliveryStatus.FAILED, notification.getDeliveryStatus());
        assertEquals(1, notification.getRetryCount());

        // Test mark as cancelled
        notification.markAsCancelled();
        assertEquals(PlanExpiryNotification.DeliveryStatus.CANCELLED, notification.getDeliveryStatus());
    }

    @Test
    @DisplayName("Should get correct priority from notification type")
    void testGetPriority() {
        notification.setNotificationType(NotificationType.URGENT_ACTION_REQUIRED);
        assertEquals(1, notification.getPriority());

        notification.setNotificationType(NotificationType.SETTLEMENT_TRANSACTION_CREATED);
        assertEquals(2, notification.getPriority());

        notification.setNotificationType(NotificationType.PLAN_EXPIRY_REMINDER);
        assertEquals(3, notification.getPriority());

        notification.setNotificationType(NotificationType.SETTLEMENT_COMPLETED);
        assertEquals(4, notification.getPriority());

        notification.setNotificationType(NotificationType.AUDIT_EVENT);
        assertEquals(5, notification.getPriority());
    }

    @Test
    @DisplayName("Should determine immediate delivery requirement correctly")
    void testRequiresImmediateDelivery() {
        notification.setNotificationType(NotificationType.URGENT_ACTION_REQUIRED);
        assertTrue(notification.requiresImmediateDelivery());

        notification.setNotificationType(NotificationType.SETTLEMENT_TRANSACTION_CREATED);
        assertTrue(notification.requiresImmediateDelivery());

        notification.setNotificationType(NotificationType.PLAN_EXPIRY_REMINDER);
        assertFalse(notification.requiresImmediateDelivery());

        notification.setNotificationType(NotificationType.SETTLEMENT_COMPLETED);
        assertFalse(notification.requiresImmediateDelivery());
    }

    @Test
    @DisplayName("Should calculate days until scheduled correctly")
    void testGetDaysUntilScheduled() {
        // Future date
        notification.setScheduledDate(LocalDate.now().plusDays(5));
        assertEquals(5, notification.getDaysUntilScheduled());

        // Today
        notification.setScheduledDate(LocalDate.now());
        assertEquals(0, notification.getDaysUntilScheduled());

        // Past date (should be negative)
        notification.setScheduledDate(LocalDate.now().minusDays(3));
        assertEquals(-3, notification.getDaysUntilScheduled());

        // Null scheduled date
        notification.setScheduledDate(null);
        assertEquals(0, notification.getDaysUntilScheduled());
    }

    @Test
    @DisplayName("Should calculate next retry time with exponential backoff")
    void testGetNextRetryTime() {
        // Setup for retry
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.FAILED);
        notification.setMaxRetries(5);

        // Test different retry counts
        LocalDateTime baseTime = LocalDateTime.now();

        notification.setRetryCount(0);
        LocalDateTime nextRetry0 = notification.getNextRetryTime();
        assertNotNull(nextRetry0);
        assertTrue(nextRetry0.isAfter(baseTime));

        notification.setRetryCount(1);
        LocalDateTime nextRetry1 = notification.getNextRetryTime();
        assertNotNull(nextRetry1);
        assertTrue(nextRetry1.isAfter(nextRetry0));

        notification.setRetryCount(2);
        LocalDateTime nextRetry2 = notification.getNextRetryTime();
        assertNotNull(nextRetry2);
        assertTrue(nextRetry2.isAfter(nextRetry1));

        // Test no retry when exceeded limit
        notification.setRetryCount(5);
        assertNull(notification.getNextRetryTime());

        // Test no retry when not failed
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.PENDING);
        notification.setRetryCount(1);
        assertNull(notification.getNextRetryTime());
    }

    @Test
    @DisplayName("Should identify terminal states correctly")
    void testIsTerminal() {
        // Non-terminal states
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.PENDING);
        assertFalse(notification.isTerminal());

        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.SENT);
        assertFalse(notification.isTerminal());

        // Terminal states
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.DELIVERED);
        assertTrue(notification.isTerminal());

        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.CANCELLED);
        assertTrue(notification.isTerminal());

        // Failed with retry available (not terminal)
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.FAILED);
        notification.setRetryCount(1);
        notification.setMaxRetries(3);
        assertFalse(notification.isTerminal());

        // Failed without retry available (terminal)
        notification.setRetryCount(3);
        assertTrue(notification.isTerminal());
    }

    @Test
    @DisplayName("Should provide correct status description")
    void testGetStatusDescription() {
        // Pending (future)
        notification.setScheduledDate(LocalDate.now().plusDays(1));
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.PENDING);
        String description = notification.getStatusDescription();
        assertTrue(description.contains("Scheduled"));

        // Pending (due)
        notification.setScheduledDate(LocalDate.now());
        description = notification.getStatusDescription();
        assertEquals("Ready to send", description);

        // Sent
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.SENT);
        description = notification.getStatusDescription();
        assertEquals("Sent to delivery service", description);

        // Delivered
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.DELIVERED);
        notification.setSentAt(LocalDateTime.now().minusHours(1));
        description = notification.getStatusDescription();
        assertTrue(description.contains("Delivered on"));

        // Failed (with retry)
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.FAILED);
        notification.setRetryCount(2);
        notification.setMaxRetries(3);
        description = notification.getStatusDescription();
        assertTrue(description.contains("Failed"));
        assertTrue(description.contains("Retry 2/3"));

        // Failed (no retry)
        notification.setRetryCount(3);
        description = notification.getStatusDescription();
        assertTrue(description.contains("No more retries"));

        // Cancelled
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.CANCELLED);
        description = notification.getStatusDescription();
        assertEquals("Cancelled", description);
    }

    @Test
    @DisplayName("Should manage message variables correctly")
    void testMessageVariables() {
        // Test adding message variables
        notification.addMessageVariable("tenantName", "John Doe");
        notification.addMessageVariable("roomNumber", "A-101");
        notification.addMessageVariable("endDate", "2024-12-31");

        assertEquals("John Doe", notification.getMessageVariable("tenantName"));
        assertEquals("A-101", notification.getMessageVariable("roomNumber"));
        assertEquals("2024-12-31", notification.getMessageVariable("endDate"));

        // Test getting non-existent variable
        assertNull(notification.getMessageVariable("nonExistent"));

        // Test overwriting variable
        notification.addMessageVariable("tenantName", "Jane Smith");
        assertEquals("Jane Smith", notification.getMessageVariable("tenantName"));

        // Test with null message variables map initially
        PlanExpiryNotification newNotification = new PlanExpiryNotification();
        newNotification.addMessageVariable("test", "value");
        assertEquals("value", newNotification.getMessageVariable("test"));
    }

    @Test
    @DisplayName("Should validate notification data correctly")
    void testIsValid() {
        // Valid notification
        assertTrue(notification.isValid());

        // Test null tenant
        notification.setTenant(null);
        assertFalse(notification.isValid());
        notification.setTenant(mockTenant);

        // Test null/empty agreement ID
        notification.setAgreementId(null);
        assertFalse(notification.isValid());
        notification.setAgreementId("");
        assertFalse(notification.isValid());
        notification.setAgreementId("   ");
        assertFalse(notification.isValid());
        notification.setAgreementId("agreement_123");

        // Test null notification type
        notification.setNotificationType(null);
        assertFalse(notification.isValid());
        notification.setNotificationType(NotificationType.PLAN_EXPIRY_REMINDER);

        // Test null scheduled date
        notification.setScheduledDate(null);
        assertFalse(notification.isValid());
        notification.setScheduledDate(LocalDate.now().plusDays(7));

        // Test null/empty message template
        notification.setMessageTemplate(null);
        assertFalse(notification.isValid());
        notification.setMessageTemplate("");
        assertFalse(notification.isValid());
        notification.setMessageTemplate("   ");
        assertFalse(notification.isValid());
        notification.setMessageTemplate("template");

        // Test invalid lead time days
        notification.setLeadTimeDays(null);
        assertFalse(notification.isValid());
        notification.setLeadTimeDays(0);
        assertFalse(notification.isValid());
        notification.setLeadTimeDays(-1);
        assertFalse(notification.isValid());
        notification.setLeadTimeDays(100);
        assertFalse(notification.isValid());
        notification.setLeadTimeDays(7);

        // Test invalid max retries
        notification.setMaxRetries(null);
        assertFalse(notification.isValid());
        notification.setMaxRetries(-1);
        assertFalse(notification.isValid());
        notification.setMaxRetries(3);

        // Should be valid again
        assertTrue(notification.isValid());
    }

    @Test
    @DisplayName("Should generate correct toString representation")
    void testToString() {
        String toString = notification.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("PlanExpiryNotification"));
        assertTrue(toString.contains("agreement_123"));
        assertTrue(toString.contains("PLAN_EXPIRY_REMINDER"));
        assertTrue(toString.contains("PENDING"));
        assertTrue(toString.contains("leadTimeDays=7"));
        assertTrue(toString.contains("retryCount=0"));
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void testEqualsAndHashCode() {
        // Create second notification with different data but no ID
        PlanExpiryNotification notification2 = PlanExpiryNotification.builder()
                .tenant(mockTenant)
                .agreementId("different_agreement")
                .notificationType(NotificationType.URGENT_ACTION_REQUIRED)
                .scheduledDate(LocalDate.now().plusDays(3))
                .messageTemplate("different_template")
                .leadTimeDays(3)
                .build();

        // Without IDs set, should not be equal
        assertNotEquals(notification, notification2);

        // Set same ID on both
        UUID sameId = UUID.randomUUID();
        notification.setNotificationId(sameId);
        notification2.setNotificationId(sameId);

        // Should be equal with same ID
        assertEquals(notification, notification2);
        assertEquals(notification.hashCode(), notification2.hashCode());

        // Test with null ID
        notification.setNotificationId(null);
        notification2.setNotificationId(null);
        assertNotEquals(notification, notification2);

        // Test reflexivity
        notification.setNotificationId(UUID.randomUUID());
        assertEquals(notification, notification);

        // Test with different types
        assertNotEquals(notification, "not a notification");
        assertNotEquals(notification, null);
    }

    @Test
    @DisplayName("Should handle edge cases gracefully")
    void testEdgeCases() {
        // Test with minimal valid data
        PlanExpiryNotification minimal = PlanExpiryNotification.builder()
                .tenant(mockTenant)
                .agreementId("test")
                .notificationType(NotificationType.PLAN_EXPIRY_REMINDER)
                .scheduledDate(LocalDate.now())
                .messageTemplate("template")
                .leadTimeDays(1)
                .maxRetries(0) // No retries allowed
                .build();

        assertTrue(minimal.isValid());
        assertFalse(minimal.canRetry()); // Never can retry with maxRetries=0

        // Test with boundary lead time values
        minimal.setLeadTimeDays(1);
        assertTrue(minimal.isValid());
        
        minimal.setLeadTimeDays(90);
        assertTrue(minimal.isValid());

        // Test retry logic with different notification types
        minimal.setNotificationType(NotificationType.AUDIT_EVENT);
        minimal.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.FAILED);
        minimal.setMaxRetries(3);
        minimal.setRetryCount(0);
        assertFalse(minimal.canRetry()); // AUDIT_EVENT doesn't allow retry

        minimal.setNotificationType(NotificationType.URGENT_ACTION_REQUIRED);
        assertTrue(minimal.canRetry()); // High priority allows retry
    }
}