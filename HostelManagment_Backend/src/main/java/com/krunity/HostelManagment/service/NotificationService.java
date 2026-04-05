package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;
    
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
    
    public void sendQrActivationSms(Agreement agreement, User tenant) {
        // SMS implementation - integrate with SMS gateway (Twilio, AWS SNS, etc.)
        System.out.println(String.format(
            "SMS notification: Agreement activation QR sent to %s. Token: %s",
            tenant.getPhoneNumber(),
            agreement.getQrToken()
        ));
        
        // Example integration with SMS service:
        // smsService.send(tenant.getPhoneNumber(), 
        //     "Your agreement activation link: " + frontendUrl + "/tenant/activate?token=" + agreement.getQrToken());
    }
    
    public void sendAgreementAcceptedNotification(Agreement agreement, User owner) {
        if (!emailEnabled || mailSender == null) {
            System.out.println("Email notification: Agreement " + agreement.getId() + " has been accepted by tenant.");
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(owner.getUsername() + "@example.com");
            message.setSubject("Agreement Accepted - Hostel Management");
            message.setText(String.format(
                "Dear %s,\n\n" +
                "The agreement %s has been accepted and activated by the tenant.\n\n" +
                "Agreement Details:\n" +
                "- Type: %s\n" +
                "- Rent: %s\n" +
                "- Status: %s\n\n" +
                "Thank you,\nHostel Management System",
                owner.getDisplayName(),
                agreement.getId(),
                agreement.getType(),
                agreement.getRent(),
                agreement.getStatus()
            ));
            
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
}

