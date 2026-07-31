package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.NotificationType;
import com.krunity.HostelManagment.model.PlanExpiryNotification;
import com.krunity.HostelManagment.model.Role;
import com.krunity.HostelManagment.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PlanExpiryNotificationRepository
 * Tests repository methods with actual database operations
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PlanExpiryNotification Repository Tests")
class PlanExpiryNotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlanExpiryNotificationRepository notificationRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testTenant;
    private Role testRole;

    @BeforeEach
    void setUp() {
        // Create and persist a test role
        testRole = new Role();
        testRole.setName("TENANT");
        testRole = roleRepository.save(testRole);

        // Create and persist a test tenant
        testTenant = User.builder()
                .displayName("Test Tenant")
                .username("test.tenant")
                .passwordHash("hashedpassword")
                .phoneNumber("+1234567890")
                .role(testRole)
                .isActive(true)
                .build();
        testTenant = entityManager.persistAndFlush(testTenant);
    }

    @Test
    @DisplayName("Should save and retrieve notification successfully")
    void testSaveAndRetrieveNotification() {
        // Arrange
        PlanExpiryNotification notification = PlanExpiryNotification.builder()
                .tenant(testTenant)
                .agreementId("test_agreement_123")
                .notificationType(NotificationType.PLAN_EXPIRY_REMINDER)
                .scheduledDate(LocalDate.now().plusDays(7))
                .messageTemplate("plan_expiry_reminder")
                .leadTimeDays(7)
                .maxRetries(3)
                .build();

        // Act
        PlanExpiryNotification savedNotification = notificationRepository.save(notification);
        entityManager.flush();

        Optional<PlanExpiryNotification> retrievedNotification = 
                notificationRepository.findById(savedNotification.getNotificationId());

        // Assert
        assertTrue(retrievedNotification.isPresent());
        assertEquals(notification.getAgreementId(), retrievedNotification.get().getAgreementId());
        assertEquals(notification.getNotificationType(), retrievedNotification.get().getNotificationType());
        assertEquals(notification.getScheduledDate(), retrievedNotification.get().getScheduledDate());
        assertEquals(PlanExpiryNotification.DeliveryStatus.PENDING, retrievedNotification.get().getDeliveryStatus());
    }

    @Test
    @DisplayName("Should find due notifications correctly")
    void testFindDueNotifications() {
        // Arrange
        // Create notifications with different scheduled dates
        PlanExpiryNotification dueToday = createTestNotification("agreement_1", LocalDate.now());
        PlanExpiryNotification overdue = createTestNotification("agreement_2", LocalDate.now().minusDays(2));
        PlanExpiryNotification future = createTestNotification("agreement_3", LocalDate.now().plusDays(3));

        notificationRepository.saveAll(List.of(dueToday, overdue, future));
        entityManager.flush();

        // Act
        List<PlanExpiryNotification> dueNotifications = 
                notificationRepository.findDueNotifications(LocalDate.now());

        // Assert
        assertEquals(2, dueNotifications.size());
        assertTrue(dueNotifications.stream().anyMatch(n -> n.getAgreementId().equals("agreement_1")));
        assertTrue(dueNotifications.stream().anyMatch(n -> n.getAgreementId().equals("agreement_2")));
        assertFalse(dueNotifications.stream().anyMatch(n -> n.getAgreementId().equals("agreement_3")));
    }

    @Test
    @DisplayName("Should find notifications by agreement ID")
    void testFindByAgreementId() {
        // Arrange
        String agreementId = "test_agreement_456";
        PlanExpiryNotification notification1 = createTestNotification(agreementId, LocalDate.now().plusDays(5));
        notification1.setNotificationType(NotificationType.PLAN_EXPIRY_REMINDER);

        PlanExpiryNotification notification2 = createTestNotification(agreementId, LocalDate.now().plusDays(2));
        notification2.setNotificationType(NotificationType.URGENT_ACTION_REQUIRED);

        notificationRepository.saveAll(List.of(notification1, notification2));
        entityManager.flush();

        // Act
        List<PlanExpiryNotification> notifications = 
                notificationRepository.findByAgreementIdOrderByScheduledDateAsc(agreementId);

        // Assert
        assertEquals(2, notifications.size());
        assertEquals(NotificationType.URGENT_ACTION_REQUIRED, notifications.get(0).getNotificationType()); // Earlier date
        assertEquals(NotificationType.PLAN_EXPIRY_REMINDER, notifications.get(1).getNotificationType()); // Later date
    }

    @Test
    @DisplayName("Should prevent duplicate notifications")
    void testExistsByAgreementIdAndNotificationType() {
        // Arrange
        String agreementId = "test_agreement_789";
        NotificationType notificationType = NotificationType.SETTLEMENT_WINDOW_OPEN;

        PlanExpiryNotification notification = createTestNotification(agreementId, LocalDate.now().plusDays(1));
        notification.setNotificationType(notificationType);
        notificationRepository.save(notification);
        entityManager.flush();

        // Act & Assert
        assertTrue(notificationRepository.existsByAgreementIdAndNotificationType(agreementId, notificationType));
        assertFalse(notificationRepository.existsByAgreementIdAndNotificationType(agreementId, NotificationType.FINAL_NOTICE));
        assertFalse(notificationRepository.existsByAgreementIdAndNotificationType("different_agreement", notificationType));
    }

    @Test
    @DisplayName("Should mark notification as sent")
    void testMarkAsSent() {
        // Arrange
        PlanExpiryNotification notification = createTestNotification("agreement_mark_sent", LocalDate.now());
        notification = notificationRepository.save(notification);
        entityManager.flush();

        // Act
        int updatedRows = notificationRepository.markAsSent(
                notification.getNotificationId(), 
                java.time.LocalDateTime.now()
        );
        entityManager.flush();
        entityManager.clear(); // Clear persistence context to force reload

        // Assert
        assertEquals(1, updatedRows);
        
        PlanExpiryNotification updatedNotification = 
                notificationRepository.findById(notification.getNotificationId()).orElseThrow();
        assertEquals(PlanExpiryNotification.DeliveryStatus.SENT, updatedNotification.getDeliveryStatus());
        assertNotNull(updatedNotification.getSentAt());
    }

    @Test
    @DisplayName("Should cancel notifications for agreement")
    void testCancelNotificationsForAgreement() {
        // Arrange
        String agreementId = "agreement_to_cancel";
        PlanExpiryNotification pending = createTestNotification(agreementId, LocalDate.now().plusDays(1));
        pending.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.PENDING);

        PlanExpiryNotification failed = createTestNotification(agreementId, LocalDate.now().plusDays(2));
        failed.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.FAILED);

        PlanExpiryNotification delivered = createTestNotification(agreementId, LocalDate.now().plusDays(3));
        delivered.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.DELIVERED);

        notificationRepository.saveAll(List.of(pending, failed, delivered));
        entityManager.flush();

        // Act
        int cancelledCount = notificationRepository.cancelNotificationsForAgreement(agreementId);
        entityManager.flush();
        entityManager.clear(); // Clear persistence context to force reload

        // Assert
        assertEquals(2, cancelledCount); // Only PENDING and FAILED should be cancelled

        List<PlanExpiryNotification> notifications = 
                notificationRepository.findByAgreementIdOrderByScheduledDateAsc(agreementId);
        
        long cancelledNotifications = notifications.stream()
                .filter(n -> n.getDeliveryStatus() == PlanExpiryNotification.DeliveryStatus.CANCELLED)
                .count();
        long deliveredNotifications = notifications.stream()
                .filter(n -> n.getDeliveryStatus() == PlanExpiryNotification.DeliveryStatus.DELIVERED)
                .count();

        assertEquals(2, cancelledNotifications);
        assertEquals(1, deliveredNotifications);
    }

    @Test
    @DisplayName("Should find retryable failed notifications")
    void testFindRetryableFailedNotifications() {
        // Arrange
        PlanExpiryNotification retryable = createTestNotification("retryable_agreement", LocalDate.now());
        retryable.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.FAILED);
        retryable.setRetryCount(1);
        retryable.setMaxRetries(3);

        PlanExpiryNotification exhausted = createTestNotification("exhausted_agreement", LocalDate.now());
        exhausted.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.FAILED);
        exhausted.setRetryCount(3);
        exhausted.setMaxRetries(3);

        notificationRepository.saveAll(List.of(retryable, exhausted));
        entityManager.flush();

        // Act
        List<PlanExpiryNotification> retryableNotifications = 
                notificationRepository.findRetryableFailedNotifications();

        // Assert
        assertEquals(1, retryableNotifications.size());
        assertEquals("retryable_agreement", retryableNotifications.get(0).getAgreementId());
    }

    private PlanExpiryNotification createTestNotification(String agreementId, LocalDate scheduledDate) {
        return PlanExpiryNotification.builder()
                .tenant(testTenant)
                .agreementId(agreementId)
                .notificationType(NotificationType.PLAN_EXPIRY_REMINDER)
                .scheduledDate(scheduledDate)
                .messageTemplate("test_template")
                .leadTimeDays(7)
                .maxRetries(3)
                .build();
    }
}