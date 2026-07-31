package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.NotificationType;
import com.krunity.HostelManagment.model.PlanExpiryNotification;
import com.krunity.HostelManagment.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for PlanExpiryNotification entity
 * Provides methods for managing plan expiry notification data access
 */
@Repository
public interface PlanExpiryNotificationRepository extends JpaRepository<PlanExpiryNotification, UUID> {

    // ─── Basic Queries ────────────────────────────────────────────────────────

    /**
     * Find notifications by agreement ID
     */
    List<PlanExpiryNotification> findByAgreementIdOrderByScheduledDateAsc(String agreementId);

    /**
     * Find notifications by tenant
     */
    List<PlanExpiryNotification> findByTenantOrderByScheduledDateDesc(User tenant);

    /**
     * Find notifications by tenant and delivery status
     */
    List<PlanExpiryNotification> findByTenantAndDeliveryStatusOrderByScheduledDateDesc(
        User tenant, PlanExpiryNotification.DeliveryStatus deliveryStatus);

    /**
     * Find notifications by notification type
     */
    List<PlanExpiryNotification> findByNotificationTypeOrderByScheduledDateAsc(NotificationType notificationType);

    /**
     * Find notifications by delivery status
     */
    List<PlanExpiryNotification> findByDeliveryStatusOrderByScheduledDateAsc(
        PlanExpiryNotification.DeliveryStatus deliveryStatus);

    // ─── Scheduled Notifications Queries ─────────────────────────────────────

    /**
     * Find notifications that are due for sending (scheduled date is today or past and status is PENDING)
     */
    @Query("SELECT pen FROM PlanExpiryNotification pen " +
           "WHERE pen.scheduledDate <= :currentDate " +
           "AND pen.deliveryStatus = 'PENDING' " +
           "ORDER BY pen.scheduledDate ASC")
    List<PlanExpiryNotification> findDueNotifications(@Param("currentDate") LocalDate currentDate);

    /**
     * Find overdue notifications (scheduled date is in the past and still pending)
     */
    @Query("SELECT pen FROM PlanExpiryNotification pen " +
           "WHERE pen.scheduledDate < :currentDate " +
           "AND pen.deliveryStatus = 'PENDING' " +
           "ORDER BY pen.scheduledDate ASC")
    List<PlanExpiryNotification> findOverdueNotifications(@Param("currentDate") LocalDate currentDate);

    /**
     * Find notifications scheduled for a specific date
     */
    List<PlanExpiryNotification> findByScheduledDateAndDeliveryStatus(
        LocalDate scheduledDate, PlanExpiryNotification.DeliveryStatus deliveryStatus);

    /**
     * Find notifications scheduled within a date range
     */
    @Query("SELECT pen FROM PlanExpiryNotification pen " +
           "WHERE pen.scheduledDate BETWEEN :startDate AND :endDate " +
           "ORDER BY pen.scheduledDate ASC")
    List<PlanExpiryNotification> findByScheduledDateBetween(@Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);

    // ─── Failed Notifications and Retry Queries ─────────────────────────────

    /**
     * Find failed notifications that can be retried
     */
    @Query("SELECT pen FROM PlanExpiryNotification pen " +
           "WHERE pen.deliveryStatus = 'FAILED' " +
           "AND pen.retryCount < pen.maxRetries " +
           "ORDER BY pen.lastRetryAt ASC NULLS FIRST")
    List<PlanExpiryNotification> findRetryableFailedNotifications();

    /**
     * Find notifications that need retry based on time elapsed since last retry
     */
    @Query("SELECT pen FROM PlanExpiryNotification pen " +
           "WHERE pen.deliveryStatus = 'FAILED' " +
           "AND pen.retryCount < pen.maxRetries " +
           "AND (pen.lastRetryAt IS NULL OR pen.lastRetryAt <= :retryThreshold) " +
           "ORDER BY pen.retryCount ASC, pen.lastRetryAt ASC NULLS FIRST")
    List<PlanExpiryNotification> findNotificationsReadyForRetry(@Param("retryThreshold") LocalDateTime retryThreshold);

