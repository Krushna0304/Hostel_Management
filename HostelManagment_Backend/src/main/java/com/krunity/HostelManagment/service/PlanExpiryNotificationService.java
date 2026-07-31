package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.enums.NotificationType;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.PlanExpiryNotification;
import com.krunity.HostelManagment.model.RoomAllotment;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.repository.PlanExpiryNotificationRepository;
import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for managing plan expiry notifications
 * Handles automated notification scheduling, processing, and delivery
 */
@Slf4j
@Service
public class PlanExpiryNotificationService {

    @Autowired
    private PlanExpiryNotificationRepository notificationRepository;

    @Autowired
    private RoomAllotmentRepository roomAllotmentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AgreementService agreementService;

    // ─── Configuration ────────────────────────────────────────────────────────

    @Value("${app.plan-expiry.lead-times:30,15,7,3,1}")
    private String[] defaultLeadTimes;

    @Value("${app.plan-expiry.enabled:true}")
    private boolean planExpiryNotificationsEnabled;

    @Value("${app.plan-expiry.retry-max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.plan-expiry.cleanup-days:30}")
    private int cleanupDaysThreshold;

    @Value("${app.plan-expiry.retry-delay-minutes:5}")
    private int retryDelayMinutes;

    @Value("${app.plan-expiry.delivery-confirmation-timeout:5}")
    private int deliveryConfirmationTimeoutMinutes;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ─── Scheduled Jobs ───────────────────────────────────────────────────────

    /**
     * Daily scheduled job to process plan expiry notifications
     * Runs every day at 8:00 AM
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void processPlanExpiryNotifications() {
        if (!planExpiryNotificationsEnabled) {
            log.debug("Plan expiry notifications are disabled");
            return;
        }

        log.info("🔔 Starting daily plan expiry notification processing");

        try {
            // Process due notifications
            int dueSent = processDueNotifications();
            
            // Process retry notifications
            int retrySent = processRetryNotifications();
            
            // Schedule new notifications
            int newScheduled = scheduleNewNotifications();
            
            // Cleanup old notifications
            int cleaned = cleanupOldNotifications();

            log.info("✅ Plan expiry notification processing completed - " +
                    "Due: {} sent, Retries: {} sent, New: {} scheduled, Cleaned: {} old notifications",
                    dueSent, retrySent, newScheduled, cleaned);

        } catch (Exception e) {
            log.error("❌ Error processing plan expiry notifications: {}", e.getMessage(), e);
        }
    }

    /**
     * Process notifications that are due for sending today
     */
    @Transactional
    public int processDueNotifications() {
        LocalDate currentDate = LocalDate.now();
        List<PlanExpiryNotification> dueNotifications = notificationRepository.findDueNotifications(currentDate);

        log.info("📅 Found {} due notifications for {}", dueNotifications.size(), currentDate);

        int sentCount = 0;
        for (PlanExpiryNotification notification : dueNotifications) {
            try {
                if (sendPlanExpiryNotification(notification)) {
                    sentCount++;
                }
            } catch (Exception e) {
                log.error("Failed to send due notification {}: {}", 
                        notification.getNotificationId(), e.getMessage());
                notification.markAsFailed();
                notificationRepository.save(notification);
            }
        }

        return sentCount;
    }

    /**
     * Process failed notifications that are ready for retry
     */
    @Transactional
    public int processRetryNotifications() {
        LocalDateTime retryThreshold = LocalDateTime.now().minusMinutes(retryDelayMinutes);
        List<PlanExpiryNotification> retryNotifications = 
            notificationRepository.findNotificationsReadyForRetry(retryThreshold);

        log.info("🔄 Found {} notifications ready for retry", retryNotifications.size());

        int sentCount = 0;
        for (PlanExpiryNotification notification : retryNotifications) {
            try {
                // Skip notifications that have exhausted their retry limit
                if (!notification.canRetry()) {
                    log.warn("Notification {} has exhausted retry limit ({}/{}), marking as failed",
                            notification.getNotificationId(), notification.getRetryCount(), notification.getMaxRetries());
                    notification.markAsFailed();
                    notificationRepository.save(notification);
                    continue;
                }
                if (sendPlanExpiryNotification(notification)) {
                    sentCount++;
                }
            } catch (Exception e) {
                log.error("Failed to retry notification {}: {}", 
                        notification.getNotificationId(), e.getMessage());
                notification.markAsFailed();
                notificationRepository.save(notification);
            }
        }

        return sentCount;
    }

    /**
     * Schedule new notifications for upcoming plan expiries
     */
    @Transactional
    public int scheduleNewNotifications() {
        // Find active agreements that are approaching expiry
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(90); // Look ahead 90 days

        List<RoomAllotment> upcomingExpiries = roomAllotmentRepository.findActiveAllotmentsExpiringBetween(
            startDate, endDate);

        log.info("🗓️ Found {} allotments expiring in the next 90 days", upcomingExpiries.size());

        int scheduledCount = 0;
        for (RoomAllotment allotment : upcomingExpiries) {
            try {
                int scheduled = scheduleNotificationsForAllotment(allotment);
                scheduledCount += scheduled;
            } catch (Exception e) {
                log.error("Failed to schedule notifications for allotment {}: {}", 
                        allotment.getAllotmentId(), e.getMessage());
            }
        }

        return scheduledCount;
    }

