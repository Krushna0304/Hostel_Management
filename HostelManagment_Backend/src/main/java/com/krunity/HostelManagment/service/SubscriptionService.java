package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.SubscriptionResponse;
import com.krunity.HostelManagment.enums.SubscriptionTier;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.OwnerSubscription;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.repository.OwnerSubscriptionRepository;
import com.krunity.HostelManagment.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class SubscriptionService {

    @Autowired
    private OwnerSubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get active subscription for an owner
     */
    public OwnerSubscription getActiveSubscription(UUID ownerId) {
        return subscriptionRepository.findByOwner_UserIdAndIsActiveTrue(ownerId)
                .orElse(createDefaultFreeSubscription(ownerId));
    }

    /**
     * Get subscription response DTO
     */
    public SubscriptionResponse getSubscriptionResponse(UUID ownerId) {
        OwnerSubscription subscription = getActiveSubscription(ownerId);
        return toResponse(subscription);
    }

    /**
     * Create default FREE subscription for new owners
     */
    @Transactional
    public OwnerSubscription createDefaultFreeSubscription(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner not found"));

        OwnerSubscription subscription = OwnerSubscription.builder()
                .owner(owner)
                .tier(SubscriptionTier.FREE)
                .startDate(LocalDateTime.now())
                .isActive(true)
                .smsRemindersEnabled(false)
                .emailRemindersEnabled(true)
                .customTemplatesEnabled(false)
                .maxHostels(1)
                .maxTenants(10)
                .build();

        return subscriptionRepository.save(subscription);
    }

    /**
     * Upgrade subscription tier
     */
    @Transactional
    public SubscriptionResponse upgradeSubscription(UUID ownerId, SubscriptionTier newTier) {
        OwnerSubscription subscription = getActiveSubscription(ownerId);
        
        // Deactivate old subscription
        subscription.setIsActive(false);
        subscriptionRepository.save(subscription);

        // Create new subscription with upgraded tier
        OwnerSubscription newSubscription = createSubscriptionForTier(subscription.getOwner(), newTier);
        return toResponse(newSubscription);
    }

    /**
     * Create subscription based on tier with appropriate features
     */
    @Transactional
    public OwnerSubscription createSubscriptionForTier(User owner, SubscriptionTier tier) {
        OwnerSubscription.OwnerSubscriptionBuilder builder = OwnerSubscription.builder()
                .owner(owner)
                .tier(tier)
                .startDate(LocalDateTime.now())
                .isActive(true);

        switch (tier) {
            case FREE:
                builder.smsRemindersEnabled(false)
                       .emailRemindersEnabled(true)
                       .customTemplatesEnabled(false)
                       .maxHostels(1)
                       .maxTenants(10);
                break;
            case BETA:
                builder.smsRemindersEnabled(true)
                       .emailRemindersEnabled(true)
                       .customTemplatesEnabled(false)
                       .maxHostels(3)
                       .maxTenants(50)
                       .endDate(LocalDateTime.now().plusMonths(6)); // 6 months beta
                break;
            case PRO:
                builder.smsRemindersEnabled(true)
                       .emailRemindersEnabled(true)
                       .customTemplatesEnabled(true)
                       .maxHostels(10)
                       .maxTenants(200);
                break;
            case ENTERPRISE:
                builder.smsRemindersEnabled(true)
                       .emailRemindersEnabled(true)
                       .customTemplatesEnabled(true)
                       .maxHostels(null) // Unlimited
                       .maxTenants(null); // Unlimited
                break;
        }

        return subscriptionRepository.save(builder.build());
    }

    /**
     * Toggle SMS reminders on/off for an owner (persists directly on the active subscription)
     */
    @Transactional
    public SubscriptionResponse toggleSmsReminders(UUID ownerId, boolean enabled) {
        OwnerSubscription subscription = getActiveSubscription(ownerId);
        subscription.setSmsRemindersEnabled(enabled);
        subscriptionRepository.save(subscription);
        log.info("SMS reminders {} for owner {}", enabled ? "enabled" : "disabled", ownerId);
        return toResponse(subscription);
    }

    /**
     * Check if owner has feature enabled
     */
    public boolean hasFeature(UUID ownerId, String feature) {
        OwnerSubscription subscription = getActiveSubscription(ownerId);
        
        switch (feature.toLowerCase()) {
            case "sms_reminders":
                return subscription.getSmsRemindersEnabled();
            case "email_reminders":
                return subscription.getEmailRemindersEnabled();
            case "custom_templates":
                return subscription.getCustomTemplatesEnabled();
            default:
                return false;
        }
    }

    private SubscriptionResponse toResponse(OwnerSubscription subscription) {
        return SubscriptionResponse.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .tier(subscription.getTier())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .isActive(subscription.getIsActive())
                .smsRemindersEnabled(subscription.getSmsRemindersEnabled())
                .emailRemindersEnabled(subscription.getEmailRemindersEnabled())
                .customTemplatesEnabled(subscription.getCustomTemplatesEnabled())
                .maxHostels(subscription.getMaxHostels())
                .maxTenants(subscription.getMaxTenants())
                .build();
    }
}
