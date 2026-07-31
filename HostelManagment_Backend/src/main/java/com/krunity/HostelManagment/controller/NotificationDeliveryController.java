package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.model.PlanExpiryNotification;
import com.krunity.HostelManagment.service.PlanExpiryNotificationService;
import com.krunity.HostelManagment.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for notification delivery status tracking
 * Handles webhooks and API calls from external delivery services
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationDeliveryController {

    @Autowired
    private PlanExpiryNotificationService planExpiryNotificationService;

    // ─── Delivery Status Webhook Endpoints ───────────────────────────────────

    /**
     * Webhook endpoint for SMS/Email delivery status updates
     * Called by external delivery services (Twilio, SendGrid, etc.)
     */
    @PostMapping("/delivery-callback")
    public ResponseEntity<Map<String, Object>> handleDeliveryCallback(
            @Valid @RequestBody DeliveryCallbackRequest request) {
        
        log.info("📥 Received delivery callback for notification {}: {} -> {}",
                request.getNotificationId(), request.getProviderReference(), request.getDeliveryStatus());

        try {
            boolean updated = planExpiryNotificationService.updateDeliveryStatus(
                request.getNotificationId(),
                mapDeliveryStatus(request.getDeliveryStatus()),
                request.getProviderReference(),
                request.getFailureReason()
            );

            Map<String, Object> response = new HashMap<>();
            if (updated) {
                response.put("status", "success");
                response.put("message", "Delivery status updated successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to update delivery status");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Failed to process delivery callback: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Bulk delivery status update endpoint
     */
    @PostMapping("/delivery-callback/bulk")
    public ResponseEntity<Map<String, Object>> handleBulkDeliveryCallback(
            @Valid @RequestBody BulkDeliveryCallbackRequest request) {
        
        log.info("📥 Received bulk delivery callback for {} notifications", request.getUpdates().size());

        try {
            Map<UUID, PlanExpiryNotification.DeliveryStatus> statusUpdates = new HashMap<>();
            
            for (DeliveryCallbackRequest update : request.getUpdates()) {
                statusUpdates.put(update.getNotificationId(), mapDeliveryStatus(update.getDeliveryStatus()));
            }

            Map<UUID, Boolean> results = planExpiryNotificationService.bulkUpdateDeliveryStatus(statusUpdates);
            
            long successCount = results.values().stream().mapToLong(b -> b ? 1 : 0).sum();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("totalProcessed", results.size());
            response.put("successfulUpdates", successCount);
            response.put("failedUpdates", results.size() - successCount);
            response.put("results", results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to process bulk delivery callback: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ─── Delivery Tracking API Endpoints ─────────────────────────────────────

    /**
     * Get delivery tracking information for a specific notification
     */
    @GetMapping("/delivery-tracking/{notificationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> getDeliveryTrackingInfo(
            @PathVariable UUID notificationId) {

        try {
            Map<String, Object> trackingInfo = planExpiryNotificationService.getDeliveryTrackingInfo(notificationId);
            
            if (trackingInfo == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Notification not found");
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", trackingInfo);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get delivery tracking info for notification {}: {}", notificationId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get delivery statistics
     */
    @GetMapping("/delivery-statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> getDeliveryStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            Map<String, Object> statistics = planExpiryNotificationService.getDetailedDeliveryStatistics(startDate, endDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", statistics);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get delivery statistics: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Manual delivery status update (for admin use)
     */
    @PutMapping("/delivery-status/{notificationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateDeliveryStatus(
            @PathVariable UUID notificationId,
            @Valid @RequestBody ManualDeliveryStatusUpdate request) {

        try {
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            log.info("Admin {} manually updating delivery status for notification {} to {}",
                    currentUserId, notificationId, request.getDeliveryStatus());

            boolean updated = planExpiryNotificationService.updateDeliveryStatus(
                notificationId,
                request.getDeliveryStatus(),
                "MANUAL_UPDATE_BY_ADMIN_" + currentUserId,
                request.getReason()
            );

            Map<String, Object> response = new HashMap<>();
            if (updated) {
                response.put("status", "success");
                response.put("message", "Delivery status updated successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to update delivery status");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Failed to manually update delivery status: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Resend notification (for failed deliveries)
     */
    @PostMapping("/resend/{notificationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> resendNotification(@PathVariable UUID notificationId) {
        
        try {
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            log.info("User {} requesting resend for notification {}", currentUserId, notificationId);

            boolean resent = planExpiryNotificationService.resendNotification(notificationId);

            Map<String, Object> response = new HashMap<>();
            if (resent) {
                response.put("status", "success");
                response.put("message", "Notification resent successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to resend notification");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Failed to resend notification {}: {}", notificationId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ─── Helper Methods ───────────────────────────────────────────────────────

    /**
     * Map external delivery status to internal enum
     */
    private PlanExpiryNotification.DeliveryStatus mapDeliveryStatus(String externalStatus) {
        if (externalStatus == null) {
            return PlanExpiryNotification.DeliveryStatus.FAILED;
        }

        switch (externalStatus.toUpperCase()) {
            case "DELIVERED":
            case "DELIVERED_TO_HANDSET":
            case "DELIVERED_TO_TERMINAL":
                return PlanExpiryNotification.DeliveryStatus.DELIVERED;
                
            case "SENT":
            case "QUEUED":
            case "ACCEPTED":
                return PlanExpiryNotification.DeliveryStatus.SENT;
                
            case "FAILED":
            case "UNDELIVERED":
            case "REJECTED":
            case "BLOCKED":
                return PlanExpiryNotification.DeliveryStatus.FAILED;
                
            case "CANCELLED":
            case "CANCELED":
                return PlanExpiryNotification.DeliveryStatus.CANCELLED;
                
            default:
                log.warn("Unknown delivery status received: {}", externalStatus);
                return PlanExpiryNotification.DeliveryStatus.FAILED;
        }
    }

    // ─── Request/Response DTOs ────────────────────────────────────────────────

    /**
     * DTO for delivery callback requests
     */
    public static class DeliveryCallbackRequest {
        @NotNull
        private UUID notificationId;
        
        @NotNull
        private String deliveryStatus;
        
        private String providerReference;
        private String failureReason;
        private String timestamp;

        // Getters and setters
        public UUID getNotificationId() { return notificationId; }
        public void setNotificationId(UUID notificationId) { this.notificationId = notificationId; }

        public String getDeliveryStatus() { return deliveryStatus; }
        public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

        public String getProviderReference() { return providerReference; }
        public void setProviderReference(String providerReference) { this.providerReference = providerReference; }

        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    /**
     * DTO for bulk delivery callback requests
     */
    public static class BulkDeliveryCallbackRequest {
        @NotNull
        private java.util.List<DeliveryCallbackRequest> updates;

        public java.util.List<DeliveryCallbackRequest> getUpdates() { return updates; }
        public void setUpdates(java.util.List<DeliveryCallbackRequest> updates) { this.updates = updates; }
    }

    /**
     * DTO for manual delivery status updates
     */
    public static class ManualDeliveryStatusUpdate {
        @NotNull
        private PlanExpiryNotification.DeliveryStatus deliveryStatus;
        
        private String reason;

        public PlanExpiryNotification.DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
        public void setDeliveryStatus(PlanExpiryNotification.DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}