    /**
     * Schedule expiry notifications for a specific allotment
     */
    @Transactional
    public int scheduleNotificationsForAllotment(RoomAllotment allotment) {
        if (allotment == null || allotment.getEndDate() == null) {
            log.warn("Cannot schedule notifications: allotment or end date is null");
            return 0;
        }

        // Get agreement details
        Agreement agreement = null;
        try {
            Optional<Agreement> agreementOpt = agreementService.getAgreementById(allotment.getAgreementId());
            agreement = agreementOpt.orElse(null);
        } catch (Exception e) {
            log.warn("Could not fetch agreement {} for allotment {}: {}", 
                    allotment.getAgreementId(), allotment.getAllotmentId(), e.getMessage());
            return 0;
        }

        if (agreement == null) {
            log.warn("Agreement {} not found for allotment {}", 
                    allotment.getAgreementId(), allotment.getAllotmentId());
            return 0;
        }

        LocalDate endDate = allotment.getEndDate();
        User tenant = allotment.getTenant();

        // Get lead times from agreement plan or use defaults
        int[] leadTimes = getLeadTimesForPlan(agreement);

        int scheduledCount = 0;
        for (int leadTime : leadTimes) {
            NotificationType notificationType = getNotificationTypeForLeadTime(leadTime);
            LocalDate scheduledDate = endDate.minusDays(leadTime);

            // Skip if scheduled date is in the past
            if (scheduledDate.isBefore(LocalDate.now())) {
                continue;
            }

            // Check if notification already exists
            if (notificationRepository.existsByAgreementIdAndNotificationType(
                    allotment.getAgreementId(), notificationType)) {
                continue;
            }

            // Create notification record
            PlanExpiryNotification notification = createNotificationRecord(
                allotment, agreement, tenant, notificationType, scheduledDate, leadTime);

            notificationRepository.save(notification);
            scheduledCount++;

            log.debug("📝 Scheduled {} notification for agreement {} on {} (lead time: {} days)",
                    notificationType, agreement.getId(), scheduledDate, leadTime);
        }

        return scheduledCount;
    }

    /**
     * Create notification record with proper message variables
     */
    private PlanExpiryNotification createNotificationRecord(RoomAllotment allotment, Agreement agreement, 
                                                          User tenant, NotificationType notificationType,
                                                          LocalDate scheduledDate, int leadTimeDays) {
        
        PlanExpiryNotification notification = PlanExpiryNotification.builder()
            .tenant(tenant)
            .roomAllotment(allotment)
            .agreementId(allotment.getAgreementId())
            .notificationType(notificationType)
            .scheduledDate(scheduledDate)
            .messageTemplate(getMessageTemplate(notificationType))
            .leadTimeDays(leadTimeDays)
            .maxRetries(maxRetryAttempts)
            .build();

        // Set message variables
        Map<String, String> variables = createMessageVariables(allotment, agreement, tenant, leadTimeDays);
        notification.setMessageVariables(variables);

        return notification;
    }

    /**
     * Send plan expiry notification via SMS/Email with enhanced retry logic
     */
    @Transactional
    public boolean sendPlanExpiryNotification(PlanExpiryNotification notification) {
        try {
            log.info("📤 Sending {} notification to tenant {} for agreement {} (attempt: {}/{})",
                    notification.getNotificationType(),
                    notification.getTenant().getDisplayName(),
                    notification.getAgreementId(),
                    notification.getRetryCount() + 1,
                    notification.getMaxRetries());

            // Generate actual message from template and variables
            String message = renderMessageTemplate(notification);
            notification.setActualMessage(message);

            // Mark as sent first (before attempting delivery)
            notification.markAsSent();
            
            notificationRepository.save(notification);

            // Send via notification service with delivery tracking callback
            String title = notification.getNotificationType().getDisplayName();
            
            boolean deliverySuccessful = sendNotificationWithDeliveryTracking(
                notification.getTenant(),
                notification.getNotificationType(),
                title,
                message,
                notification.getMessageVariables(),
                notification.getNotificationId()
            );

            if (deliverySuccessful) {
                // Mark as delivered only if we get confirmation
                notification.markAsDelivered();
                notificationRepository.save(notification);
                
                log.info("✅ Successfully sent {} notification to tenant {}",
                        notification.getNotificationType(),
                        notification.getTenant().getDisplayName());
                return true;
            } else {
                // Mark as failed if delivery was not successful
                notification.markAsFailed();
                notificationRepository.save(notification);
                
                log.warn("⚠️ Notification {} marked as failed due to delivery issues (attempt: {}/{})",
                        notification.getNotificationId(),
                        notification.getRetryCount() + 1,
                        notification.getMaxRetries());
                return false;
            }

        } catch (Exception e) {
            log.error("❌ Failed to send notification {}: {} (attempt: {}/{})", 
                    notification.getNotificationId(), 
                    e.getMessage(),
                    notification.getRetryCount() + 1,
                    notification.getMaxRetries());

            notification.markAsFailed();
            notificationRepository.save(notification);

            return false;
        }
    }

