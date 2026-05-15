package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
            System.out.println("Email notification disabled. Would send QR activation email to: " + tenant.getUsername());
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
            System.err.println("Failed to send email: " + e.getMessage());
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
            System.out.printf("📱 Agreement creation SMS sent to tenant %s (%s)%n",
                    tenant.getDisplayName(), tenant.getPhoneNumber());

        } catch (Exception e) {
            System.err.println("Failed to send agreement creation SMS to tenant: " + e.getMessage());
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
            System.out.printf("📱 Agreement acceptance SMS sent to owner %s (%s)%n",
                    owner.getDisplayName(), owner.getPhoneNumber());

        } catch (Exception e) {
            System.err.println("Failed to send agreement acceptance SMS to owner: " + e.getMessage());
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
                System.err.println("Failed to send agreement acceptance email: " + e.getMessage());
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
            System.out.printf("📱 Agreement activation confirmation SMS sent to tenant %s (%s)%n",
                    tenant.getDisplayName(), tenant.getPhoneNumber());

        } catch (Exception e) {
            System.err.println("Failed to send agreement activation SMS to tenant: " + e.getMessage());
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
        System.out.println(String.format(
            "💰 Cash Payment OTP sent to owner %s (%s). OTP: %s",
            owner.getDisplayName(),
            owner.getPhoneNumber(),
            otp
        ));
    }
    
    /**
     * Send SMS notification
     */
    public void sendSms(String phoneNumber, String message) {
        try {
            smsService.sendMessage(phoneNumber, message);
            System.out.println(String.format(
                "📱 SMS sent to %s: %s",
                phoneNumber,
                message
            ));
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Send email notification
     */
    public void sendEmail(String toEmail, String subject, String body) {
        if (!emailEnabled || mailSender == null) {
            System.out.println(String.format(
                "📧 Email notification disabled. Would send to %s: %s",
                toEmail,
                subject
            ));
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            System.out.println(String.format(
                "📧 Email sent to %s: %s",
                toEmail,
                subject
            ));
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            throw e;
        }
    }
}
