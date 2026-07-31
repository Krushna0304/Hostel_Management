package com.krunity.HostelManagment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for room availability information including overbooking status.
 * Used for displaying room allocation status to owners and for room allocation decisions.
 * 
 * @author Kiro Enhanced Settlement System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAvailabilityDto {

    private UUID roomId;
    private String roomNumber;
    private Integer totalBedCapacity;
    private Integer currentOccupancy;
    private Integer tenantActionPendingCount;
    private Integer actualAvailableBeds;
    private Boolean overbookingDetected;
    private AllocationStatus allocationStatus;
    private List<UpcomingDeparture> upcomingDepartures;

    /**
     * Enum for room allocation status
     */
    public enum AllocationStatus {
        AVAILABLE,           // Room has available beds
        FULLY_OCCUPIED,      // Room at capacity but not overbooked
        OVERBOOKED,         // More agreements than bed capacity
        NOT_AVAILABLE       // Room not available for allocation
    }

    /**
     * DTO for upcoming departure information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpcomingDeparture {
        private String tenantName;
        private LocalDate endDate;
        private String settlementStatus;
        private String allotmentStatus;
    }

    /**
     * Factory method to create availability DTO from room data
     */
    public static RoomAvailabilityDto create(
            UUID roomId, String roomNumber, int totalBedCapacity,
            int currentOccupancy, int tenantActionPendingCount,
            List<UpcomingDeparture> upcomingDepartures) {
        
        int actualAvailableBeds = Math.max(0, totalBedCapacity - currentOccupancy);
        boolean overbookingDetected = currentOccupancy > totalBedCapacity;
        
        AllocationStatus status = determineAllocationStatus(
                totalBedCapacity, currentOccupancy, actualAvailableBeds);
        
        return RoomAvailabilityDto.builder()
                .roomId(roomId)
                .roomNumber(roomNumber)
                .totalBedCapacity(totalBedCapacity)
                .currentOccupancy(currentOccupancy)
                .tenantActionPendingCount(tenantActionPendingCount)
                .actualAvailableBeds(actualAvailableBeds)
                .overbookingDetected(overbookingDetected)
                .allocationStatus(status)
                .upcomingDepartures(upcomingDepartures)
                .build();
    }

    /**
     * Determines allocation status based on occupancy data
     */
    private static AllocationStatus determineAllocationStatus(
            int totalCapacity, int currentOccupancy, int availableBeds) {
        
        if (currentOccupancy > totalCapacity) {
            return AllocationStatus.OVERBOOKED;
        } else if (availableBeds > 0) {
            return AllocationStatus.AVAILABLE;
        } else if (currentOccupancy == totalCapacity) {
            return AllocationStatus.FULLY_OCCUPIED;
        } else {
            return AllocationStatus.NOT_AVAILABLE;
        }
    }

    /**
     * Checks if room can accept new allocations
     */
    public boolean canAcceptNewAllocation() {
        return allocationStatus == AllocationStatus.AVAILABLE;
    }

    /**
     * Gets the overbooking degree (positive if overbooked, 0 if not)
     */
    public int getOverbookingDegree() {
        if (!overbookingDetected) {
            return 0;
        }
        return currentOccupancy - totalBedCapacity;
    }

    /**
     * Gets occupancy percentage
     */
    public double getOccupancyPercentage() {
        if (totalBedCapacity == 0) {
            return 0.0;
        }
        return (double) currentOccupancy / totalBedCapacity * 100.0;
    }

    /**
     * Gets display priority for room selection sorting (lower = higher priority)
     */
    public int getDisplayPriority() {
        switch (allocationStatus) {
            case AVAILABLE:
                return tenantActionPendingCount != null ? tenantActionPendingCount : 0;
            case FULLY_OCCUPIED:
                return 1000;
            case OVERBOOKED:
                return 2000 + getOverbookingDegree();
            case NOT_AVAILABLE:
                return 9999;
            default:
                return 5000;
        }
    }

    /**
     * Gets human-readable status description
     */
    public String getStatusDescription() {
        switch (allocationStatus) {
            case AVAILABLE:
                return String.format("%d bed(s) available", actualAvailableBeds);
            case FULLY_OCCUPIED:
                return "Fully occupied";
            case OVERBOOKED:
                return String.format("Overbooked by %d agreement(s)", getOverbookingDegree());
            case NOT_AVAILABLE:
                return "Not available";
            default:
                return "Unknown status";
        }
    }

    /**
     * Checks if room has pending tenant actions that might free up space
     */
    public boolean hasPendingActions() {
        return tenantActionPendingCount != null && tenantActionPendingCount > 0;
    }

    /**
     * Checks if room has upcoming departures
     */
    public boolean hasUpcomingDepartures() {
        return upcomingDepartures != null && !upcomingDepartures.isEmpty();
    }

    /**
     * Gets count of upcoming departures within specified days
     */
    public long getUpcomingDeparturesCount(int withinDays) {
        if (upcomingDepartures == null) {
            return 0;
        }
        
        LocalDate cutoffDate = LocalDate.now().plusDays(withinDays);
        return upcomingDepartures.stream()
                .filter(departure -> departure.getEndDate() != null)
                .filter(departure -> !departure.getEndDate().isAfter(cutoffDate))
                .count();
    }
}