    /**
     * Send notification with delivery tracking integration
     */
    private boolean sendNotificationWithDeliveryTracking(User recipient, NotificationType notificationType,
                                                        String title, String message, 
                                                        Map<String, String> variables, UUID notificationId) {
        try {
            // Create enhanced variables map with delivery tracking info
            Map<String, String> enhancedVariables = new HashMap<>(variables != null ? variables : new HashMap<>());
            enhancedVariables.put("notificationId", notificationId.toString());
            enhancedVariables.put("deliveryCallbackUrl", getDeliveryCallbackUrl());

            // Send via notification service
            notificationService.sendNotification(
                recipient,
                notificationType,
                title,
                message,
                enhancedVariables
            );

            // For now, assume delivery is successful immediately
            // In a real implementation, this would return true only after receiving delivery confirmation
            return true;

        } catch (Exception e) {
            log.error("Failed to send notification with delivery tracking: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get delivery callback URL for webhook notifications
     */
    private String getDeliveryCallbackUrl() {
        // This would be configured based on your application's webhook endpoint
        return "https://your-app-domain.com/api/v1/notifications/delivery-callback";
    }

    /**
     * Render message template with variables
     */
    private String renderMessageTemplate(PlanExpiryNotification notification) {
        String template = getMessageTemplateContent(notification.getNotificationType());
        Map<String, String> variables = notification.getMessageVariables();

        if (variables == null || variables.isEmpty()) {
            return template;
        }

        String message = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (entry.getValue() != null) {
                message = message.replace(placeholder, entry.getValue());
            }
        }

        return message;
    }

    /**
     * Get message template content for notification type
     */
    private String getMessageTemplateContent(NotificationType notificationType) {
        switch (notificationType) {
            case PLAN_EXPIRY_REMINDER:
                return "Dear {tenantName},\n\n" +
                       "Your hostel agreement for room {roomNumber} expires on {endDate} ({daysRemaining} days remaining).\n\n" +
                       "Plan: {planName}\n" +
                       "Agreement ID: {agreementId}\n\n" +
                       "Please take action:\n" +
                       "• Submit settlement request if leaving\n" +
                       "• Request extension to continue staying\n\n" +
                       "Login to your portal: {portalUrl}\n\n" +
                       "- Hostel Management";

            case SETTLEMENT_WINDOW_OPEN:
                return "Dear {tenantName},\n\n" +
                       "Settlement window is now open for room {roomNumber}. Your agreement expires on {endDate}.\n\n" +
                       "Plan: {planName}\n" +
                       "Days remaining: {daysRemaining}\n\n" +
                       "You can now submit a settlement request through your tenant portal.\n" +
                       "Portal: {portalUrl}\n\n" +
                       "- Hostel Management";

            case URGENT_ACTION_REQUIRED:
                return "URGENT: Dear {tenantName},\n\n" +
                       "Your agreement for room {roomNumber} expires in {daysRemaining} days ({endDate}).\n\n" +
                       "Plan: {planName}\n" +
                       "Agreement ID: {agreementId}\n\n" +
                       "IMMEDIATE ACTION REQUIRED:\n" +
                       "Please submit settlement request or extension request immediately.\n\n" +
                       "Portal: {portalUrl}\n\n" +
                       "- Hostel Management";

            case FINAL_NOTICE:
                return "FINAL NOTICE: Dear {tenantName},\n\n" +
                       "Your agreement for room {roomNumber} expires TOMORROW ({endDate}).\n\n" +
                       "Plan: {planName}\n" +
                       "Agreement ID: {agreementId}\n\n" +
                       "This is your final notice. Please contact management immediately.\n\n" +
                       "Portal: {portalUrl}\n" +
                       "Contact: +91-XXXXXXXXXX\n\n" +
                       "- Hostel Management";

            default:
                return "Dear {tenantName},\n\n" +
                       "Please check your tenant portal for important updates regarding room {roomNumber}.\n\n" +
                       "Portal: {portalUrl}\n\n" +
                       "- Hostel Management";
        }
    }

    /**
     * Create message variables for template rendering
     */
    private Map<String, String> createMessageVariables(RoomAllotment allotment, Agreement agreement, 
                                                      User tenant, int leadTimeDays) {
        Map<String, String> variables = new HashMap<>();

        variables.put("tenantName", tenant.getDisplayName());
        variables.put("roomNumber", allotment.getRoom() != null ? allotment.getRoom().getRoomNumber() : "N/A");
        variables.put("endDate", allotment.getEndDate() != null ? allotment.getEndDate().format(DATE_FORMATTER) : "N/A");
        variables.put("daysRemaining", String.valueOf(leadTimeDays));
        variables.put("agreementId", agreement.getId());
        variables.put("portalUrl", "https://hostel-management-dashboard.onrender.com/tenant/dashboard");

        // Additional plan details if available
        if (agreement.getPlanSnapshot() != null) {
            variables.put("planName", agreement.getPlanSnapshot().getPlanName() != null ? 
                         agreement.getPlanSnapshot().getPlanName() : "N/A");
        } else {
            variables.put("planName", "N/A");
        }

        return variables;
    }

    /**
     * Get lead times for a specific plan (from agreement or defaults)
     */
    private int[] getLeadTimesForPlan(Agreement agreement) {
        // For now, use default lead times
        // In future, this could be configured per plan
        return Arrays.stream(defaultLeadTimes)
                     .mapToInt(Integer::parseInt)
                     .toArray();
    }

    /**
     * Get notification type based on lead time
     */
    private NotificationType getNotificationTypeForLeadTime(int leadTimeDays) {
        if (leadTimeDays >= 30) {
            return NotificationType.PLAN_EXPIRY_REMINDER;
        } else if (leadTimeDays >= 7) {
            return NotificationType.SETTLEMENT_WINDOW_OPEN;
        } else if (leadTimeDays >= 2) {
            return NotificationType.URGENT_ACTION_REQUIRED;
        } else {
            return NotificationType.FINAL_NOTICE;
        }
    }

    /**
     * Get message template name for notification type
     */
    private String getMessageTemplate(NotificationType notificationType) {
        return notificationType.getDefaultTemplateName();
    }

    /**
     * Clean up old delivered/cancelled notifications
     */
    @Transactional
    public int cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupDaysThreshold);

        int deliveredCleaned = notificationRepository.deleteOldDeliveredNotifications(cutoffDate);
        int cancelledCleaned = notificationRepository.deleteOldCancelledNotifications(cutoffDate);

        int totalCleaned = deliveredCleaned + cancelledCleaned;
        if (totalCleaned > 0) {
            log.info("🧹 Cleaned up {} old notifications (delivered: {}, cancelled: {})",
                    totalCleaned, deliveredCleaned, cancelledCleaned);
        }

        return totalCleaned;
    }

