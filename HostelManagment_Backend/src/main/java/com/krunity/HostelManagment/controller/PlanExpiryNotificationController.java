package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.enums.NotificationType;
import com.krunity.HostelManagment.model.PlanExpiryNotification;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.service.PlanExpiryNotificationService;
import com.krunity.HostelManagment.util.SecurityUtils;
import com.krunity.HostelManagment.Utils.ApplicationContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Plan Expiry Notification Management
 * Handles notification queries, scheduling, resending, and admin configuration
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications/plan-expiry")
public class PlanExpiryNotificationController {

    @Autowired
    private PlanExpiryNotificationService planExpiryNotificationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ─── Tenant Notification Endpoints ───────────────────────────────────────

    /**
     * Get plan expiry notifications for tenant
     */
    @GetMapping("/{tenantId}")
    @PreAuthorize("hasRole('TENANT') and #tenantId == authentication.principal.userId or hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> getTenantNotifications(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String notificationType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        try {
            log.info("Fetching plan expiry notifications for tenant {} with filters: status={}, type={}, from={}, to={}", 
                    tenantId, status, notificationType, fromDate, toDate);

            // Get tenant user
            User tenant = new User();
            tenant.setUserId(tenantId);

            // Parse status filter
            PlanExpiryNotification.DeliveryStatus deliveryStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    deliveryStatus = PlanExpiryNotification.DeliveryStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(createErrorResponse("INVALID_STATUS", 
                            "Invalid status: " + status + ". Valid values: PENDING, SENT, DELIVERED, FAILED, CANCELLED"));
                }
            }

            // Get notifications with filters
            List<PlanExpiryNotification> notifications = planExpiryNotificationService.getTenantNotifications(tenant, deliveryStatus);

            // Apply additional filters
            if (notificationType != null && !notificationType.isEmpty()) {
                try {
                    NotificationType typeFilter = NotificationType.valueOf(notificationType.toUpperCase());
                    notifications = notifications.stream()
                            .filter(n -> n.getNotificationType() == typeFilter)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(createErrorResponse("INVALID_NOTIFICATION_TYPE", 
                            "Invalid notification type: " + notificationType));
                }
            }

            if (fromDate != null) {
                notifications = notifications.stream()
                        .filter(n -> n.getScheduledDate() == null || !n.getScheduledDate().isBefore(fromDate))
                        .collect(Collectors.toList());
            }

            if (toDate != null) {
                notifications = notifications.stream()
                        .filter(n -> n.getScheduledDate() == null || !n.getScheduledDate().isAfter(toDate))
                        .collect(Collectors.toList());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("notifications", notifications.stream().map(this::createNotificationSummary).collect(Collectors.toList()));
            response.put("count", notifications.size());
            response.put("tenantId", tenantId);

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to fetch tenant notifications for {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("NOTIFICATIONS_RETRIEVAL_FAILED", e.getMessage()));
        }
    }

    /**
     * Get notifications for a specific agreement
     */
    @GetMapping("/agreement/{agreementId}")
    @PreAuthorize("hasRole('TENANT') or hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> getAgreementNotifications(@PathVariable String agreementId) {
        
        try {
            log.info("Fetching plan expiry notifications for agreement {}", agreementId);

            List<PlanExpiryNotification> notifications = planExpiryNotificationService.getAgreementNotifications(agreementId);

            Map<String, Object> response = new HashMap<>();
            response.put("notifications", notifications.stream().map(this::createNotificationSummary).collect(Collectors.toList()));
            response.put("count", notifications.size());
            response.put("agreementId", agreementId);

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to fetch agreement notifications for {}: {}", agreementId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("NOTIFICATIONS_RETRIEVAL_FAILED", e.getMessage()));
        }
    }

    // ─── Notification Scheduling Endpoints ───────────────────────────────────

    /**
     * Schedule new plan expiry notifications
     */
    @PostMapping("/schedule")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> scheduleNotifications(@Valid @RequestBody ScheduleNotificationRequest request) {
        
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Scheduling plan expiry notifications for agreement {} by user {}", 
                    request.getAgreementId(), currentUser.getUserId());

            int scheduledCount = planExpiryNotificationService.scheduleNotificationsForAgreement(
                    request.getAgreementId(), request.getLeadTimes());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("scheduledCount", scheduledCount);
            response.put("agreementId", request.getAgreementId());
            response.put("message", "Successfully scheduled " + scheduledCount + " notifications");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (SecurityException e) {
            log.warn("Unauthorized notification scheduling attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("ACCESS_DENIED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid notification scheduling request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to schedule notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("SCHEDULING_FAILED", e.getMessage()));
        }
    }

    /**
     * Schedule urgent notification
     */
    @PostMapping("/schedule/urgent")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> scheduleUrgentNotification(@Valid @RequestBody UrgentNotificationRequest request) {
        
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Scheduling urgent notification for allotment {} by user {}", 
                    request.getAllotmentId(), currentUser.getUserId());

            boolean sent = planExpiryNotificationService.sendUrgentNotificationForAllotment(
                    request.getAllotmentId(), request.getNotificationType());

            Map<String, Object> response = new HashMap<>();
            if (sent) {
                response.put("status", "success");
                response.put("message", "Urgent notification sent successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to send urgent notification");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            log.error("Failed to send urgent notification: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("URGENT_NOTIFICATION_FAILED", e.getMessage()));
        }
    }

    // ─── Notification Management Endpoints ───────────────────────────────────

    /**
     * Resend failed notification
     */
    @PutMapping("/{id}/resend")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> resendFailedNotification(@PathVariable UUID id) {
        
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Resending notification {} by user {}", id, currentUser.getUserId());

            boolean resent = planExpiryNotificationService.resendNotification(id);

            Map<String, Object> response = new HashMap<>();
            if (resent) {
                response.put("status", "success");
                response.put("notificationId", id);
                response.put("message", "Notification resent successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to resend notification - notification may not exist or not in failed state");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            log.error("Failed to resend notification {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("RESEND_FAILED", e.getMessage()));
        }
    }

    /**
     * Cancel pending notification
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> cancelNotification(@PathVariable UUID id) {
        
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Cancelling notification {} by user {}", id, currentUser.getUserId());

            boolean cancelled = planExpiryNotificationService.cancelNotification(id);

            Map<String, Object> response = new HashMap<>();
            if (cancelled) {
                response.put("status", "success");
                response.put("notificationId", id);
                response.put("message", "Notification cancelled successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to cancel notification - notification may not exist or not cancellable");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            log.error("Failed to cancel notification {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("CANCEL_FAILED", e.getMessage()));
        }
    }

    /**
     * Cancel all notifications for an agreement
     */
    @PutMapping("/agreement/{agreementId}/cancel")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> cancelAgreementNotifications(@PathVariable String agreementId) {
        
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Cancelling all notifications for agreement {} by user {}", agreementId, currentUser.getUserId());

            planExpiryNotificationService.cancelNotificationsForAgreement(agreementId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("agreementId", agreementId);
            response.put("message", "All notifications for agreement cancelled successfully");

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to cancel agreement notifications for {}: {}", agreementId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("CANCEL_AGREEMENT_NOTIFICATIONS_FAILED", e.getMessage()));
        }
    }

    // ─── Admin Endpoints for Configuration ───────────────────────────────────

    /**
     * Get delivery statistics
     */
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<?> getDeliveryStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            log.info("Fetching delivery statistics from {} to {}", startDate, endDate);

            Map<String, Object> statistics = planExpiryNotificationService.getDeliveryStatistics();

            // Add date range to response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("statistics", statistics);
            response.put("dateRange", Map.of(
                    "startDate", startDate != null ? startDate.toString() : "all-time",
                    "endDate", endDate != null ? endDate.toString() : "all-time"
            ));

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to fetch delivery statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("STATISTICS_RETRIEVAL_FAILED", e.getMessage()));
        }
    }

    /**
     * Manually trigger notification processing
     */
    @PostMapping("/admin/process")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<?> manuallyProcessNotifications() {
        
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Manually triggering notification processing by admin {}", currentUser.getUserId());

            planExpiryNotificationService.manuallyProcessNotifications();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Notification processing triggered successfully");
            response.put("triggeredBy", currentUser.getUserId());

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to manually process notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("MANUAL_PROCESSING_FAILED", e.getMessage()));
        }
    }

    /**
     * Get notification configuration
     */
    @GetMapping("/admin/config")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<?> getNotificationConfiguration() {
        
        try {
            Map<String, Object> config = planExpiryNotificationService.getNotificationConfiguration();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("configuration", config);

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to fetch notification configuration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("CONFIG_RETRIEVAL_FAILED", e.getMessage()));
        }
    }

    /**
     * Update notification configuration
     */
    @PutMapping("/admin/config")
     @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<?> updateNotificationConfiguration(@Valid @RequestBody NotificationConfigRequest request) {
        
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Updating notification configuration by admin {}", currentUser.getUserId());

            planExpiryNotificationService.updateNotificationConfiguration(request.toConfigMap());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Notification configuration updated successfully");
            response.put("updatedBy", currentUser.getUserId());

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to update notification configuration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("CONFIG_UPDATE_FAILED", e.getMessage()));
        }
    }

    /**
     * Get pending notifications summary
     */
    @GetMapping("/admin/pending-summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<?> getPendingNotificationsSummary() {
        
        try {
            Map<String, Object> summary = planExpiryNotificationService.getPendingNotificationsSummary();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("summary", summary);

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to fetch pending notifications summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("PENDING_SUMMARY_FAILED", e.getMessage()));
        }
    }

    // ─── Helper Methods ───────────────────────────────────────────────────────

    private Map<String, Object> createNotificationSummary(PlanExpiryNotification notification) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("notificationId", notification.getNotificationId());
        summary.put("agreementId", notification.getAgreementId());
        summary.put("tenantName", notification.getTenant() != null ? notification.getTenant().getDisplayName() : "Unknown");
        summary.put("notificationType", notification.getNotificationType());
        summary.put("scheduledDate", notification.getScheduledDate());
        summary.put("deliveryStatus", notification.getDeliveryStatus());
        summary.put("sentAt", notification.getSentAt());
        summary.put("leadTimeDays", notification.getLeadTimeDays());
        summary.put("retryCount", notification.getRetryCount());
        summary.put("maxRetries", notification.getMaxRetries());
        summary.put("createdAt", notification.getCreatedAt());

        // Include message if available
        if (notification.getActualMessage() != null && !notification.getActualMessage().isEmpty()) {
            summary.put("message", notification.getActualMessage());
        }

        return summary;
    }

    private Map<String, Object> createErrorResponse(String errorCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error", errorCode);
        response.put("message", message);
        response.put("timestamp", java.time.LocalDateTime.now());
        return response;
    }

    // ─── Request/Response DTOs ────────────────────────────────────────────────

    /**
     * DTO for scheduling notification requests
     */
    public static class ScheduleNotificationRequest {
        @NotNull
        private String agreementId;

        private List<Integer> leadTimes; // Days before expiry

        // Getters and setters
        public String getAgreementId() { return agreementId; }
        public void setAgreementId(String agreementId) { this.agreementId = agreementId; }

        public List<Integer> getLeadTimes() { return leadTimes; }
        public void setLeadTimes(List<Integer> leadTimes) { this.leadTimes = leadTimes; }
    }

    /**
     * DTO for urgent notification requests
     */
    public static class UrgentNotificationRequest {
        @NotNull
        private UUID allotmentId;

        @NotNull
        private NotificationType notificationType;

        private String customMessage;

        // Getters and setters
        public UUID getAllotmentId() { return allotmentId; }
        public void setAllotmentId(UUID allotmentId) { this.allotmentId = allotmentId; }

        public NotificationType getNotificationType() { return notificationType; }
        public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }

        public String getCustomMessage() { return customMessage; }
        public void setCustomMessage(String customMessage) { this.customMessage = customMessage; }
    }

    /**
     * DTO for notification configuration requests
     */
    public static class NotificationConfigRequest {
        private Boolean enabled;
        private List<Integer> defaultLeadTimes;
        private Integer maxRetryAttempts;
        private Integer retryDelayMinutes;
        private Integer cleanupDaysThreshold;

        public Map<String, Object> toConfigMap() {
            Map<String, Object> config = new HashMap<>();
            if (enabled != null) config.put("enabled", enabled);
            if (defaultLeadTimes != null) config.put("defaultLeadTimes", defaultLeadTimes);
            if (maxRetryAttempts != null) config.put("maxRetryAttempts", maxRetryAttempts);
            if (retryDelayMinutes != null) config.put("retryDelayMinutes", retryDelayMinutes);
            if (cleanupDaysThreshold != null) config.put("cleanupDaysThreshold", cleanupDaysThreshold);
            return config;
        }

        // Getters and setters
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public List<Integer> getDefaultLeadTimes() { return defaultLeadTimes; }
        public void setDefaultLeadTimes(List<Integer> defaultLeadTimes) { this.defaultLeadTimes = defaultLeadTimes; }

        public Integer getMaxRetryAttempts() { return maxRetryAttempts; }
        public void setMaxRetryAttempts(Integer maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }

        public Integer getRetryDelayMinutes() { return retryDelayMinutes; }
        public void setRetryDelayMinutes(Integer retryDelayMinutes) { this.retryDelayMinutes = retryDelayMinutes; }

        public Integer getCleanupDaysThreshold() { return cleanupDaysThreshold; }
        public void setCleanupDaysThreshold(Integer cleanupDaysThreshold) { this.cleanupDaysThreshold = cleanupDaysThreshold; }
    }
}