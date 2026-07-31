package com.krunity.HostelManagment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing room overbooking events and allocation tracking
 */
@Entity
@Table(name = "room_overbooking_log",
        indexes = {
                @Index(name = "idx_overbooking_log_room_event", columnList = "room_id, event_type"),
                @Index(name = "idx_overbooking_log_agreement", columnList = "agreement_id"),
                @Index(name = "idx_overbooking_log_timestamp", columnList = "event_timestamp")
        })
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RoomOverbookingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "overbooking_id", updatable = false)
    private UUID overbookingId;

    // Room Details
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "total_bed_capacity", nullable = false)
    private Integer totalBedCapacity;

    // Overbooking Event
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "agreements_count", nullable = false)
    private Integer agreementsCount;

    @Column(name = "pending_action_count", nullable = false)
    private Integer pendingActionCount;

    @Column(name = "available_beds", nullable = false)
    private Integer availableBeds;

    // Agreement Reference
    @Column(name = "agreement_id")
    private String agreementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private User tenant;

    // Metadata
    @Column(name = "allocation_order")
    private Integer allocationOrder;

    @Column(name = "allocation_successful", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allocationSuccessful;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    // Timestamps
    @CreationTimestamp
    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private LocalDateTime eventTimestamp;

    /**
     * Event types for room overbooking tracking
     */
    public enum EventType {
        AGREEMENT_CREATED,
        ALLOCATION_SUCCESSFUL,
        ALLOCATION_FAILED,
        OVERBOOKING_DETECTED
    }

    /**
     * Builder helper methods
     */
    public static RoomOverbookingLogBuilder forAllocationAttempt(Room room, String agreementId, User tenant) {
        return RoomOverbookingLog.builder()
                .room(room)
                .totalBedCapacity(room.getTotalBeds())
                .agreementId(agreementId)
                .tenant(tenant);
    }

    public static RoomOverbookingLogBuilder forOverbookingDetection(Room room) {
        return RoomOverbookingLog.builder()
                .room(room)
                .totalBedCapacity(room.getTotalBeds())
                .eventType(EventType.OVERBOOKING_DETECTED);
    }

    /**
     * Check if this event represents an overbooking scenario
     */
    public boolean isOverbooking() {
        return agreementsCount != null && totalBedCapacity != null &&
               agreementsCount > totalBedCapacity;
    }

    /**
     * Check if allocation was within capacity
     */
    public boolean isWithinCapacity() {
        return agreementsCount != null && totalBedCapacity != null &&
               agreementsCount <= totalBedCapacity;
    }
}