package com.krunity.HostelManagment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores encrypted Razorpay credentials for each Owner.
 * Implements "Bring Your Own Razorpay" model.
 */
@Entity
@Table(name = "owner_razorpay_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerRazorpayConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "config_id", updatable = false, nullable = false)
    private UUID configId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(name = "razorpay_key_id", nullable = false, length = 100)
    private String razorpayKeyId;

    /**
     * Encrypted Razorpay Key Secret using AES-256
     * Never expose this in API responses
     */
    @Column(name = "razorpay_key_secret_encrypted", nullable = false, length = 500)
    private String razorpayKeySecretEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.NOT_VERIFIED;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "verification_error", length = 500)
    private String verificationError;

    /**
     * Owner can activate/deactivate their own payments
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    /**
     * MCP can override and disable payments for any owner
     * Takes precedence over isActive
     */
    @Column(name = "mcp_override_disabled", nullable = false)
    @Builder.Default
    private Boolean mcpOverrideDisabled = false;

    @Column(name = "mcp_override_reason", length = 500)
    private String mcpOverrideReason;

    @Column(name = "mcp_override_by")
    private UUID mcpOverrideBy;

    @Column(name = "mcp_override_at")
    private LocalDateTime mcpOverrideAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Check if payments are enabled for this owner
     */
    public boolean isPaymentsEnabled() {
        return isActive 
            && !mcpOverrideDisabled 
            && verificationStatus == VerificationStatus.VERIFIED;
    }

    public enum VerificationStatus {
        NOT_VERIFIED,
        VERIFYING,
        VERIFIED,
        FAILED
    }
}
