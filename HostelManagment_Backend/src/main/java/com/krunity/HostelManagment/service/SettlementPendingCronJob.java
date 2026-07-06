package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.model.RoomAllotment;
import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Daily cron job that transitions UPCOMING / ACTIVE allotments to SETTLEMENT_PENDING
 * when the current date reaches the notice-period window:
 *
 *   currentDate >= (endDate - noticePeriodMonths)
 *
 * Runs at 01:00 AM every day (low-traffic window).
 * Only processes allotments that have both endDate and noticePeriodMonths set.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementPendingCronJob {

    private final RoomAllotmentRepository roomAllotmentRepository;
    private final RoomAllotmentStatusTransitionValidator transitionValidator;
    private final NotificationService notificationService;

    private static final Set<RoomAllotmentStatus> ELIGIBLE_STATUSES =
            Set.of(RoomAllotmentStatus.UPCOMING, RoomAllotmentStatus.ACTIVE);

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void checkAndMarkSettlementPending() {
        LocalDate today = LocalDate.now();
        log.info("SettlementPendingCronJob started for date {}", today);

        List<RoomAllotment> candidates =
                roomAllotmentRepository.findEligibleForSettlementPendingCheck(ELIGIBLE_STATUSES);

        int marked = 0;
        for (RoomAllotment allotment : candidates) {
            LocalDate noticeWindowStart =
                    allotment.getEndDate().minusMonths(allotment.getNoticePeriodMonths());

            if (!today.isBefore(noticeWindowStart)) {
                // currentDate >= (endDate - noticePeriodMonths) → enter SETTLEMENT_PENDING
                try {
                    transitionValidator.validate(
                            allotment.getRoomAllotmentStatus(), RoomAllotmentStatus.SETTLEMENT_PENDING);

                    allotment.setRoomAllotmentStatus(RoomAllotmentStatus.SETTLEMENT_PENDING);
                    allotment.setLastStatusChangedBy("SYSTEM");
                    allotment.setLastStatusChangedAt(LocalDateTime.now());
                    roomAllotmentRepository.save(allotment);

                    notificationService.sendSettlementPendingReminder(allotment.getTenant(), allotment);
                    marked++;

                    log.info("Allotment {} → SETTLEMENT_PENDING (end={}, noticePeriod={} months)",
                            allotment.getAllotmentId(),
                            allotment.getEndDate(),
                            allotment.getNoticePeriodMonths());
                } catch (Exception e) {
                    log.error("Failed to transition allotment {}: {}", allotment.getAllotmentId(), e.getMessage());
                }
            }
        }

        log.info("SettlementPendingCronJob finished. Candidates={}, Marked={}", candidates.size(), marked);
    }
}
