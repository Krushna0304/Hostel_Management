package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.exception.InvalidStatusTransitionException;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.exception.UnauthorizedException;
import com.krunity.HostelManagment.model.RoomAllotment;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class AllotmentService {

    public final RoomAllotmentRepository roomAllotmentRepository;
    private final RoomAllotmentStatusTransitionValidator transitionValidator;
    private final NotificationService notificationService;

    public AllotmentService(RoomAllotmentRepository roomAllotmentRepository,
                            RoomAllotmentStatusTransitionValidator transitionValidator,
                            NotificationService notificationService) {
        this.roomAllotmentRepository = roomAllotmentRepository;
        this.transitionValidator = transitionValidator;
        this.notificationService = notificationService;
    }

    // ─── Tenant marks arrival (UPCOMING → ACTIVE) ─────────────────────────────

    @Transactional
    public RoomAllotment markArrival(UUID allotmentId, UUID tenantId) {
        RoomAllotment allotment = loadAndVerifyTenant(allotmentId, tenantId);

        transitionValidator.validate(allotment.getRoomAllotmentStatus(), RoomAllotmentStatus.ACTIVE);

        allotment.setRoomAllotmentStatus(RoomAllotmentStatus.ACTIVE);
        allotment.setLastStatusChangedBy("TENANT");
        allotment.setLastStatusChangedAt(LocalDateTime.now());

        RoomAllotment saved = roomAllotmentRepository.save(allotment);
        log.info("Allotment {} marked ACTIVE by tenant {}", allotmentId, tenantId);
        return saved;
    }

    // ─── Owner marks tenant arrival (UPCOMING → ACTIVE) ──────────────────────

    @Transactional
    public RoomAllotment markArrivalByOwner(UUID allotmentId) {
        UUID ownerId = ApplicationContext.getUser().getUserId();
        RoomAllotment allotment = loadAndVerifyOwner(allotmentId, ownerId);

        transitionValidator.validate(allotment.getRoomAllotmentStatus(), RoomAllotmentStatus.ACTIVE);

        allotment.setRoomAllotmentStatus(RoomAllotmentStatus.ACTIVE);
        allotment.setLastStatusChangedBy("OWNER");
        allotment.setLastStatusChangedAt(LocalDateTime.now());

        RoomAllotment saved = roomAllotmentRepository.save(allotment);
        log.info("Allotment {} marked ACTIVE by owner {}", allotmentId, ownerId);
        return saved;
    }

    // ─── Dual LEFT confirmation ───────────────────────────────────────────────

    /**
     * Tenant confirms they have physically vacated.
     * Status moves to LEFT only when the owner also confirms.
     */
    @Transactional
    public RoomAllotment markTenantLeft(UUID allotmentId, UUID tenantId) {
        RoomAllotment allotment = loadAndVerifyTenant(allotmentId, tenantId);

        requireOnNoticePeriod(allotment);

        allotment.setTenantMarkedLeft(true);
        allotment.setTenantLeftAt(LocalDateTime.now());

        if (allotment.isOwnerMarkedLeft()) {
            finalizeLeft(allotment);
        } else {
            User owner = allotment.getRoom().getHostel().getOwner();
            notificationService.sendLeftConfirmationPendingReminder(owner, allotment, "OWNER");
            log.info("Allotment {}: tenant confirmed left, waiting for owner", allotmentId);
        }

        return roomAllotmentRepository.save(allotment);
    }

    /**
     * Owner confirms the tenant has physically vacated.
     * Status moves to LEFT only when the tenant also confirms.
     */
    @Transactional
    public RoomAllotment markOwnerLeft(UUID allotmentId, UUID ownerId) {
        RoomAllotment allotment = loadAndVerifyOwner(allotmentId, ownerId);

        requireOnNoticePeriod(allotment);

        allotment.setOwnerMarkedLeft(true);
        allotment.setOwnerLeftAt(LocalDateTime.now());

        if (allotment.isTenantMarkedLeft()) {
            finalizeLeft(allotment);
        } else {
            notificationService.sendLeftConfirmationPendingReminder(
                    allotment.getTenant(), allotment, "TENANT");
            log.info("Allotment {}: owner confirmed left, waiting for tenant", allotmentId);
        }

        return roomAllotmentRepository.save(allotment);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /** Applies the LEFT transition once both parties have confirmed. */
    private void finalizeLeft(RoomAllotment allotment) {
        transitionValidator.validate(allotment.getRoomAllotmentStatus(), RoomAllotmentStatus.LEFT);
        allotment.setRoomAllotmentStatus(RoomAllotmentStatus.LEFT);
        allotment.setLastStatusChangedBy("SYSTEM");
        allotment.setLastStatusChangedAt(LocalDateTime.now());
        log.info("Allotment {} transitioned to LEFT (both parties confirmed)", allotment.getAllotmentId());
    }

    private void requireOnNoticePeriod(RoomAllotment allotment) {
        if (allotment.getRoomAllotmentStatus() != RoomAllotmentStatus.ON_NOTICE_PERIOD) {
            throw new InvalidStatusTransitionException(
                    "Departure can only be confirmed when allotment is in ON_NOTICE_PERIOD status. Current: "
                    + allotment.getRoomAllotmentStatus());
        }
    }

    private RoomAllotment loadAndVerifyTenant(UUID allotmentId, UUID tenantId) {
        RoomAllotment allotment = roomAllotmentRepository.findById(allotmentId)
                .orElseThrow(() -> new NotFoundException("Allotment not found: " + allotmentId));
        if (!allotment.getTenant().getUserId().equals(tenantId)) {
            throw new UnauthorizedException("You can only act on your own allotment");
        }
        return allotment;
    }

    private RoomAllotment loadAndVerifyOwner(UUID allotmentId, UUID ownerId) {
        return roomAllotmentRepository.findByAllotmentIdAndOwner(allotmentId, ownerId)
                .orElseThrow(() -> new UnauthorizedException(
                        "Allotment not found or does not belong to your hostel: " + allotmentId));
    }
}
