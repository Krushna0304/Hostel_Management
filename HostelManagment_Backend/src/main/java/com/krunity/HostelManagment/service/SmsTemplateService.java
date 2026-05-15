package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.SmsTemplateRequest;
import com.krunity.HostelManagment.dto.SmsTemplateResponse;
import com.krunity.HostelManagment.enums.ReminderType;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.SmsTemplate;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.repository.SmsTemplateRepository;
import com.krunity.HostelManagment.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SmsTemplateService {

    @Autowired
    private SmsTemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    // Default templates
    private static final Map<ReminderType, String> DEFAULT_TEMPLATES = new HashMap<>();
    
    static {
        DEFAULT_TEMPLATES.put(
            ReminderType.BEFORE_DUE_DATE,
            "Hi {tenantName}, your rent of Rs.{amount} for {hostelName} - Room {roomNumber} is due tomorrow ({dueDate}). Please pay on time to avoid late fees."
        );
        DEFAULT_TEMPLATES.put(
            ReminderType.ON_DUE_DATE,
            "Hi {tenantName}, your rent of Rs.{amount} for {hostelName} - Room {roomNumber} is due today ({dueDate}). Please make the payment to avoid penalties."
        );
        DEFAULT_TEMPLATES.put(
            ReminderType.AFTER_DUE_DATE,
            "Hi {tenantName}, your rent payment is overdue! Total amount due: Rs.{totalAmount} (Rent: Rs.{amount} + Late Fee: Rs.{lateFee}). Please pay immediately for {hostelName} - Room {roomNumber}."
        );
        DEFAULT_TEMPLATES.put(
            ReminderType.OTHER_CHARGE,
            "Hi {tenantName}, a new charge '{chargeName}' of Rs.{amount} has been added to your account at {hostelName} - Room {roomNumber}. Due date: {dueDate}. Please log in to your portal to view details."
        );
    }

    /**
     * Get template for owner and reminder type
     */
    public String getTemplate(UUID ownerId, ReminderType reminderType) {
        // Check if owner has custom templates enabled
        if (subscriptionService.hasFeature(ownerId, "custom_templates")) {
            return templateRepository.findByOwner_UserIdAndReminderTypeAndIsActiveTrue(ownerId, reminderType)
                    .map(SmsTemplate::getTemplateContent)
                    .orElse(DEFAULT_TEMPLATES.get(reminderType));
        }
        
        // Return default template
        return DEFAULT_TEMPLATES.get(reminderType);
    }

    /**
     * Create or update SMS template
     */
    @Transactional
    public SmsTemplateResponse createOrUpdateTemplate(UUID ownerId, SmsTemplateRequest request) {
        // Check if owner has custom templates feature
        if (!subscriptionService.hasFeature(ownerId, "custom_templates")) {
            throw new IllegalStateException("Custom templates are not available in your subscription plan. Please upgrade to PRO or ENTERPRISE.");
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner not found"));

        // Check if template already exists
        SmsTemplate template = templateRepository
                .findByOwner_UserIdAndReminderTypeAndIsActiveTrue(ownerId, request.getReminderType())
                .orElse(null);

        if (template != null) {
            // Update existing template
            template.setTemplateContent(request.getTemplateContent());
        } else {
            // Create new template
            template = SmsTemplate.builder()
                    .owner(owner)
                    .reminderType(request.getReminderType())
                    .templateContent(request.getTemplateContent())
                    .isActive(true)
                    .build();
        }

        template = templateRepository.save(template);
        return toResponse(template);
    }

    /**
     * Get all templates for owner
     */
    public List<SmsTemplateResponse> getOwnerTemplates(UUID ownerId) {
        List<SmsTemplate> templates = templateRepository.findByOwner_UserIdAndIsActiveTrue(ownerId);
        return templates.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all available templates (custom + default)
     */
    public Map<ReminderType, String> getAllTemplatesForOwner(UUID ownerId) {
        Map<ReminderType, String> templates = new HashMap<>(DEFAULT_TEMPLATES);
        
        if (subscriptionService.hasFeature(ownerId, "custom_templates")) {
            List<SmsTemplate> customTemplates = templateRepository.findByOwner_UserIdAndIsActiveTrue(ownerId);
            customTemplates.forEach(t -> templates.put(t.getReminderType(), t.getTemplateContent()));
        }
        
        return templates;
    }

    /**
     * Delete template
     */
    @Transactional
    public void deleteTemplate(UUID ownerId, UUID templateId) {
        SmsTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found"));

        if (!template.getOwner().getUserId().equals(ownerId)) {
            throw new IllegalStateException("You don't have permission to delete this template");
        }

        template.setIsActive(false);
        templateRepository.save(template);
    }

    /**
     * Replace placeholders in template
     */
    public String replacePlaceholders(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private SmsTemplateResponse toResponse(SmsTemplate template) {
        return SmsTemplateResponse.builder()
                .templateId(template.getTemplateId())
                .reminderType(template.getReminderType())
                .templateContent(template.getTemplateContent())
                .isActive(template.getIsActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
