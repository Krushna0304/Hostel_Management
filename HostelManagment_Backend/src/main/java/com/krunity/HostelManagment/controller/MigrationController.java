package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.service.AgreementMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/migration")
public class MigrationController {

    @Autowired
    private AgreementMigrationService migrationService;

    /**
     * Fixes agreement end dates for all existing agreements
     */
    @PostMapping("/fix-agreement-end-dates")
    public ResponseEntity<Map<String, String>> fixAgreementEndDates() {
        try {
            migrationService.fixAgreementEndDates();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Agreement end dates migration completed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Migration failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Fixes a specific agreement by ID
     */
    @PostMapping("/fix-agreement/{agreementId}")
    public ResponseEntity<Map<String, String>> fixSpecificAgreement(@PathVariable String agreementId) {
        try {
            boolean success = migrationService.fixSpecificAgreement(agreementId);
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Agreement " + agreementId + " fixed successfully"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Failed to fix agreement " + agreementId
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Migration failed: " + e.getMessage()
            ));
        }
    }
}