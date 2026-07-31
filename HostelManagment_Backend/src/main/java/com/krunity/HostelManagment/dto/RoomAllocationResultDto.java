package com.krunity.HostelManagment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for room allocation operation results.
 * Contains allocation outcome, metadata, and suggested alternatives if allocation fails.
 * 
 * @author Kiro Enhanced Settlement System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAllocationResultDto {

    private UUID allocationId;
    private AllocationStatus status;
    private Integer allocationOrder;
    private Boolean bedAssigned;
    private OverbookingStatus overbookingStatus;
    private String message;
    private String failureReason;
    private List<UUID> suggestedAlternatives;
    private AllocationMetadata metadata;

    /**
     * Enum for allocation status
     */
    public enum AllocationStatus {
        ALLOCATED,           // Successfully allocated
        FAILED,              // Allocation failed
        PENDING,             // Allocation pending (awaiting confirmation)
        WAITLISTED          // Added to waitlist due to overbooking
    }

    /**
     * Enum for overbooking status
     */
    public enum OverbookingStatus {
        WITHIN_CAPACITY,     // Allocation within room capacity
        OVERBOOKED,          // Room is overbooked but allocation succeeded (first-come-first-served)
        CAPACITY_EXCEEDED,   // Room capacity exceeded, allocation failed
        WAITLIST_REQUIRED    // Allocation requires waitlist due to severe overbooking
    }

    /**
     * Metadata about the allocation process
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AllocationMetadata {
        private Integer totalCapacity;
        private Integer currentOccupancy;
        private Integer pendingActionCount;
        private Integer availableBeds;
        private LocalDateTime allocationTimestamp;
        private String allocationMethod; // "FIRST_COME_FIRST_SERVED", "CAPACITY_BASED", etc.
    }

    /**
     * Factory method for successful allocation
     */
    public static RoomAllocationResultDto createSuccessful(
            UUID allocationId, int allocationOrder, boolean bedAssigned,
            int totalCapacity, int currentOccupancy, int pendingActionCount) {
        
        OverbookingStatus overbookingStatus = currentOccupancy > totalCapacity 
                ? OverbookingStatus.OVERBOOKED 
                : OverbookingStatus.WITHIN_CAPACITY;
        
        AllocationMetadata metadata = AllocationMetadata.builder()
                .totalCapacity(totalCapacity)
                .currentOccupancy(currentOccupancy)
                .pendingActionCount(pendingActionCount)
                .availableBeds(Math.max(0, totalCapacity - currentOccupancy))
                .allocationTimestamp(LocalDateTime.now())
                .allocationMethod("FIRST_COME_FIRST_SERVED")
                .build();
        
        String message = bedAssigned 
                ? String.format("Successfully allocated bed (position: %d)", allocationOrder)
                : String.format("Allocation recorded (waitlist position: %d)", allocationOrder);
        
        return RoomAllocationResultDto.builder()
                .allocationId(allocationId)
                .status(bedAssigned ? AllocationStatus.ALLOCATED : AllocationStatus.WAITLISTED)
                .allocationOrder(allocationOrder)
                .bedAssigned(bedAssigned)
                .overbookingStatus(overbookingStatus)
                .message(message)
                .metadata(metadata)
                .build();
    }

    /**
     * Factory method for failed allocation
     */
    public static RoomAllocationResultDto createFailed(
            String reason, int totalCapacity, int currentOccupancy, 
            int pendingActionCount, List<UUID> suggestedAlternatives) {
        
        OverbookingStatus overbookingStatus = currentOccupancy > totalCapacity 
                ? OverbookingStatus.CAPACITY_EXCEEDED 
                : OverbookingStatus.WITHIN_CAPACITY;
        
        AllocationMetadata metadata = AllocationMetadata.builder()
                .totalCapacity(totalCapacity)
                .currentOccupancy(currentOccupancy)
                .pendingActionCount(pendingActionCount)
                .availableBeds(Math.max(0, totalCapacity - currentOccupancy))
                .allocationTimestamp(LocalDateTime.now())
                .allocationMethod("CAPACITY_BASED")
                .build();
        
        return RoomAllocationResultDto.builder()
                .status(AllocationStatus.FAILED)
                .bedAssigned(false)
                .overbookingStatus(overbookingStatus)
                .message("Room allocation failed: " + reason)
                .failureReason(reason)
                .suggestedAlternatives(suggestedAlternatives)
                .metadata(metadata)
                .build();
    }

    /**
     * Factory method for waitlisted allocation
     */
    public static RoomAllocationResultDto createWaitlisted(
            UUID allocationId, int waitlistPosition, int totalCapacity, 
            int currentOccupancy, int pendingActionCount) {
        
        AllocationMetadata metadata = AllocationMetadata.builder()
                .totalCapacity(totalCapacity)
                .currentOccupancy(currentOccupancy)
                .pendingActionCount(pendingActionCount)
                .availableBeds(Math.max(0, totalCapacity - currentOccupancy))
                .allocationTimestamp(LocalDateTime.now())
                .allocationMethod("WAITLIST")
                .build();
        
        return RoomAllocationResultDto.builder()
                .allocationId(allocationId)
                .status(AllocationStatus.WAITLISTED)
                .allocationOrder(waitlistPosition)
                .bedAssigned(false)
                .overbookingStatus(OverbookingStatus.WAITLIST_REQUIRED)
                .message(String.format("Added to waitlist (position: %d)", waitlistPosition))
                .metadata(metadata)
                .build();
    }

    /**
     * Checks if allocation was successful (either allocated or waitlisted)
     */
    public boolean isSuccessful() {
        return status == AllocationStatus.ALLOCATED || status == AllocationStatus.WAITLISTED;
    }

    /**
     * Checks if allocation failed completely
     */
    public boolean isFailed() {
        return status == AllocationStatus.FAILED;
    }

    /**
     * Checks if tenant has an actual bed assigned
     */
    public boolean hasBedAssigned() {
        return bedAssigned != null && bedAssigned;
    }

    /**
     * Checks if allocation involves overbooking
     */
    public boolean isOverbooked() {
        return overbookingStatus == OverbookingStatus.OVERBOOKED || 
               overbookingStatus == OverbookingStatus.CAPACITY_EXCEEDED ||
               overbookingStatus == OverbookingStatus.WAITLIST_REQUIRED;
    }

    /**
     * Gets human-readable status description
     */
    public String getStatusDescription() {
        switch (status) {
            case ALLOCATED:
                return String.format("Allocated (Bed #%d)", allocationOrder);
            case WAITLISTED:
                return String.format("Waitlisted (#%d in queue)", allocationOrder);
            case FAILED:
                return "Allocation Failed";
            case PENDING:
                return "Allocation Pending";
            default:
                return "Unknown Status";
        }
    }

    /**
     * Gets overbooking status description
     */
    public String getOverbookingStatusDescription() {
        switch (overbookingStatus) {
            case WITHIN_CAPACITY:
                return "Within room capacity";
            case OVERBOOKED:
                return "Room overbooked - first-come-first-served";
            case CAPACITY_EXCEEDED:
                return "Room capacity exceeded";
            case WAITLIST_REQUIRED:
                return "Waitlist required due to overbooking";
            default:
                return "Unknown overbooking status";
        }
    }

    /**
     * Checks if there are suggested alternative rooms
     */
    public boolean hasSuggestedAlternatives() {
        return suggestedAlternatives != null && !suggestedAlternatives.isEmpty();
    }

    /**
     * Gets the count of suggested alternatives
     */
    public int getSuggestedAlternativesCount() {
        return hasSuggestedAlternatives() ? suggestedAlternatives.size() : 0;
    }

    /**
     * Checks if this allocation requires special handling due to overbooking
     */
    public boolean requiresSpecialHandling() {
        return isOverbooked() && (isFailed() || status == AllocationStatus.WAITLISTED);
    }
}