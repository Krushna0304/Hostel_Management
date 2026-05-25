package com.krunity.HostelManagment.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Getter
public class TwilioConfig {
    
    @Value("${twilio.account-sid}")
    private String accountSid;
    
    @Value("${twilio.auth-token}")
    private String authToken;
    
    @Value("${twilio.phone-number}")
    private String phoneNumber;
    
    @Value("${twilio.enabled:false}")
    private boolean enabled;
    
    @PostConstruct
    public void initTwilio() {
        if (enabled) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized successfully with phone number: {}", phoneNumber);
        } else {
            log.warn("Twilio SMS is disabled. Set twilio.enabled=true to enable SMS sending.");
        }
    }
}
