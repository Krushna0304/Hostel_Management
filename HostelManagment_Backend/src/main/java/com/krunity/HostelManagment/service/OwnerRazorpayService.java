package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.RazorpayConfigRequest;
import com.krunity.HostelManagment.dto.RazorpayConfigResponse;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.OwnerRazorpayConfig;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.repository.OwnerRazorpayConfigRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing Owner's Razorpay credentials.
 * Implements "Bring Your Own Razorpay" model.
 */
@Slf4j
@Service
public class OwnerRazorpayService {

    @Autowired
    private OwnerRazorpayConfigRepository configRepository;

    @Autowired
    private EncryptionService encryptionService;

    /**
     * Get owner's Razorpay configuration
     */
    public RazorpayConfigResponse getOwnerConfig(UUID ownerId) {
        OwnerRazorpayConfig config = configRepository.findByOwner_UserId(ownerId)
                .orElse(null);
        return RazorpayConfigResponse.fromEntity(config);
    }

    /**
     * Test Razorpay credentials by making an authenticated API call
     */
    @Transactional
    public RazorpayConfigResponse testConnection(UUID ownerId, RazorpayConfigRequest request, User owner) {
        log.info("Testing Razorpay connection for owner: {}", ownerId);

        // Get or create config
        OwnerRazorpayConfig config = configRepository.findByOwner_UserId(ownerId)
                .orElse(OwnerRazorpayConfig.builder()
                        .owner(owner)
                        .build());

        // Update credentials
        config.setRazorpayKeyId(request.getKeyId());
        config.setRazorpayKeySecretEncrypted(encryptionService.encrypt(request.getKeySecret()));
        config.setVerificationStatus(OwnerRazorpayConfig.VerificationStatus.VERIFYING);
        config.setVerificationError(null);

        config = configRepository.save(config);

        // Test connection
        try {
            RazorpayClient client = new RazorpayClient(request.getKeyId(), request.getKeySecret());
            
            // Make a test API call - fetch orders with limit 1
            org.json.JSONObject options = new org.json.JSONObject();
            options.put("count", 1);
            client.orders.fetchAll(options);

            // Success
            config.setVerificationStatus(OwnerRazorpayConfig.VerificationStatus.VERIFIED);
            config.setLastVerifiedAt(LocalDateTime.now());
            config.setVerificationError(null);
            
            log.info("✅ Razorpay credentials verified for owner: {}", ownerId);

        } catch (RazorpayException e) {
            // Failed
            config.setVerificationStatus(OwnerRazorpayConfig.VerificationStatus.FAILED);
            config.setVerificationError(e.getMessage());
            
            log.error("❌ Razorpay verification failed for owner {}: {}", ownerId, e.getMessage());
        }

        config = configRepository.save(config);
        return RazorpayConfigResponse.fromEntity(config);
    }

    /**
     * Save and activate Razorpay configuration
     */
    @Transactional
    public RazorpayConfigResponse saveAndActivate(UUID ownerId, RazorpayConfigRequest request, User owner) {
        log.info("Saving and activating Razorpay config for owner: {}", ownerId);

        // First test the connection
        RazorpayConfigResponse testResult = testConnection(ownerId, request, owner);

        if (!"VERIFIED".equals(testResult.getVerificationStatus())) {
            throw new IllegalStateException("Cannot activate. Verification failed: " + testResult.getVerificationError());
        }

        // Activate
        OwnerRazorpayConfig config = configRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new NotFoundException("Configuration not found"));

        config.setIsActive(true);
        config = configRepository.save(config);

        log.info("✅ Razorpay payments activated for owner: {}", ownerId);
        return RazorpayConfigResponse.fromEntity(config);
    }

    /**
     * Deactivate payments
     */
    @Transactional
    public RazorpayConfigResponse deactivate(UUID ownerId) {
        OwnerRazorpayConfig config = configRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new NotFoundException("Configuration not found"));

        config.setIsActive(false);
        config = configRepository.save(config);

        log.info("Razorpay payments deactivated for owner: {}", ownerId);
        return RazorpayConfigResponse.fromEntity(config);
    }

    /**
     * Get decrypted Razorpay credentials for payment processing
     * INTERNAL USE ONLY - Never expose via API
     */
    public RazorpayCredentials getDecryptedCredentials(UUID ownerId) {
        OwnerRazorpayConfig config = configRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new NotFoundException("Razorpay configuration not found for owner"));

        if (!config.isPaymentsEnabled()) {
            throw new IllegalStateException("Payments are not enabled for this owner");
        }

        String decryptedSecret = encryptionService.decrypt(config.getRazorpayKeySecretEncrypted());

        return new RazorpayCredentials(config.getRazorpayKeyId(), decryptedSecret);
    }

    /**
     * Check if owner has payments enabled
     */
    public boolean isPaymentsEnabled(UUID ownerId) {
        return configRepository.findByOwner_UserId(ownerId)
                .map(OwnerRazorpayConfig::isPaymentsEnabled)
                .orElse(false);
    }

    /**
     * Internal DTO for credentials (never exposed via API)
     */
    public static class RazorpayCredentials {
        public final String keyId;
        public final String keySecret;

        public RazorpayCredentials(String keyId, String keySecret) {
            this.keyId = keyId;
            this.keySecret = keySecret;
        }
    }
}
