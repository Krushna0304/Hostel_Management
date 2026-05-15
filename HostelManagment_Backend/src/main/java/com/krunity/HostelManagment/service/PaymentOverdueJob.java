package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.model.PaymentRequestSchedule;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import com.krunity.HostelManagment.model.plan.LatePaymentPolicy;
import com.krunity.HostelManagment.repository.AgreementRepository;
import com.krunity.HostelManagment.repository.PaymentRequestScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class PaymentOverdueJob {

    @Autowired
    private PaymentRequestScheduleRepository scheduleRepository;

    @Autowired
    private AgreementRepository agreementRepository;

    /**
     * Runs every day at 9:00 AM.
     * Marks SCHEDULED installments whose due date has passed as OVERDUE
     * and applies late fees based on the plan snapshot.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void markOverdueInstallments() {
        LocalDate today = LocalDate.now();

        List<PaymentRequestSchedule> overdueSchedules = scheduleRepository
                .findByPaymentStatusAndDueDateBefore(TransactionStatus.SCHEDULED, today);

        for (PaymentRequestSchedule schedule : overdueSchedules) {
            schedule.setPaymentStatus(TransactionStatus.OVERDUE);

            // Try to apply late fee from the plan snapshot via the agreement
            try {
                String agreementId = schedule.getTenantPaymentPlan().getAgreementId();
                if (agreementId != null) {
                    agreementRepository.findById(agreementId).ifPresent(agreement -> {
                        RoomAgreementPlan snapshot = agreement.getPlanSnapshot();
                        if (snapshot != null && snapshot.getLatePaymentPolicy() != null) {
                            applyLateFee(schedule, snapshot.getLatePaymentPolicy(), today);
                        }
                    });
                }
            } catch (Exception e) {
                // Don't fail the whole batch if one lookup fails
                System.err.println("Failed to apply late fee for schedule "
                        + schedule.getScheduleId() + ": " + e.getMessage());
            }
        }

        if (!overdueSchedules.isEmpty()) {
            scheduleRepository.saveAll(overdueSchedules);
            System.out.println("Marked " + overdueSchedules.size() + " installments as OVERDUE on " + today);
        }
    }

    private void applyLateFee(PaymentRequestSchedule schedule, LatePaymentPolicy policy, LocalDate today) {
        int gracePeriodDays = policy.getGracePeriodDays() != null ? policy.getGracePeriodDays() : 0;
        long daysLate = ChronoUnit.DAYS.between(schedule.getDueDate(), today);

        if (daysLate <= gracePeriodDays) {
            return; // Still within grace period
        }

        if (policy.getPenalty() == null) return;

        long overdueDays = daysLate - gracePeriodDays;
        long lateFee = 0L;
        String penaltyType = policy.getPenalty().getType();

        if ("PER_DAY".equalsIgnoreCase(penaltyType) && policy.getPenalty().getAmount() != null) {
            lateFee = policy.getPenalty().getAmount().longValue() * overdueDays;
        } else if ("FIXED".equalsIgnoreCase(penaltyType) && policy.getPenalty().getAmount() != null) {
            lateFee = policy.getPenalty().getAmount().longValue();
        }

        // Cap at maxAmount if defined
        if (policy.getPenalty().getMaxAmount() != null) {
            lateFee = Math.min(lateFee, policy.getPenalty().getMaxAmount().longValue());
        }

        schedule.setLateFeeApplied(lateFee);
    }
}
