package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.enums.ReminderType;
import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.PaymentRequestScheduleRepository;
import com.krunity.HostelManagment.repository.ReminderLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ReminderService {

    @Autowired
    private PaymentRequestScheduleRepository scheduleRepository;

    @Autowired
    private ReminderLogRepository reminderLogRepository;

    @Autowired
    private SmsTemplateService templateService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Scheduled job: Send reminders 1 day before due date
     * Runs daily at 8:00 AM
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void sendBeforeDueDateReminders() {
        log.info("Starting before due date reminder job...");

        LocalDate targetDate = LocalDate.now().plusDays(1);
        List<PaymentRequestSchedule> schedules = scheduleRepository.findByDueDateAndPaymentStatusIn(
            targetDate,
            List.of(TransactionStatus.SCHEDULED, TransactionStatus.PARTIALLY_PAID)
        );

        log.info("Found {} installments due on {}", schedules.size(), targetDate);

        for (PaymentRequestSchedule schedule : schedules) {
            try {
                // Check if reminder already sent
                if (reminderLogRepository.existsBySchedule_ScheduleIdAndReminderType(
                        schedule.getScheduleId(), ReminderType.BEFORE_DUE_DATE)) {
                    continue;
                }

                sendReminder(schedule, ReminderType.BEFORE_DUE_DATE);
            } catch (Exception e) {
                log.error("Failed to send before due date reminder for schedule: {}", 
                         schedule.getScheduleId(), e);
            }
        }

        log.info("Before due date reminder job completed");
    }

    /**
     * Scheduled job: Send reminders on due date
     * Runs daily at 9:00 AM
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void sendOnDueDateReminders() {
        log.info("Starting on due date reminder job...");
        
        LocalDate today = LocalDate.now();
        List<PaymentRequestSchedule> schedules = scheduleRepository.findByDueDateAndPaymentStatusIn(
            today,
            List.of(TransactionStatus.SCHEDULED, TransactionStatus.PARTIALLY_PAID)
        );

        log.info("Found {} installments due today", schedules.size());

        for (PaymentRequestSchedule schedule : schedules) {
            try {
                // Check if reminder already sent
                if (reminderLogRepository.existsBySchedule_ScheduleIdAndReminderType(
                        schedule.getScheduleId(), ReminderType.ON_DUE_DATE)) {
                    continue;
                }

                sendReminder(schedule, ReminderType.ON_DUE_DATE);
            } catch (Exception e) {
                log.error("Failed to send on due date reminder for schedule: {}", 
                         schedule.getScheduleId(), e);
            }
        }

        log.info("On due date reminder job completed");
    }

    /**
     * Scheduled job: Send reminders for overdue payments
     * Runs daily at 10:00 AM
     */
    @Scheduled(cron = "0 0 10 * * ?")
    @Transactional
    public void sendOverdueReminders() {
        log.info("Starting overdue reminder job...");
        
        LocalDate today = LocalDate.now();
        List<PaymentRequestSchedule> schedules = scheduleRepository.findByDueDateBeforeAndPaymentStatusIn(
            today,
            List.of(TransactionStatus.OVERDUE, TransactionStatus.PARTIALLY_PAID)
        );

        log.info("Found {} overdue installments", schedules.size());

        for (PaymentRequestSchedule schedule : schedules) {
            try {
                sendReminder(schedule, ReminderType.AFTER_DUE_DATE);
            } catch (Exception e) {
                log.error("Failed to send overdue reminder for schedule: {}", 
                         schedule.getScheduleId(), e);
            }
        }

        log.info("Overdue reminder job completed");
    }

    /**
     * Send reminder for a specific schedule
     */
    @Transactional
    public void sendReminder(PaymentRequestSchedule schedule, ReminderType reminderType) {
        TenantPaymentPlan plan = schedule.getTenantPaymentPlan();
        User tenant = plan.getTenant();
        
        // Get owner from the payment plan
        UUID ownerId = getOwnerIdFromPlan(plan);
        
        // Check if owner has reminders enabled
        OwnerSubscription subscription = subscriptionService.getActiveSubscription(ownerId);
        
        boolean smsEnabled = subscription.getSmsRemindersEnabled();
        boolean emailEnabled = subscription.getEmailRemindersEnabled();
        
        if (!smsEnabled && !emailEnabled) {
            log.info("Reminders not enabled for owner: {}", ownerId);
            return;
        }

        // Get template
        String template = templateService.getTemplate(ownerId, reminderType);
        
        // Prepare placeholder values
        Map<String, String> values = preparePlaceholderValues(schedule, tenant);
        
        // Replace placeholders
        String message = templateService.replacePlaceholders(template, values);
        
        // Send notifications
        boolean success = false;
        String errorMessage = null;
        String sentVia = "";
        
        try {
            if (smsEnabled && tenant.getPhoneNumber() != null) {
                notificationService.sendSms(tenant.getPhoneNumber(), message);
                sentVia = "SMS";
                success = true;
            }
            
            if (emailEnabled && tenant.getUsername() != null) {
                // Assuming username is email or construct email
                String email = tenant.getUsername().contains("@") ? 
                              tenant.getUsername() : 
                              tenant.getUsername() + "@example.com";
                notificationService.sendEmail(
                    email,
                    "Payment Reminder - " + reminderType.name().replace("_", " "),
                    message
                );
                sentVia = sentVia.isEmpty() ? "EMAIL" : "BOTH";
                success = true;
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("Failed to send reminder: {}", errorMessage, e);
        }
        
        // Log reminder
        ReminderLog reminderLog = ReminderLog.builder()
                .schedule(schedule)
                .tenant(tenant)
                .reminderType(reminderType)
                .messageSent(message)
                .sentVia(sentVia)
                .success(success)
                .errorMessage(errorMessage)
                .build();
        
        reminderLogRepository.save(reminderLog);
        
        log.info("Reminder sent to tenant {} for schedule {} via {}", 
                tenant.getUserId(), schedule.getScheduleId(), sentVia);
    }

    /**
     * Prepare placeholder values for template
     */
    private Map<String, String> preparePlaceholderValues(PaymentRequestSchedule schedule, User tenant) {
        TenantPaymentPlan plan = schedule.getTenantPaymentPlan();
        
        // Get hostel and room info
        String hostelName = "Your Hostel";
        String roomNumber = "N/A";
        
        try {
            if (plan.getRoomAllotment() != null) {
                Room room = plan.getRoomAllotment().getRoom();
                if (room != null) {
                    roomNumber = room.getRoomNumber();
                    if (room.getHostel() != null) {
                        hostelName = room.getHostel().getHostelName();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch hostel/room info", e);
        }
        
        long totalAmount = schedule.getAmount() + schedule.getLateFeeApplied() - schedule.getPaidAmount();
        
        Map<String, String> values = new HashMap<>();
        values.put("tenantName", tenant.getDisplayName());
        values.put("amount", String.valueOf(schedule.getAmount()));
        values.put("dueDate", schedule.getDueDate().toString());
        values.put("hostelName", hostelName);
        values.put("roomNumber", roomNumber);
        values.put("lateFee", String.valueOf(schedule.getLateFeeApplied()));
        values.put("totalAmount", String.valueOf(totalAmount));
        
        return values;
    }

    /**
     * Get owner ID from payment plan
     */
    private UUID getOwnerIdFromPlan(TenantPaymentPlan plan) {
        try {
            if (plan.getRoomAllotment() != null) {
                Room room = plan.getRoomAllotment().getRoom();
                if (room != null && room.getHostel() != null) {
                    return room.getHostel().getOwner().getUserId();
                }
            }
        } catch (Exception e) {
            log.error("Could not get owner ID from plan", e);
        }
        return null;
    }

    /**
     * Manual trigger for testing
     */
    @Transactional
    public void sendManualReminder(UUID scheduleId, ReminderType reminderType) {
        PaymentRequestSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        
        sendReminder(schedule, reminderType);
    }
}
