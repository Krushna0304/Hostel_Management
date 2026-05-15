package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.SubscriptionResponse;
import com.krunity.HostelManagment.enums.SubscriptionTier;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/owner/subscription")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * Get current subscription
     */
    @GetMapping
    public ResponseEntity<SubscriptionResponse> getMySubscription() {
        User owner = ApplicationContext.getUser();
        SubscriptionResponse response = subscriptionService.getSubscriptionResponse(owner.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * Upgrade subscription
     */
    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradeSubscription(@RequestBody Map<String, String> request) {
        try {
            User owner = ApplicationContext.getUser();
            String tierStr = request.get("tier");
            SubscriptionTier tier = SubscriptionTier.valueOf(tierStr.toUpperCase());
            
            SubscriptionResponse response = subscriptionService.upgradeSubscription(owner.getUserId(), tier);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid subscription tier"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Toggle SMS reminders on/off
     */
    @PostMapping("/toggle-sms-reminders")
    public ResponseEntity<?> toggleSmsReminders(@RequestBody Map<String, Boolean> request) {
        try {
            User owner = ApplicationContext.getUser();
            boolean enabled = Boolean.TRUE.equals(request.get("enabled"));
            SubscriptionResponse response = subscriptionService.toggleSmsReminders(owner.getUserId(), enabled);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if feature is available
     */
    @GetMapping("/feature/{featureName}")
    public ResponseEntity<Map<String, Boolean>> checkFeature(@PathVariable String featureName) {
        User owner = ApplicationContext.getUser();
        boolean hasFeature = subscriptionService.hasFeature(owner.getUserId(), featureName);
        return ResponseEntity.ok(Map.of("available", hasFeature));
    }
}
