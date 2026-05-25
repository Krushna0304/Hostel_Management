package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.service.PaymentOverdueJob;
import com.krunity.HostelManagment.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test Controller for manually triggering scheduled jobs
 * WARNING: Only use this in development/testing environment
 * Remove or secure this controller in production
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private PaymentOverdueJob paymentOverdueJob;

    @Autowired
    private ReminderService reminderService;

    /**
     * Manually trigger the overdue payment job
     * This will mark SCHEDULED payments with past due dates as OVERDUE
     */
    @PostMapping("/trigger-overdue-job")
    public ResponseEntity<String> triggerOverdueJob() {
        try {
            paymentOverdueJob.markOverdueInstallments();
            return ResponseEntity.ok("Overdue job executed successfully. Check logs for details.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Failed to execute overdue job: " + e.getMessage());
        }
    }

    /**
     * Manually trigger the reminder service
     * This will send SMS reminders for overdue payments
     */
    @PostMapping("/trigger-reminder-job")
    public ResponseEntity<String> triggerReminderJob() {
        try {
            reminderService.sendOverdueReminders();
            return ResponseEntity.ok("Reminder job executed successfully. Check logs for details.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Failed to execute reminder job: " + e.getMessage());
        }
    }

    /**
     * Trigger both jobs in sequence
     */
    @PostMapping("/trigger-both-jobs")
    public ResponseEntity<String> triggerBothJobs() {
        try {
            paymentOverdueJob.markOverdueInstallments();
            Thread.sleep(1000); // Small delay between jobs
            reminderService.sendOverdueReminders();
            return ResponseEntity.ok("Both jobs executed successfully. Check logs for details.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Failed to execute jobs: " + e.getMessage());
        }
    }
}