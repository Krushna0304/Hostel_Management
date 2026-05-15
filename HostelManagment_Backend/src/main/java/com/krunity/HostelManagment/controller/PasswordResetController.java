package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.service.PasswordResetService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/password-reset")
public class PasswordResetController {
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    @PostMapping("/generate/{userId}")
    public ResponseEntity<?> generateResetToken(@PathVariable UUID userId) {
        String token = passwordResetService.generatePasswordResetToken(userId);
        return ResponseEntity.ok(new TokenResponse(token));
    }
    
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(new SuccessResponse("Password reset successfully"));
    }
    
    @GetMapping("/validate/{token}")
    public ResponseEntity<?> validateToken(@PathVariable String token) {
        boolean isValid = passwordResetService.validateToken(token);
        return ResponseEntity.ok(new ValidationResponse(isValid));
    }
    
    @Data
    private static class TokenResponse {
        private String token;
        public TokenResponse(String token) { this.token = token; }
    }
    
    @Data
    private static class ResetPasswordRequest {
        @NotBlank
        private String token;
        @NotBlank
        private String newPassword;
    }
    
    @Data
    private static class SuccessResponse {
        private String message;
        public SuccessResponse(String message) { this.message = message; }
    }
    
    @Data
    private static class ErrorResponse {
        private String error;
        public ErrorResponse(String error) { this.error = error; }
    }
    
    @Data
    private static class ValidationResponse {
        private boolean valid;
        public ValidationResponse(boolean valid) { this.valid = valid; }
    }
}

