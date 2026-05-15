package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.SubscriptionTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "owner_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "subscription_id", updatable = false, nullable = false)
    private UUID subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)
    private SubscriptionTier tier;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Feature flags based on subscription tier
    @Column(name = "sms_reminders_enabled", nullable = false)
    @Builder.Default
    private Boolean smsRemindersEnabled = false;

    @Column(name = "email_reminders_enabled", nullable = false)
    @Builder.Default
    private Boolean emailRemindersEnabled = false;

    @Column(name = "custom_templates_enabled", nullable = false)
    @Builder.Default
    private Boolean customTemplatesEnabled = false;

    @Column(name = "max_hostels")
    private Integer maxHostels;

    @Column(name = "max_tenants")
    private Integer maxTenants;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
