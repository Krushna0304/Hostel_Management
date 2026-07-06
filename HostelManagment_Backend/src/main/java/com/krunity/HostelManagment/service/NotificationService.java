package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.RoomAllotment;
import com.krunity.HostelManagment.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class NotificationService {
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Autowired
    private SmsService smsService;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("dd MMM yyyy, hh:mm a")
            .withZone(ZoneId.of("Asia/Kolkata"));
    
    public void sendQrActivationEmail(Agreement agreement, User tenant) {
        if (!emailEnabled || mailSender == null) {
            log.info("{}", "Email notification disabled. Would send QR activation email to: " + tenant.getUsername());
            return;
        }
        
        try {
            String activationUrl = frontendUrl + "/tenant/activate?token=" + agreement.getQrToken();
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(tenant.getUsername() + "@example.com"); // Adjust based on your email field
            message.setSubject("Agreement Activation - Hostel Management");
            message.setText(String.format(
                "Dear %s,\n\n" +
                "An agreement has been created for you. Please activate it using the QR code or link below:\n\n" +
                "Activation Link: %s\n\n" +
                "QR Token: %s\n\n" +
                "This link will expire on: %s\n\n" +
                "Thank you,\nHostel Management System",
                tenant.getDisplayName(),
                activationUrl,
                agreement.getQrToken(),
                agreement.getQrExpiry()
            ));
            
            mailSender.send(message);
        } catch (Exception e) {
            log.error("{}", "Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Sends SMS to tenant when an agreement is created.
     * Includes plan name, rent, start date, and a clickable activation link.
     */
    public void sendQrActivationSms(Agreement agreement, User tenant,
                                    String hostelName, String roomNumber) {
        try {
            String activationUrl = frontendUrl + "/tenant/activate?token=" + agreement.getQrToken();

            String planName = (agreement.getPlanSnapshot() != null && agreement.getPlanSnapshot().getPlanName() != null)
                    ? agreement.getPlanSnapshot().getPlanName()
                    : "N/A";

            BigDecimal monthlyRent = BigDecimal.ZERO;
            if (agreement.getPlanSnapshot() != null
                    && agreement.getPlanSnapshot().getRentDetails() != null
                    && agreement.getPlanSnapshot().getRentDetails().getMonthlyRent() != null) {
                monthlyRent = agreement.getPlanSnapshot().getRentDetails().getMonthlyRent();
            } else if (agreement.getRent() != null) {
                monthlyRent = agreement.getRent();
            }

            String startDate = agreement.getStartDate() != null
                    ? agreement.getStartDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    : "N/A";

            String expiryFormatted = agreement.getQrExpiry() != null
                    ? DATE_FMT.format(agreement.getQrExpiry())
                    : "72 hours";

            String smsBody = String.format(
                "Dear %s,\n" +
                "A room agreement has been created for you.\n\n" +
                "Details:\n" +
                "  Hostel : %s\n" +
                "  Room   : %s\n" +
                "  Plan   : %s\n" +
                "  Rent   : Rs.%s/month\n" +
                "  Start  : %s\n\n" +
                "Activate your agreement here:\n%s\n\n" +
                "Link expires: %s\n" +
                "- Hostel Management",
                tenant.getDisplayName(),
                hostelName,
                roomNumber,
                planName,
                monthlyRent.toPlainString(),
                startDate,
                activationUrl,
                expiryFormatted
            );

            smsService.sendMessage(tenant.getPhoneNumber(), smsBody);
            log.info("📱 Agreement creation SMS sent to tenant %s (%s)%n", tenant.getDisplayName(), tenant.getPhoneNumber());

        } catch (Exception e) {
            log.error("{}", "Failed to send agreement creation SMS to tenant: " + e.getMessage());
        }
    }

    /**
     * Legacy overload — kept for backward compatibility; hostel/room info not available.
     */
    public void sendQrActivationSms(Agreement agreement, User tenant) {
        sendQrActivationSms(agreement, tenant, "N/A", "N/A");
    }

    /**
     * Sends SMS to owner when a tenant accepts (activates) an agreement.
     */
    public void sendAgreementAcceptedNotification(Agreement agreement, User owner,
                                                  String tenantName, String hostelName, String roomNumber) {
        try {
            String planName = (agreement.getPlanSnapshot() != null && agreement.getPlanSnapshot().getPlanName() != null)
                    ? agreement.getPlanSnapshot().getPlanName()
                    : "N/A";

            BigDecimal monthlyRent = BigDecimal.ZERO;
            if (agreement.getPlanSnapshot() != null
                    && agreement.getPlanSnapshot().getRentDetails() != null
                    && agreement.getPlanSnapshot().getRentDetails().getMonthlyRent() != null) {
                monthlyRent = agreement.getPlanSnapshot().getRentDetails().getMonthlyRent();
            } else if (agreement.getRent() != null) {
                monthlyRent = agreement.getRent();
            }

            String activatedAt = agreement.getActivatedAt() != null
                    ? DATE_FMT.format(agreement.getActivatedAt())
                    : "just now";

            String ownerSms = String.format(
                "Dear %s,\n" +
                "Good news! A tenant has accepted and activated their agreement.\n\n" +
                "Details:\n" +
                "  Tenant : %s\n" +
                "  Hostel : %s\n" +
                "  Room   : %s\n" +
                "  Plan   : %s\n" +
                "  Rent   : Rs.%s/month\n" +
                "  Status : ACTIVE\n" +
                "  Time   : %s\n\n" +
                "Login to your dashboard to view details.\n" +
                "- Hostel Management",
                owner.getDisplayName(),
                tenantName,
                hostelName,
                roomNumber,
                planName,
                monthlyRent.toPlainString(),
                activatedAt
            );

            smsService.sendMessage(owner.getPhoneNumber(), ownerSms);
            log.info("📱 Agreement acceptance SMS sent to owner %s (%s)%n", owner.getDisplayName(), owner.getPhoneNumber());

        } catch (Exception e) {
            log.error("{}", "Failed to send agreement acceptance SMS to owner: " + e.getMessage());
        }

        // Also send email if enabled
        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(owner.getUsername() + "@example.com");
                message.setSubject("Agreement Accepted - Hostel Management");
                message.setText(String.format(
                    "Dear %s,\n\nThe agreement for tenant %s (Hostel: %s, Room: %s) has been accepted and is now ACTIVE.\n\nThank you,\nHostel Management System",
                    owner.getDisplayName(), tenantName, hostelName, roomNumber
                ));
                mailSender.send(message);
            } catch (Exception e) {
                log.error("{}", "Failed to send agreement acceptance email: " + e.getMessage());
            }
        }
    }

    /**
     * Sends SMS to tenant when their agreement is accepted/activated.
     */
    public void sendAgreementActivatedSmsToTenant(Agreement agreement, User tenant,
                                                   String hostelName, String roomNumber) {
        try {
            String planName = (agreement.getPlanSnapshot() != null && agreement.getPlanSnapshot().getPlanName() != null)
                    ? agreement.getPlanSnapshot().getPlanName()
                    : "N/A";

            BigDecimal monthlyRent = BigDecimal.ZERO;
            if (agreement.getPlanSnapshot() != null
                    && agreement.getPlanSnapshot().getRentDetails() != null
                    && agreement.getPlanSnapshot().getRentDetails().getMonthlyRent() != null) {
                monthlyRent = agreement.getPlanSnapshot().getRentDetails().getMonthlyRent();
            } else if (agreement.getRent() != null) {
                monthlyRent = agreement.getRent();
            }

            String startDate = agreement.getStartDate() != null
                    ? agreement.getStartDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    : "N/A";

            String tenantSms = String.format(
                "Dear %s,\n" +
                "Your hostel agreement is now ACTIVE. Welcome!\n\n" +
                "Details:\n" +
                "  Hostel : %s\n" +
                "  Room   : %s\n" +
                "  Plan   : %s\n" +
                "  Rent   : Rs.%s/month\n" +
                "  Start  : %s\n\n" +
                "You can now log in to your tenant portal to manage payments and view your agreement.\n" +
                "Portal: %s\n\n" +
                "- Hostel Management",
                tenant.getDisplayName(),
                hostelName,
                roomNumber,
                planName,
                monthlyRent.toPlainString(),
                startDate,
                frontendUrl + "/tenant/dashboard"
            );

            smsService.sendMessage(tenant.getPhoneNumber(), tenantSms);
            log.info("📱 Agreement activation confirmation SMS sent to tenant %s (%s)%n", tenant.getDisplayName(), tenant.getPhoneNumber());

        } catch (Exception e) {
            log.error("{}", "Failed to send agreement activation SMS to tenant: " + e.getMessage());
        }
    }

    /**
     * Legacy overload — kept for backward compatibility.
     */
    public void sendAgreementAcceptedNotification(Agreement agreement, User owner) {
        sendAgreementAcceptedNotification(agreement, owner, "N/A", "N/A", "N/A");
    }
    
    public void sendCashPaymentOtp(User owner, String otp) {
        // Send OTP via SMS using Twilio
        smsService.sendOtp(owner.getPhoneNumber(), otp);
        
        // Also log to console for debugging
        log.info("💰 Cash Payment OTP sent to owner %s (%s). OTP: %s",
            owner.getDisplayName(),
            owner.getPhoneNumber(),
            otp
        );
    }
    
    /**
     * Send SMS notification
     */
    public void sendSms(String phoneNumber, String message) {
        try {
            smsService.sendMessage(phoneNumber, message);
            log.info("📱 SMS sent to %s: %s",
                phoneNumber,
                message
                );
        } catch (Exception e) {
            log.error("{}", "Failed to send SMS: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Send email notification
     */
    public void sendEmail(String toEmail, String subject, String body) {
        if (!emailEnabled || mailSender == null) {
            log.info("📧 Email notification disabled. Would send to %s: %s",
                toEmail,
                subject
            );;
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            log.info("📧 Email sent to %s: %s",
                toEmail,
                subject
            );
        } catch (Exception e) {
            log.error("{}", "Failed to send email: " + e.getMessage());
            throw e;
        }
    }
    
    // Settlement-related notification methods
    
    public void sendSettlementRequestNotification(User owner, User tenant, com.krunity.HostelManagment.model.SettlementRequest settlement) {
        if (!emailEnabled || mailSender == null) {
            log.info("{}", "Email notification disabled. Would send settlement request notification to owner: " + owner.getUsername());
            return;
        }
        
        try {
            String reviewUrl = frontendUrl + "/owner/settlements";
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(owner.getUsername() + "@example.com");
            message.setSubject("New Settlement Request - Hostel Management");
            message.setText(String.format(
                "Dear %s,\n\n" +
                "A new settlement request has been submitted by tenant %s.\n\n" +
                "Agreement ID: %s\n" +
                "Security Deposit: ₹%s\n" +
                "Tenant Notes: %s\n\n" +
                "Please review and process the settlement request:\n%s\n\n" +
                "Best regards,\n" +
                "Hostel Management System",
                owner.getDisplayName(),
                tenant.getDisplayName(),
                settlement.getAgreementId(),
                settlement.getSecurityDeposit(),
                settlement.getTenantNotes() != null ? settlement.getTenantNotes() : "No notes provided",
                reviewUrl
            ));
            
            mailSender.send(message);
            log.info("{}", "Settlement request notification sent to owner: " + owner.getUsername());
        } catch (Exception e) {
            log.error("{}", "Failed to send settlement request notification: " + e.getMessage());
        }
    }
    
    public void sendSettlementApprovalNotification(User tenant, com.krunity.HostelManagment.model.SettlementRequest settlement) {
        if (!emailEnabled || mailSender == null) {
            log.info("{}", "Email notification disabled. Would send settlement approval notification to tenant: " + tenant.getUsername());
            return;
        }
        
        try {
            String settlementUrl = frontendUrl + "/tenant-portal/settlements";
            String settlementType = settlement.getSettlementType();
            String amountText = settlementType.equals("TENANT_PAYABLE") ? 
                "You need to pay ₹" + settlement.getFinalSettlementAmount() :
                "You will receive ₹" + settlement.getFinalSettlementAmount();
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(tenant.getUsername() + "@example.com");
            message.setSubject("Settlement Approved - Hostel Management");
            message.setText(String.format(
                "Dear %s,\n\n" +
                "Your settlement request has been approved by the owner.\n\n" +
                "Settlement Details:\n" +
                "- Security Deposit: ₹%s\n" +
                "- Total Deductions: ₹%s\n" +
                "- Final Amount: %s\n\n" +
                "Owner Notes: %s\n\n" +
                "Please check your dashboard for next steps:\n%s\n\n" +
                "Best regards,\n" +
                "Hostel Management System",
                tenant.getDisplayName(),
                settlement.getSecurityDeposit(),
                settlement.getTotalDeductions(),
                amountText,
                settlement.getOwnerNotes() != null ? settlement.getOwnerNotes() : "No additional notes",
                settlementUrl
            ));
            
            mailSender.send(message);
            log.info("{}", "Settlement approval notification sent to tenant: " + tenant.getUsername());
        } catch (Exception e) {
            log.error("{}", "Failed to send settlement approval notification: " + e.getMessage());
        }
    }
    
    public void sendSettlementRejectionNotification(User tenant, com.krunity.HostelManagment.model.SettlementRequest settlement) {
        if (!emailEnabled || mailSender == null) {
            log.info("{}", "Email notification disabled. Would send settlement rejection notification to tenant: " + tenant.getUsername());
            return;
        }
        
        try {
            String settlementUrl = frontendUrl + "/tenant-portal/settlements";
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(tenant.getUsername() + "@example.com");
            message.setSubject("Settlement Request Rejected - Hostel Management");
            message.setText(String.format(
                "Dear %s,\n\n" +
                "Your settlement request has been rejected by the owner.\n\n" +
                "Reason: %s\n\n" +
                "You can submit a new settlement request if needed:\n%s\n\n" +
                "Best regards,\n" +
                "Hostel Management System",
                tenant.getDisplayName(),
                settlement.getOwnerNotes() != null ? settlement.getOwnerNotes() : "No reason provided",
                settlementUrl
            ));
            
            mailSender.send(message);
            log.info("{}", "Settlement rejection notification sent to tenant: " + tenant.getUsername());
        } catch (Exception e) {
            log.error("{}", "Failed to send settlement rejection notification: " + e.getMessage());
        }
    }
    
    // ─── Allotment lifecycle reminders ───────────────────────────────────────

    /**
     * Sent by the cron job when an allotment enters SETTLEMENT_PENDING.
     * Reminds the tenant that their agreement is nearing its end date.
     */
    public void sendSettlementPendingReminder(User tenant, RoomAllotment allotment) {
        String roomNumber = allotment.getRoom() != null ? allotment.getRoom().getRoomNumber() : "N/A";
        String endDate   = allotment.getEndDate() != null ? allotment.getEndDate().toString() : "N/A";

        String body = String.format(
            "Dear %s,\n" +
            "Your hostel agreement for room %s is approaching its end date (%s).\n\n" +
            "Please log in to your tenant portal and submit a settlement request if you plan to vacate.\n" +
            "Portal: %s\n\n- Hostel Management",
            tenant.getDisplayName(), roomNumber, endDate,
            frontendUrl + "/tenant/settlements"
        );

        trySendSms(tenant.getPhoneNumber(), body, "settlement-pending reminder");
    }

    /**
     * Sent to the owner when a tenant requests settlement (SETTLEMENT_REQUESTED).
     */
    public void sendSettlementRequestedOwnerReminder(User owner, RoomAllotment allotment) {
        String tenantName = allotment.getTenant() != null ? allotment.getTenant().getDisplayName() : "N/A";
        String roomNumber = allotment.getRoom()   != null ? allotment.getRoom().getRoomNumber()   : "N/A";

        String body = String.format(
            "Dear %s,\n" +
            "Tenant %s (Room %s) has requested settlement. " +
            "Please review and approve it in your owner portal.\n" +
            "Portal: %s\n\n- Hostel Management",
            owner.getDisplayName(), tenantName, roomNumber,
            frontendUrl + "/owner/settlements"
        );

        trySendSms(owner.getPhoneNumber(), body, "settlement-requested owner reminder");
    }

    /**
     * Sent to whichever party (TENANT or OWNER) has not yet confirmed departure.
     *
     * @param recipient   the user who needs to confirm
     * @param allotment   the allotment awaiting confirmation
     * @param waitingFor  "TENANT" or "OWNER" — the role of the recipient
     */
    public void sendLeftConfirmationPendingReminder(User recipient, RoomAllotment allotment, String waitingFor) {
        String roomNumber = allotment.getRoom() != null ? allotment.getRoom().getRoomNumber() : "N/A";
        String portalPath = "TENANT".equals(waitingFor)
                ? "/tenant/settlements"
                : "/owner/settlements";

        String body = String.format(
            "Dear %s,\n" +
            "Please confirm the departure for room %s in your portal to complete the move-out process.\n" +
            "Portal: %s\n\n- Hostel Management",
            recipient.getDisplayName(), roomNumber,
            frontendUrl + portalPath
        );

        trySendSms(recipient.getPhoneNumber(), body, "left-confirmation pending reminder");
    }

    private void trySendSms(String phone, String body, String context) {
        try {
            smsService.sendMessage(phone, body);
        } catch (Exception e) {
            log.error("Failed to send {} SMS to {}: {}", context, phone, e.getMessage());
        }
    }

    public void sendSettlementCompletionNotification(User owner, User tenant, com.krunity.HostelManagment.model.SettlementRequest settlement) {
        if (!emailEnabled || mailSender == null) {
            System.out.println("Email notification disabled. Would send settlement completion notifications");
            return;
        }
        
        try {
            // Notification to owner
            SimpleMailMessage ownerMessage = new SimpleMailMessage();
            ownerMessage.setTo(owner.getUsername() + "@example.com");
            ownerMessage.setSubject("Settlement Completed - Hostel Management");
            ownerMessage.setText(String.format(
                "Dear %s,\n\n" +
                "The settlement for tenant %s has been completed successfully.\n\n" +
                "Agreement ID: %s\n" +
                "Final Amount: ₹%s (%s)\n" +
                "Payment Reference: %s\n" +
                "Completed At: %s\n\n" +
                "The agreement has been marked as settled.\n\n" +
                "Best regards,\n" +
                "Hostel Management System",
                owner.getDisplayName(),
                tenant.getDisplayName(),
                settlement.getAgreementId(),
                settlement.getFinalSettlementAmount(),
                settlement.getSettlementType(),
                settlement.getPaymentReference(),
                DATE_FMT.format(settlement.getSettledAt())
            ));
            
            // Notification to tenant
            SimpleMailMessage tenantMessage = new SimpleMailMessage();
            tenantMessage.setTo(tenant.getUsername() + "@example.com");
            tenantMessage.setSubject("Settlement Completed - Hostel Management");
            tenantMessage.setText(String.format(
                "Dear %s,\n\n" +
                "Your agreement settlement has been completed successfully.\n\n" +
                "Final Amount: ₹%s (%s)\n" +
                "Payment Reference: %s\n" +
                "Completed At: %s\n\n" +
                "Thank you for using our hostel management system.\n\n" +
                "Best regards,\n" +
                "Hostel Management System",
                tenant.getDisplayName(),
                settlement.getFinalSettlementAmount(),
                settlement.getSettlementType(),
                settlement.getPaymentReference(),
                DATE_FMT.format(settlement.getSettledAt())
            ));
            
            mailSender.send(ownerMessage);
            mailSender.send(tenantMessage);
            System.out.println("Settlement completion notifications sent to both parties");
        } catch (Exception e) {
            log.error("{}", "Failed to send settlement completion notifications: " + e.getMessage());
        }
    }
}