    // ─── Public API Methods ───────────────────────────────────────────────────

    /**
     * Schedule notifications for a new agreement
     */
    public void scheduleExpiryNotifications(Agreement agreement, RoomAllotment allotment) {
        if (!planExpiryNotificationsEnabled) {
            return;
        }

        try {
            int scheduled = scheduleNotificationsForAllotment(allotment);
            log.info("📅 Scheduled {} expiry notifications for agreement {}", 
                    scheduled, agreement.getId());
        } catch (Exception e) {
            log.error("Failed to schedule expiry notifications for agreement {}: {}", 
                    agreement.getId(), e.getMessage());
        }
    }

    /**
     * Cancel notifications for an agreement (e.g., when settlement is completed)
     */
    @Transactional
    public void cancelNotificationsForAgreement(String agreementId) {
        try {
            int cancelled = notificationRepository.cancelNotificationsForAgreement(agreementId);
            if (cancelled > 0) {
                log.info("🚫 Cancelled {} notifications for agreement {}", cancelled, agreementId);
            }
        } catch (Exception e) {
            log.error("Failed to cancel notifications for agreement {}: {}", agreementId, e.getMessage());
        }
    }

    /**
     * Cancel notifications for a tenant
     */
    @Transactional
    public void cancelNotificationsForTenant(User tenant) {
        try {
            int cancelled = notificationRepository.cancelNotificationsForTenant(tenant);
            if (cancelled > 0) {
                log.info("🚫 Cancelled {} notifications for tenant {}", cancelled, tenant.getDisplayName());
            }
        } catch (Exception e) {
            log.error("Failed to cancel notifications for tenant {}: {}", tenant.getDisplayName(), e.getMessage());
        }
    }

    /**
     * Get notifications for a tenant with optional filtering
     */
    public List<PlanExpiryNotification> getTenantNotifications(User tenant, 
                                                             PlanExpiryNotification.DeliveryStatus status) {
        if (status != null) {
            return notificationRepository.findByTenantAndDeliveryStatusOrderByScheduledDateDesc(tenant, status);
        } else {
            return notificationRepository.findByTenantOrderByScheduledDateDesc(tenant);
        }
    }

    /**
     * Get notifications for an agreement
     */
    public List<PlanExpiryNotification> getAgreementNotifications(String agreementId) {
        return notificationRepository.findByAgreementIdOrderByScheduledDateAsc(agreementId);
    }

    /**
     * Manually trigger notification processing (for testing or admin use)
     */
    public void manuallyProcessNotifications() {
        log.info("🔧 Manually triggering notification processing");
        processPlanExpiryNotifications();
    }

