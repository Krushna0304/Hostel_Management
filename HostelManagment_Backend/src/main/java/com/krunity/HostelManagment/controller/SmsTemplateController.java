package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.SmsTemplateRequest;
import com.krunity.HostelManagment.dto.SmsTemplateResponse;
import com.krunity.HostelManagment.enums.ReminderType;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.service.SmsTemplateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/owner/sms-templates")
public class SmsTemplateController {

    @Autowired
    private SmsTemplateService templateService;

    /**
     * Get all templates for logged-in owner
     */
    @GetMapping
    public ResponseEntity<List<SmsTemplateResponse>> getMyTemplates() {
        User owner = ApplicationContext.getUser();
        List<SmsTemplateResponse> templates = templateService.getOwnerTemplates(owner.getUserId());
        return ResponseEntity.ok(templates);
    }

    /**
     * Get all available templates (custom + default)
     */
    @GetMapping("/all")
    public ResponseEntity<Map<ReminderType, String>> getAllTemplates() {
        User owner = ApplicationContext.getUser();
        Map<ReminderType, String> templates = templateService.getAllTemplatesForOwner(owner.getUserId());
        return ResponseEntity.ok(templates);
    }

    /**
     * Create or update SMS template
     */
    @PostMapping
    public ResponseEntity<?> createOrUpdateTemplate(@Valid @RequestBody SmsTemplateRequest request) {
        try {
            User owner = ApplicationContext.getUser();
            SmsTemplateResponse response = templateService.createOrUpdateTemplate(owner.getUserId(), request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete template
     */
    @DeleteMapping("/{templateId}")
    public ResponseEntity<?> deleteTemplate(@PathVariable UUID templateId) {
        try {
            User owner = ApplicationContext.getUser();
            templateService.deleteTemplate(owner.getUserId(), templateId);
            return ResponseEntity.ok(Map.of("message", "Template deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
