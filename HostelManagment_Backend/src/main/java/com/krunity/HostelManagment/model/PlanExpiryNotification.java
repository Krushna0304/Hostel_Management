package com.krunity.HostelManagment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.krunity.HostelManagment.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing scheduled notifications for plan expiry events
 * Supports configurable lead times and retry mechanisms
 */
@Entity
@Table(name = "plan_expiry_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PlanExpiryNotification {

    // ─── Identity ─────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id", updatable = false, nullable = false)
    private UUID notificationId;

    // ─── Relationships ────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_allotment_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private RoomAllotment roomAllotment;

    // ─── Agreement Reference ──────────────────────────────────────────────────

    @Column(name = "agreement_id", nullable = false)
    private String agreementId; // MongoDB Agreement ID

    // ─── Notification Details ─────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    // ─── Message Content ──────────────────────────────────────────────────────

    @Column(name = "message_template", nullable = false, length = 50)
    private String messageTemplate;

    @ElementCollection
    @CollectionTable(name = "notification_message_variables", 
                    joinColumns = @JoinColumn(name = "notification_id"))
    @MapKeyColumn(name = "variable_key")
    @Column(name = "variable_value")
    private Map<String, String> messageVariables;

    @Column(name = "actual_message", columnDefinition = "TEXT")
    private String actualMessage;

    // ─── Metadata ─────────────────────────────────────────────────────────────

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    // ─── Timestamps ───────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Enums ────────────────────────────────────────────────────────────────

    public enum DeliveryStatus {
        PENDING,    // Notification scheduled but not yet sent
        SENT,       // Notification sent to delivery service
        DELIVERED,  // Notification successfully delivered
        FAILED,     // Notification delivery failed
        CANCELLED   // Notification cancelled (e.g., tenant took action)
    }

    // ─── Business Logic Methods ───────────────────────────────────────────────

    /**
     * Checks if the notification is due for sending
     */
    public boolean isDue() {
        return scheduledDate != null && 
               (scheduledDate.isBefore(LocalDate.now()) || scheduledDate.equals(LocalDate.now())) &&
               deliveryStatus == DeliveryStatus.PENDING;
    }

    /**
     * Checks if the notification is overdue
     */
    public boolean isOverdue() {
        return scheduledDate != null && 
               scheduledDate.isBefore(LocalDate.now()) &&
               deliveryStatus == DeliveryStatus.PENDING;
    }

    /**
     * Checks if retry is allowed for failed delivery
     */
    public boolean canRetry() {
        return deliveryStatus == DeliveryStatus.FAILED && 
               retryCount < maxRetries &&
               notificationType.allowsRetry();
    }

    /**
     * Increments retry count and updates last retry timestamp
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
    }

    /**
     * Marks notification as sent
     */
    public void markAsSent() {
        this.deliveryStatus = DeliveryStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * Marks notification as delivered
     */
    public void markAsDelivered() {
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }

    /**
     * Marks notification as failed
     */
    public void markAsFailed() {
        this.deliveryStatus = DeliveryStatus.FAILED;
        incrementRetryCount();
    }

    /**
     * Marks notification as cancelled
     */
    public void markAsCancelled() {
        this.deliveryStatus = DeliveryStatus.CANCELLED;
    }

    /**
     * Gets the priority based on notification type
     */
    public int getPriority() {
        return notificationType != null ? notificationType.getPriority() : 3;
    }

    /**
     * Checks if immediate delivery is required
     */
    public boolean requiresImmediateDelivery() {
        return notificationType != null && notificationType.requiresImmediateDelivery();
    }

    /**
     * Gets days until scheduled date (negative if overdue)
     */
    public long getDaysUntilScheduled() {
        if (scheduledDate == null) {
            return 0;
        }
        return LocalDate.now().until(scheduledDate).getDays();
    }

    /**
     * Calculates next retry time based on exponential backoff
     */
    public LocalDateTime getNextRetryTime() {
        if (!canRetry()) {
            return null;
        }
        
        // Exponential backoff: 1min, 5min, 15min, 30min, 1hr
        int[] delayMinutes = {1, 5, 15, 30, 60};
        int delayIndex = Math.min(retryCount, delayMinutes.length - 1);
        
        return LocalDateTime.now().plusMinutes(delayMinutes[delayIndex]);
    }

    /**
     * Checks if notification is in terminal state
     */
    public boolean isTerminal() {
        return deliveryStatus == DeliveryStatus.DELIVERED || 
               deliveryStatus == DeliveryStatus.CANCELLED ||
               (deliveryStatus == DeliveryStatus.FAILED && !canRetry());
    }

    /**
     * Gets user-friendly status description
     */
    public String getStatusDescription() {
        switch (deliveryStatus) {
            case PENDING:
                if (isDue()) {
                    return "Ready to send";
                } else {
                    return String.format("Scheduled for %s", scheduledDate);
                }
            case SENT:
                return "Sent to delivery service";
            case DELIVERED:
                return String.format("Delivered on %s", sentAt.toLocalDate());
            case FAILED:
                if (canRetry()) {
                    return String.format("Failed (Retry %d/%d)", retryCount, maxRetries);
                } else {
                    return "Failed - No more retries";
                }
            case CANCELLED:
                return "Cancelled";
            default:
                return deliveryStatus.toString();
        }
    }

    /**
     * Adds a message variable for template rendering
     */
    public void addMessageVariable(String key, String value) {
        if (messageVariables == null) {
            messageVariables = new java.util.HashMap<>();
        }
        messageVariables.put(key, value);
    }

    /**
     * Gets a message variable value
     */
    public String getMessageVariable(String key) {
        return messageVariables != null ? messageVariables.get(key) : null;
    }

    /**
     * Validates notification data
     */
    public boolean isValid() {
        return tenant != null &&
               agreementId != null && !agreementId.trim().isEmpty() &&
               notificationType != null &&
               scheduledDate != null &&
               messageTemplate != null && !messageTemplate.trim().isEmpty() &&
               leadTimeDays != null && leadTimeDays > 0 && leadTimeDays <= 90 &&
               maxRetries != null && maxRetries >= 0;
    }

    // ─── toString and equals/hashCode ─────────────────────────────────────────

    @Override
    public String toString() {
        return "PlanExpiryNotification{" +
                "notificationId=" + notificationId +
                ", tenantId=" + (tenant != null ? tenant.getUserId() : "null") +
                ", agreementId='" + agreementId + '\'' +
                ", notificationType=" + notificationType +
                ", scheduledDate=" + scheduledDate +
                ", deliveryStatus=" + deliveryStatus +
                ", leadTimeDays=" + leadTimeDays +
                ", retryCount=" + retryCount +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanExpiryNotification that = (PlanExpiryNotification) o;
        return notificationId != null && notificationId.equals(that.notificationId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}