    /**
     * Resend a specific notification
     */
    @Transactional
    public boolean resendNotification(UUID notificationId) {
        Optional<PlanExpiryNotification> optNotification = notificationRepository.findById(notificationId);
        
        if (optNotification.isEmpty()) {
            log.warn("Notification {} not found for resend", notificationId);
            return false;
        }

        PlanExpiryNotification notification = optNotification.get();
        
        // Reset status to allow resending
        notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.PENDING);
        notification.setRetryCount(0);
        notification.setLastRetryAt(null);
        notificationRepository.save(notification);

        // Attempt to send
        return sendPlanExpiryNotification(notification);
    }

    /**
     * Get delivery statistics
     */
    public Map<String, Object> getDeliveryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Delivery status counts
        List<Object[]> statusCounts = notificationRepository.countByDeliveryStatus();
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCounts) {
            statusMap.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("deliveryStatusCounts", statusMap);

        // Notification type counts
        List<Object[]> typeCounts = notificationRepository.countByNotificationType();
        Map<String, Long> typeMap = new HashMap<>();
        for (Object[] row : typeCounts) {
            typeMap.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("notificationTypeCounts", typeMap);

        // Success rate
        Double successRate = notificationRepository.getDeliverySuccessRate();
        stats.put("deliverySuccessRate", successRate != null ? successRate : 0.0);

        // Today's notifications
        List<PlanExpiryNotification> todaysNotifications = notificationRepository.findTodaysPendingNotifications();
        stats.put("todaysPendingCount", todaysNotifications.size());

        return stats;
    }

    /**
     * Force immediate notification for urgent cases
     */
    @Transactional
    public boolean sendUrgentNotification(RoomAllotment allotment, NotificationType notificationType) {
        try {
            Agreement agreement = agreementService.getAgreementById(allotment.getAgreementId()).get();
            if (agreement == null) {
                return false;
            }

            PlanExpiryNotification notification = createNotificationRecord(
                allotment, agreement, allotment.getTenant(), notificationType, LocalDate.now(), 0);

            notificationRepository.save(notification);
            return sendPlanExpiryNotification(notification);

        } catch (Exception e) {
            log.error("Failed to send urgent notification: {}", e.getMessage());
            return false;
        }
    }

    // ─── Delivery Status Tracking API Methods ────────────────────────────────

    /**
     * Update delivery status from external delivery service webhooks/callbacks
     */
    @Transactional
    public boolean updateDeliveryStatus(UUID notificationId, PlanExpiryNotification.DeliveryStatus newStatus, 
                                      String deliveryReference, String failureReason) {
        try {
            Optional<PlanExpiryNotification> optNotification = notificationRepository.findById(notificationId);
            
            if (optNotification.isEmpty()) {
                log.warn("Notification {} not found for delivery status update", notificationId);
                return false;
            }

            PlanExpiryNotification notification = optNotification.get();
            
            // Validate status transition
            if (!isValidStatusTransition(notification.getDeliveryStatus(), newStatus)) {
                log.warn("Invalid status transition for notification {}: {} -> {}", 
                        notificationId, notification.getDeliveryStatus(), newStatus);
                return false;
            }

            // Update status based on new delivery status
            switch (newStatus) {
                case DELIVERED:
                    notification.markAsDelivered();
                    log.info("✅ Notification {} marked as DELIVERED via external callback", notificationId);
                    break;
                    
                case FAILED:
                    notification.markAsFailed();
                    // Store failure reason if provided
                    if (failureReason != null) {
                        notification.addMessageVariable("failureReason", failureReason);
                    }
                    log.warn("❌ Notification {} marked as FAILED via external callback: {}", 
                            notificationId, failureReason);
                    break;
                    
                case SENT:
                    notification.markAsSent();
                    log.info("📤 Notification {} marked as SENT via external callback", notificationId);
                    break;
                    
                case CANCELLED:
                    notification.markAsCancelled();
                    log.info("🚫 Notification {} marked as CANCELLED via external callback", notificationId);
                    break;
                    
                default:
                    log.warn("Unsupported delivery status update: {}", newStatus);
                    return false;
            }

            // Store delivery reference if provided
            if (deliveryReference != null) {
                notification.addMessageVariable("deliveryReference", deliveryReference);
            }

            notificationRepository.save(notification);
            return true;

        } catch (Exception e) {
            log.error("Failed to update delivery status for notification {}: {}", notificationId, e.getMessage());
            return false;
        }
    }

    /**
     * Validate if status transition is allowed
     */
    private boolean isValidStatusTransition(PlanExpiryNotification.DeliveryStatus currentStatus, 
                                          PlanExpiryNotification.DeliveryStatus newStatus) {
        // Define allowed transitions
        switch (currentStatus) {
            case PENDING:
                return newStatus == PlanExpiryNotification.DeliveryStatus.SENT || 
                       newStatus == PlanExpiryNotification.DeliveryStatus.FAILED ||
                       newStatus == PlanExpiryNotification.DeliveryStatus.CANCELLED;
                       
            case SENT:
                return newStatus == PlanExpiryNotification.DeliveryStatus.DELIVERED ||
                       newStatus == PlanExpiryNotification.DeliveryStatus.FAILED;
                       
            case FAILED:
                return newStatus == PlanExpiryNotification.DeliveryStatus.SENT || // Retry
                       newStatus == PlanExpiryNotification.DeliveryStatus.CANCELLED;
                       
            case DELIVERED:
            case CANCELLED:
                return false; // Terminal states
                
            default:
                return false;
        }
    }

    /**
     * Bulk update delivery status for multiple notifications (for batch processing)
     */
    @Transactional
    public Map<UUID, Boolean> bulkUpdateDeliveryStatus(Map<UUID, PlanExpiryNotification.DeliveryStatus> statusUpdates) {
        Map<UUID, Boolean> results = new HashMap<>();
        
        for (Map.Entry<UUID, PlanExpiryNotification.DeliveryStatus> entry : statusUpdates.entrySet()) {
            UUID notificationId = entry.getKey();
            PlanExpiryNotification.DeliveryStatus newStatus = entry.getValue();
            
            boolean updated = updateDeliveryStatus(notificationId, newStatus, null, null);
            results.put(notificationId, updated);
        }
        
        log.info("Bulk delivery status update completed: {} notifications processed, {} successful",
                statusUpdates.size(), results.values().stream().mapToInt(b -> b ? 1 : 0).sum());
        
        return results;
    }

    /**
     * Get detailed delivery tracking information for a notification
     */
    public Map<String, Object> getDeliveryTrackingInfo(UUID notificationId) {
        Optional<PlanExpiryNotification> optNotification = notificationRepository.findById(notificationId);
        
        if (optNotification.isEmpty()) {
            return null;
        }

        PlanExpiryNotification notification = optNotification.get();
        Map<String, Object> trackingInfo = new HashMap<>();
        
        trackingInfo.put("notificationId", notification.getNotificationId());
        trackingInfo.put("deliveryStatus", notification.getDeliveryStatus());
        trackingInfo.put("statusDescription", notification.getStatusDescription());
        trackingInfo.put("scheduledDate", notification.getScheduledDate());
        trackingInfo.put("sentAt", notification.getSentAt());
        trackingInfo.put("retryCount", notification.getRetryCount());
        trackingInfo.put("maxRetries", notification.getMaxRetries());
        trackingInfo.put("canRetry", notification.canRetry());
        trackingInfo.put("nextRetryTime", notification.getNextRetryTime());
        trackingInfo.put("isTerminal", notification.isTerminal());
        
        // Add delivery reference if available
        String deliveryRef = notification.getMessageVariable("deliveryReference");
        if (deliveryRef != null) {
            trackingInfo.put("deliveryReference", deliveryRef);
        }
        
        // Add failure reason if available
        String failureReason = notification.getMessageVariable("failureReason");
        if (failureReason != null) {
            trackingInfo.put("failureReason", failureReason);
        }
        
        return trackingInfo;
    }

    /**
     * Get delivery statistics with detailed breakdowns
     */
    public Map<String, Object> getDetailedDeliveryStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = getDeliveryStatistics();
        
        // Add time-based statistics
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            
            List<PlanExpiryNotification> periodNotifications = 
                notificationRepository.findByCreatedAtBetween(start, end);
            
            Map<String, Long> periodStatusCounts = periodNotifications.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    n -> n.getDeliveryStatus().toString(),
                    java.util.stream.Collectors.counting()
                ));
            
            stats.put("periodStatistics", Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "totalNotifications", periodNotifications.size(),
                "statusCounts", periodStatusCounts
            ));
        }
        
        // Add retry statistics
        List<PlanExpiryNotification> retryableNotifications = 
            notificationRepository.findRetryableFailedNotifications();
        List<PlanExpiryNotification> exhaustedNotifications = 
            notificationRepository.findExhaustedRetryNotifications();
            
        stats.put("retryStatistics", Map.of(
            "retryableFailures", retryableNotifications.size(),
            "exhaustedRetries", exhaustedNotifications.size()
        ));
        
        return stats;
    }

    /**
     * Mark notifications as cancelled for completed agreements (to prevent unnecessary notifications)
     */
    @Transactional
    public int cancelNotificationsForCompletedAgreement(String agreementId, String reason) {
        try {
            int cancelled = notificationRepository.cancelNotificationsForAgreement(agreementId);
            
            if (cancelled > 0) {
                log.info("🚫 Cancelled {} notifications for completed agreement {} - Reason: {}", 
                        cancelled, agreementId, reason);
            }
            
            return cancelled;
            
        } catch (Exception e) {
            log.error("Failed to cancel notifications for agreement {}: {}", agreementId, e.getMessage());
            return 0;
        }
    }

    /**
     * Configure lead times for specific plan types
     */
    public void configurePlanLeadTimes(String planType, int[] leadTimes) {
        // In future implementation, this could store custom lead times per plan type
        // For now, log the configuration attempt
        log.info("📋 Plan lead time configuration requested for plan type '{}': {}",
                planType, Arrays.toString(leadTimes));
    }

    /**
     * Get notification configuration summary
     */
    public Map<String, Object> getNotificationConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("enabled", planExpiryNotificationsEnabled);
        config.put("leadTimes", Arrays.toString(defaultLeadTimes));
        config.put("maxRetryAttempts", maxRetryAttempts);
        config.put("cleanupDaysThreshold", cleanupDaysThreshold);
        config.put("retryDelayMinutes", retryDelayMinutes);
        config.put("deliveryConfirmationTimeoutMinutes", deliveryConfirmationTimeoutMinutes);
        
        return config;
    }

    /**
     * Health check for notification system
     */
    public Map<String, Object> getNotificationSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Check database connectivity
            long totalNotifications = notificationRepository.count();
            
            // Check pending notifications count
            List<PlanExpiryNotification> pendingNotifications = 
                notificationRepository.findByDeliveryStatusOrderByScheduledDateAsc(
                    PlanExpiryNotification.DeliveryStatus.PENDING);
            
            // Check failed notifications that need attention
            List<PlanExpiryNotification> failedNotifications = 
                notificationRepository.findExhaustedRetryNotifications();
            
            health.put("status", "HEALTHY");
            health.put("totalNotifications", totalNotifications);
            health.put("pendingNotifications", pendingNotifications.size());
            health.put("failedNotifications", failedNotifications.size());
            health.put("systemEnabled", planExpiryNotificationsEnabled);
            health.put("lastCheck", LocalDateTime.now());
            
            // Mark as unhealthy if too many failed notifications
            if (failedNotifications.size() > 50) {
                health.put("status", "UNHEALTHY");
                health.put("reason", "Too many failed notifications: " + failedNotifications.size());
            }
            
        } catch (Exception e) {
            health.put("status", "ERROR");
            health.put("reason", e.getMessage());
            health.put("lastCheck", LocalDateTime.now());
        }
        
        return health;
    }

    /**
     * Process delivery status confirmations from message queue or batch jobs
     */
    @Transactional
    public void processDeliveryStatusConfirmations() {
        try {
            // Find notifications that are marked as SENT but haven't been confirmed as DELIVERED
            // This could integrate with your SMS/Email provider's delivery reports
            
            List<PlanExpiryNotification> sentNotifications = 
                notificationRepository.findByDeliveryStatusOrderByScheduledDateAsc(PlanExpiryNotification.DeliveryStatus.SENT);
            
            // Check delivery status with external providers (placeholder for actual integration)
            for (PlanExpiryNotification notification : sentNotifications) {
                if (isDeliveryConfirmed(notification)) {
                    notification.markAsDelivered();
                    notificationRepository.save(notification);
                    
                    log.debug("✅ Confirmed delivery for notification {}", notification.getNotificationId());
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to process delivery status confirmations: {}", e.getMessage());
        }
    }

    /**
     * Placeholder method for checking delivery confirmation with external providers
     * In a real implementation, this would integrate with SMS/Email provider APIs
     */
    private boolean isDeliveryConfirmed(PlanExpiryNotification notification) {
        // This would integrate with your SMS/Email provider's delivery status API
        // Use configurable timeout for delivery confirmation
        
        if (notification.getSentAt() == null) {
            return false;
        }
        
        LocalDateTime confirmationTimeout = LocalDateTime.now().minusMinutes(deliveryConfirmationTimeoutMinutes);
        return notification.getSentAt().isBefore(confirmationTimeout);
    }

    // ─── Additional API Methods for Controller Support ────────────────────────

    /**
     * Schedule notifications for a specific agreement with custom lead times
     */
    @Transactional
    public int scheduleNotificationsForAgreement(String agreementId, List<Integer> customLeadTimes) {
        try {
            // Find room allotment for the agreement
            Optional<RoomAllotment> optAllotment = roomAllotmentRepository.findByAgreementId(agreementId);
            if (optAllotment.isEmpty()) {
                log.warn("No allotment found for agreement {}", agreementId);
                return 0;
            }

            RoomAllotment allotment = optAllotment.get();
            
            // Use custom lead times if provided, otherwise use defaults
            int[] leadTimes;
            if (customLeadTimes != null && !customLeadTimes.isEmpty()) {
                leadTimes = customLeadTimes.stream().mapToInt(Integer::intValue).toArray();
            } else {
                leadTimes = Arrays.stream(defaultLeadTimes).mapToInt(Integer::parseInt).toArray();
            }

            // Schedule notifications with the specified lead times
            LocalDate endDate = allotment.getEndDate();
            if (endDate == null) {
                log.warn("No end date found for allotment {}", allotment.getAllotmentId());
                return 0;
            }

            int scheduledCount = 0;
            User tenant = allotment.getTenant();
            
            for (int leadTime : leadTimes) {
                NotificationType notificationType = getNotificationTypeForLeadTime(leadTime);
                LocalDate scheduledDate = endDate.minusDays(leadTime);

                // Skip if scheduled date is in the past
                if (scheduledDate.isBefore(LocalDate.now())) {
                    continue;
                }

                // Check if notification already exists
                if (notificationRepository.existsByAgreementIdAndNotificationType(agreementId, notificationType)) {
                    continue;
                }

                // Create and save notification
                Agreement agreement = agreementService.getAgreementById(agreementId).orElse(null);
                if (agreement != null) {
                    PlanExpiryNotification notification = createNotificationRecord(
                        allotment, agreement, tenant, notificationType, scheduledDate, leadTime);
                    notificationRepository.save(notification);
                    scheduledCount++;
                }
            }

            return scheduledCount;
            
        } catch (Exception e) {
            log.error("Failed to schedule notifications for agreement {}: {}", agreementId, e.getMessage());
            return 0;
        }
    }

    /**
     * Send urgent notification for a specific allotment
     */
    @Transactional
    public boolean sendUrgentNotificationForAllotment(UUID allotmentId, NotificationType notificationType) {
        try {
            Optional<RoomAllotment> optAllotment = roomAllotmentRepository.findById(allotmentId);
            if (optAllotment.isEmpty()) {
                log.warn("Allotment {} not found for urgent notification", allotmentId);
                return false;
            }

            RoomAllotment allotment = optAllotment.get();
            Agreement agreement = agreementService.getAgreementById(allotment.getAgreementId()).orElse(null);
            
            if (agreement == null) {
                log.warn("Agreement {} not found for allotment {}", allotment.getAgreementId(), allotmentId);
                return false;
            }

            return sendUrgentNotification(allotment, notificationType);
            
        } catch (Exception e) {
            log.error("Failed to send urgent notification for allotment {}: {}", allotmentId, e.getMessage());
            return false;
        }
    }

    /**
     * Cancel a specific notification
     */
    @Transactional
    public boolean cancelNotification(UUID notificationId) {
        try {
            Optional<PlanExpiryNotification> optNotification = notificationRepository.findById(notificationId);
            if (optNotification.isEmpty()) {
                log.warn("Notification {} not found for cancellation", notificationId);
                return false;
            }

            PlanExpiryNotification notification = optNotification.get();
            
            // Check if notification can be cancelled
            if (notification.getDeliveryStatus() == PlanExpiryNotification.DeliveryStatus.DELIVERED ||
                notification.getDeliveryStatus() == PlanExpiryNotification.DeliveryStatus.CANCELLED) {
                log.warn("Notification {} cannot be cancelled - current status: {}", 
                        notificationId, notification.getDeliveryStatus());
                return false;
            }

            notification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.CANCELLED);
            notification.setUpdatedAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("🚫 Cancelled notification {}", notificationId);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to cancel notification {}: {}", notificationId, e.getMessage());
            return false;
        }
    }

    /**
     * Update notification configuration
     */
    public void updateNotificationConfiguration(Map<String, Object> configUpdates) {
        // In a real implementation, this would update configuration values
        // For now, log the configuration updates
        log.info("📋 Notification configuration update requested: {}", configUpdates);
        
        // You could update application properties or database configuration here
        configUpdates.forEach((key, value) -> {
            log.info("Config update: {} = {}", key, value);
        });
    }

    /**
     * Get summary of pending notifications
     */
    public Map<String, Object> getPendingNotificationsSummary() {
        try {
            LocalDate today = LocalDate.now();
            
            // Get pending notifications due today
            List<PlanExpiryNotification> dueToday = notificationRepository.findDueNotifications(today);
            
            // Get overdue notifications (should have been sent but are still pending)
            List<PlanExpiryNotification> overdue = notificationRepository
                    .findByDeliveryStatusAndScheduledDateBefore(
                            PlanExpiryNotification.DeliveryStatus.PENDING, today);
            
            // Get failed notifications that can be retried
            List<PlanExpiryNotification> retryable = notificationRepository.findRetryableFailedNotifications();
            
            // Get notifications scheduled for next 7 days
            LocalDate nextWeek = today.plusDays(7);
            List<PlanExpiryNotification> upcoming = notificationRepository
                    .findByDeliveryStatusAndScheduledDateBetween(
                            PlanExpiryNotification.DeliveryStatus.PENDING, today.plusDays(1), nextWeek);

            Map<String, Object> summary = new HashMap<>();
            summary.put("dueToday", dueToday.size());
            summary.put("overdue", overdue.size());
            summary.put("retryable", retryable.size());
            summary.put("upcomingWeek", upcoming.size());
            summary.put("totalPending", dueToday.size() + overdue.size() + upcoming.size());
            
            // Add breakdown by notification type
            Map<String, Long> typeBreakdown = new HashMap<>();
            for (NotificationType type : NotificationType.values()) {
                long count = notificationRepository.countByNotificationTypeAndDeliveryStatus(
                        type, PlanExpiryNotification.DeliveryStatus.PENDING);
                if (count > 0) {
                    typeBreakdown.put(type.toString(), count);
                }
            }
            summary.put("typeBreakdown", typeBreakdown);
            
            return summary;
            
        } catch (Exception e) {
            log.error("Failed to get pending notifications summary: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}