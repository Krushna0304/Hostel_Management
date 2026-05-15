package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.RazorpayConfigRequest;
import com.krunity.HostelManagment.dto.RazorpayConfigResponse;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.service.OwnerRazorpayService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Owner-only endpoints for managing Razorpay credentials.
 * Implements "Bring Your Own Razorpay" model.
 */
@Slf4j
@RestController
@RequestMapping("/owner/payment-settings")
//@PreAuthorize("hasRole('OWNER')")
public class OwnerRazorpayController {

    @Autowired
    private OwnerRazorpayService razorpayService;

    /**
     * Get current Razorpay configuration
     */
    @GetMapping
    public ResponseEntity<RazorpayConfigResponse> getConfiguration() {
        User owner = ApplicationContext.getUser();
        RazorpayConfigResponse config = razorpayService.getOwnerConfig(owner.getUserId());
        return ResponseEntity.ok(config);
    }

    /**
     * Test Razorpay connection with provided credentials
     */
    @PostMapping("/test-connection")
    public ResponseEntity<?> testConnection(@Valid @RequestBody RazorpayConfigRequest request) {
        try {
            User owner = ApplicationContext.getUser();
            RazorpayConfigResponse result = razorpayService.testConnection(owner.getUserId(), request, owner);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test connection failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Connection test failed",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Save and activate Razorpay payments
     */
    @PostMapping("/save-and-activate")
    public ResponseEntity<?> saveAndActivate(@Valid @RequestBody RazorpayConfigRequest request) {
        try {
            User owner = ApplicationContext.getUser();
            RazorpayConfigResponse result = razorpayService.saveAndActivate(owner.getUserId(), request, owner);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Activation failed",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Save and activate failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Deactivate payments
     */
    @PostMapping("/deactivate")
    public ResponseEntity<RazorpayConfigResponse> deactivate() {
        User owner = ApplicationContext.getUser();
        RazorpayConfigResponse result = razorpayService.deactivate(owner.getUserId());
        return ResponseEntity.ok(result);
    }

    /**
     * Check if payments are enabled
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        User owner = ApplicationContext.getUser();
        boolean enabled = razorpayService.isPaymentsEnabled(owner.getUserId());
        return ResponseEntity.ok(Map.of("paymentsEnabled", enabled));
    }
}
