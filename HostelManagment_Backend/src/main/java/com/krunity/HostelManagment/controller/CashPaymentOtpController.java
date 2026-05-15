package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.service.CashPaymentOtpService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cash-payment-otp")
public class CashPaymentOtpController {
    
    @Autowired
    private CashPaymentOtpService otpService;
    
    @PostMapping("/send/{agreementId}")
    public ResponseEntity<?> sendOtp(@PathVariable String agreementId) {
        String message = otpService.generateAndSendOtp(agreementId);
        return ResponseEntity.ok(new OtpResponse(message));
    }
    
    @PostMapping("/send-installment/{scheduleId}")
    public ResponseEntity<?> sendInstallmentOtp(@PathVariable String scheduleId) {
        try {
            // Convert string to UUID if needed
            UUID scheduleUuid = UUID.fromString(scheduleId);
            String message = otpService.generateAndSendInstallmentOtp(scheduleUuid.toString());
            return ResponseEntity.ok(new OtpResponse(message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new OtpResponse("Invalid schedule ID format"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new OtpResponse("Failed to send OTP: " + e.getMessage()));
        }
    }
    
    @PostMapping("/send-other-charge/{chargeId}")
    public ResponseEntity<?> sendOtherChargeOtp(@PathVariable String chargeId) {
        try {
            String message = otpService.generateAndSendOtherChargeOtp(chargeId);
            return ResponseEntity.ok(new OtpResponse(message));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new OtpResponse("Failed to send OTP: " + e.getMessage()));
        }
    }
    
    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        boolean isValid = otpService.verifyOtp(request.getAgreementId(), request.getOtp());
        return ResponseEntity.ok(new VerificationResponse(isValid));
    }
    
    @Data
    private static class OtpResponse {
        private String message;
        public OtpResponse(String message) { this.message = message; }
    }
    
    @Data
    private static class VerifyOtpRequest {
        private String agreementId;
        private String otp;
    }
    
    @Data
    private static class VerificationResponse {
        private boolean valid;
        public VerificationResponse(boolean valid) { this.valid = valid; }
    }
}