    /**
     * Find notifications that have exhausted retry attempts
     */
    @Query("SELECT pen FROM PlanExpiryNotification pen " +
           "WHERE pen.deliveryStatus = 'FAILED' " +
           "AND pen.retryCount >= pen.maxRetries")
    List<PlanExpiryNotification> findExhaustedRetryNotifications();

    // ─── Duplicate Prevention Queries ────────────────────────────────────────

    /**
     * Check if notification already exists for agreement and type
     */
    @Query("SELECT COUNT(pen) > 0 FROM PlanExpiryNotification pen " +
           "WHERE pen.agreementId = :agreementId " +
           "AND pen.notificationType = :notificationType " +
           "AND pen.deliveryStatus != 'CANCELLED'")
    boolean existsByAgreementIdAndNotificationType(@Param("agreementId") String agreementId,
                                                  @Param("notificationType") NotificationType notificationType);

    /**
     * Find existing notification for agreement, type and lead time
     */
    Optional<PlanExpiryNotification> findByAgreementIdAndNotificationTypeAndLeadTimeDays(
        String agreementId, NotificationType notificationType, Integer leadTimeDays);

    // ─── Statistics and Reporting Queries ────────────────────────────────────

    /**
     * Count notifications by delivery status
     */
    @Query("SELECT pen.deliveryStatus, COUNT(pen) FROM PlanExpiryNotification pen GROUP BY pen.deliveryStatus")
    List<Object[]> countByDeliveryStatus();

    /**
     * Count notifications by notification type
     */
    @Query("SELECT pen.notificationType, COUNT(pen) FROM PlanExpiryNotification pen GROUP BY pen.notificationType")
    List<Object[]> countByNotificationType();

    /**
     * Get delivery success rate (delivered / total sent)
     */
    @Query("SELECT " +
           "COALESCE(COUNT(CASE WHEN pen.deliveryStatus = 'DELIVERED' THEN 1 END) * 100.0 / " +
           "NULLIF(COUNT(CASE WHEN pen.deliveryStatus IN ('DELIVERED', 'FAILED') THEN 1 END), 0), 0.0) " +
           "FROM PlanExpiryNotification pen")
    Double getDeliverySuccessRate();

    /**
     * Find notifications created within date range
     */
    @Query("SELECT pen FROM PlanExpiryNotification pen " +
           "WHERE pen.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY pen.createdAt DESC")
    List<PlanExpiryNotification> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    // ─── Paginated Queries ───────────────────────────────────────────────────

    /**
     * Find notifications for tenant with pagination
     */
    Page<PlanExpiryNotification> findByTenant(User tenant, Pageable pageable);

    /**
     * Find notifications by delivery status with pagination
     */
    Page<PlanExpiryNotification> findByDeliveryStatus(PlanExpiryNotification.DeliveryStatus deliveryStatus, Pageable pageable);

    // ─── Update Operations ────────────────────────────────────────────────────

    /**
     * Mark notification as sent
     */
    @Modifying
    @Transactional
    @Query("UPDATE PlanExpiryNotification pen SET pen.deliveryStatus = 'SENT', pen.sentAt = :sentAt " +
           "WHERE pen.notificationId = :notificationId")
    int markAsSent(@Param("notificationId") UUID notificationId, @Param("sentAt") LocalDateTime sentAt);

    /**
     * Mark notification as delivered
     */
    @Modifying
    @Transactional
    @Query("UPDATE PlanExpiryNotification pen SET pen.deliveryStatus = 'DELIVERED' " +
           "WHERE pen.notificationId = :notificationId")
    int markAsDelivered(@Param("notificationId") UUID notificationId);

    /**
     * Mark notification as failed and increment retry count
     */
    @Modifying
    @Transactional
    @Query("UPDATE PlanExpiryNotification pen SET pen.deliveryStatus = 'FAILED', " +
           "pen.retryCount = pen.retryCount + 1, pen.lastRetryAt = :retryAt " +
           "WHERE pen.notificationId = :notificationId")
    int markAsFailed(@Param("notificationId") UUID notificationId, @Param("retryAt") LocalDateTime retryAt);

