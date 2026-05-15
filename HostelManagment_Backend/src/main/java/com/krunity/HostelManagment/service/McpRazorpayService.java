package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.McpOverrideRequest;
import com.krunity.HostelManagment.dto.RazorpayConfigResponse;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.OwnerRazorpayConfig;
import com.krunity.HostelManagment.repository.OwnerRazorpayConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MCP (Master Control Panel) service for monitoring and controlling
 * owner Razorpay integrations.
 */
@Slf4j
@Service
public class McpRazorpayService {

    @Autowired
    private OwnerRazorpayConfigRepository configRepository;

    /**
     * Get all owner Razorpay configurations (MCP view)
     */
    public List<RazorpayConfigResponse> getAllConfigurations() {
        return configRepository.findAll().stream()
                .map(RazorpayConfigResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get configuration for specific owner (MCP view)
     */
    public RazorpayConfigResponse getOwnerConfiguration(UUID ownerId) {
        OwnerRazorpayConfig config = configRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new NotFoundException("Configuration not found for owner"));
        return RazorpayConfigResponse.fromEntity(config);
    }

    /**
     * MCP override - Enable/Disable payments for an owner
     */
    @Transactional
    public RazorpayConfigResponse mcpOverride(UUID ownerId, McpOverrideRequest request, UUID mcpUserId) {
        log.info("MCP override for owner {}: disabled={}, reason={}", 
                ownerId, request.getDisabled(), request.getReason());

        OwnerRazorpayConfig config = configRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new NotFoundException("Configuration not found for owner"));

        config.setMcpOverrideDisabled(request.getDisabled());
        config.setMcpOverrideReason(request.getReason());
        config.setMcpOverrideBy(mcpUserId);
        config.setMcpOverrideAt(LocalDateTime.now());

        config = configRepository.save(config);

        log.info("✅ MCP override applied for owner: {}", ownerId);
        return RazorpayConfigResponse.fromEntity(config);
    }

    /**
     * Force re-verification for an owner
     */
    @Transactional
    public RazorpayConfigResponse forceReVerification(UUID ownerId) {
        log.info("MCP forcing re-verification for owner: {}", ownerId);

        OwnerRazorpayConfig config = configRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new NotFoundException("Configuration not found for owner"));

        config.setVerificationStatus(OwnerRazorpayConfig.VerificationStatus.NOT_VERIFIED);
        config.setLastVerifiedAt(null);
        config.setVerificationError("Re-verification required by MCP");
        config.setIsActive(false);

        config = configRepository.save(config);

        log.info("✅ Re-verification forced for owner: {}", ownerId);
        return RazorpayConfigResponse.fromEntity(config);
    }

    /**
     * Get statistics for MCP dashboard
     */
    public McpStatistics getStatistics() {
        List<OwnerRazorpayConfig> allConfigs = configRepository.findAll();

        long totalOwners = allConfigs.size();
        long verifiedOwners = allConfigs.stream()
                .filter(c -> c.getVerificationStatus() == OwnerRazorpayConfig.VerificationStatus.VERIFIED)
                .count();
        long activeOwners = allConfigs.stream()
                .filter(OwnerRazorpayConfig::isPaymentsEnabled)
                .count();
        long mcpDisabledOwners = allConfigs.stream()
                .filter(OwnerRazorpayConfig::getMcpOverrideDisabled)
                .count();

        return new McpStatistics(totalOwners, verifiedOwners, activeOwners, mcpDisabledOwners);
    }

    public static class McpStatistics {
        public final long totalOwners;
        public final long verifiedOwners;
        public final long activeOwners;
        public final long mcpDisabledOwners;

        public McpStatistics(long totalOwners, long verifiedOwners, long activeOwners, long mcpDisabledOwners) {
            this.totalOwners = totalOwners;
            this.verifiedOwners = verifiedOwners;
            this.activeOwners = activeOwners;
            this.mcpDisabledOwners = mcpDisabledOwners;
        }
    }
}
