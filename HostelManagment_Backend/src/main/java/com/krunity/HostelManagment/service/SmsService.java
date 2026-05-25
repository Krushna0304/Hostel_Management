package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.config.TwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {
    
    @Autowired
    private TwilioConfig twilioConfig;
    
    public void sendSms(String toPhoneNumber, String messageBody) {
        if (!twilioConfig.isEnabled()) {
            log.info("SMS Service Disabled - Would send to: {}", toPhoneNumber);
            log.debug("Message content: {}", messageBody);
            return;
        }
        
        try {
            // Format phone number (ensure it starts with country code)
            String formattedPhone = formatPhoneNumber(toPhoneNumber);
            
            Message message = Message.creator(
                new PhoneNumber(formattedPhone),  // To number
                new PhoneNumber(twilioConfig.getPhoneNumber()),  // From number (Twilio number)
                messageBody
            ).create();
            
            log.info("SMS sent successfully - SID: {}, To: {}, Status: {}", message.getSid(), formattedPhone, message.getStatus());
            
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", toPhoneNumber, e.getMessage(), e);
            // Don't throw exception - log and continue
            // In production, you might want to implement retry logic or queue failed messages
        }
    }
    
    private String formatPhoneNumber(String phoneNumber) {
        // Remove all non-digit characters
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        
        // If it doesn't start with +, assume it's an Indian number and add +91
        if (!cleaned.startsWith("+")) {
            // Remove leading 0 if present (common in Indian numbers)
            if (cleaned.startsWith("0")) {
                cleaned = cleaned.substring(1);
            }
            cleaned = "+91" + cleaned;  // Change to your country code
        }
        
        return cleaned;
    }
    
    public void sendOtp(String toPhoneNumber, String otp) {
        String message = String.format(
            "Your cash payment verification OTP is: %s\n\n" +
            "This OTP is valid for 10 minutes.\n\n" +
            "Do not share this OTP with anyone.\n\n" +
            "- Hostel Management System",
            otp
        );
        
        sendSms(toPhoneNumber, message);
    }
    
    /**
     * Send custom message (for reminders, notifications, etc.)
     */
    public void sendMessage(String toPhoneNumber, String messageBody) {
        sendSms(toPhoneNumber, messageBody);
    }
}
