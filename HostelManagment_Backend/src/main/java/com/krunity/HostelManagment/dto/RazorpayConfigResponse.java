package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.model.OwnerRazorpayConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayConfigResponse {
    
    private UUID configId;
    private String keyId; // Partially masked
    private String verificationStatus;
    private LocalDateTime lastVerifiedAt;
    private String verificationError;
    private Boolean isActive;
    private Boolean mcpOverrideDisabled;
    private String mcpOverrideReason;
    private Boolean paymentsEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static RazorpayConfigResponse fromEntity(OwnerRazorpayConfig config) {
        if (config == null) {
            return null;
        }
        
        return RazorpayConfigResponse.builder()
                .configId(config.getConfigId())
                .keyId(maskKeyId(config.getRazorpayKeyId()))
                .verificationStatus(config.getVerificationStatus().name())
                .lastVerifiedAt(config.getLastVerifiedAt())
                .verificationError(config.getVerificationError())
                .isActive(config.getIsActive())
                .mcpOverrideDisabled(config.getMcpOverrideDisabled())
                .mcpOverrideReason(config.getMcpOverrideReason())
                .paymentsEnabled(config.isPaymentsEnabled())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
    
    private static String maskKeyId(String keyId) {
        if (keyId == null || keyId.length() < 10) {
            return "***";
        }
        // Show first 8 chars and last 4 chars
        return keyId.substring(0, 8) + "..." + keyId.substring(keyId.length() - 4);
    }
}