    /**
     * Cancel notifications for an agreement
     */
    @Modifying
    @Transactional
    @Query("UPDATE PlanExpiryNotification pen SET pen.deliveryStatus = 'CANCELLED' " +
           "WHERE pen.agreementId = :agreementId " +
           "AND pen.deliveryStatus IN ('PENDING', 'FAILED')")
    int cancelNotificationsForAgreement(@Param("agreementId") String agreementId);

    /**
     * Cancel notifications for a tenant
     */
    @Modifying
    @Transactional
    @Query("UPDATE PlanExpiryNotification pen SET pen.deliveryStatus = 'CANCELLED' " +
           "WHERE pen.tenant = :tenant " +
           "AND pen.deliveryStatus IN ('PENDING', 'FAILED')")
    int cancelNotificationsForTenant(@Param("tenant") User tenant);

    // ─── Cleanup Operations ──────────────────────────────────────────────────

    /**
     * Delete old delivered notifications (older than specified days)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PlanExpiryNotification pen " +
           "WHERE pen.deliveryStatus = 'DELIVERED' " +
           "AND pen.sentAt < :cutoffDate")
    int deleteOldDeliveredNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete old cancelled notifications
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PlanExpiryNotification pen " +
           "WHERE pen.deliveryStatus = 'CANCELLED' " +
           "AND pen.updatedAt < :cutoffDate")
    int deleteOldCancelledNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    // ─── Business Logic Queries ──────────────────────────────────────────────

    /**
     * Find notifications that require immediate delivery (high priority)
     */
    @Query("SELECT pen FROM PlanExpiryNotification pen " +
           "WHERE pen.deliveryStatus = 'PENDING' " +
           "AND pen.notificationType IN ('URGENT_ACTION_REQUIRED', 'FINAL_NOTICE') " +
           "ORDER BY pen.scheduledDate ASC")
    List<PlanExpiryNotification> findHighPriorityPendingNotifications();

    /**
     * Find recent notifications for a tenant (last 30 days)
     */
    @Query("SELECT pen FROM PlanExpiryNotification pen " +
           "WHERE pen.tenant = :tenant " +
           "AND pen.createdAt >= :thirtyDaysAgo " +
           "ORDER BY pen.createdAt DESC")
    List<PlanExpiryNotification> findRecentTenantNotifications(@Param("tenant") User tenant,
                                                              @Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);

    /**
     * Count pending notifications for a tenant
     */
    @Query("SELECT COUNT(pen) FROM PlanExpiryNotification pen " +
           "WHERE pen.tenant = :tenant " +
           "AND pen.deliveryStatus = 'PENDING'")
    Long countPendingNotificationsForTenant(@Param("tenant") User tenant);

    /**
     * Find notifications scheduled for today
     */
    @Query("SELECT pen FROM PlanExpiryNotification pen " +
           "WHERE pen.scheduledDate = CURRENT_DATE " +
           "AND pen.deliveryStatus = 'PENDING' " +
           "ORDER BY pen.notificationType")
    List<PlanExpiryNotification> findTodaysPendingNotifications();

    /**
     * Find notifications by agreement and delivery status
     */
    List<PlanExpiryNotification> findByAgreementIdAndDeliveryStatus(
        String agreementId, PlanExpiryNotification.DeliveryStatus deliveryStatus);

    // ─── Additional Methods for Controller Support ───────────────────────────

    /**
     * Find notifications by delivery status and scheduled date before
     */
    List<PlanExpiryNotification> findByDeliveryStatusAndScheduledDateBefore(
        PlanExpiryNotification.DeliveryStatus deliveryStatus, LocalDate scheduledDate);

    /**
     * Find notifications by delivery status and scheduled date between
     */
    List<PlanExpiryNotification> findByDeliveryStatusAndScheduledDateBetween(
        PlanExpiryNotification.DeliveryStatus deliveryStatus, LocalDate startDate, LocalDate endDate);

    /**
     * Count notifications by notification type and delivery status
     */
    @Query("SELECT COUNT(pen) FROM PlanExpiryNotification pen " +
           "WHERE pen.notificationType = :notificationType " +
           "AND pen.deliveryStatus = :deliveryStatus")
    Long countByNotificationTypeAndDeliveryStatus(@Param("notificationType") NotificationType notificationType,
                                                 @Param("deliveryStatus") PlanExpiryNotification.DeliveryStatus deliveryStatus);
}