package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.McpOverrideRequest;
import com.krunity.HostelManagment.dto.RazorpayConfigResponse;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.service.McpRazorpayService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * MCP (Master Control Panel) endpoints for monitoring and controlling
 * owner Razorpay integrations.
 * 
 * MCP can:
 * - View all owner configurations
 * - Enable/Disable payments per owner
 * - Force re-verification
 * - View statistics
 * 
 * MCP cannot:
 * - View or access raw Razorpay secrets
 */
@Slf4j
@RestController
@RequestMapping("/mcp/payment-monitoring")
@PreAuthorize("hasRole('MCP') or hasRole('ADMIN')")
public class McpRazorpayController {

    @Autowired
    private McpRazorpayService mcpService;

    /**
     * Get all owner Razorpay configurations
     */
    @GetMapping("/configurations")
    public ResponseEntity<List<RazorpayConfigResponse>> getAllConfigurations() {
        List<RazorpayConfigResponse> configs = mcpService.getAllConfigurations();
        return ResponseEntity.ok(configs);
    }

    /**
     * Get configuration for specific owner
     */
    @GetMapping("/configurations/{ownerId}")
    public ResponseEntity<RazorpayConfigResponse> getOwnerConfiguration(@PathVariable UUID ownerId) {
        RazorpayConfigResponse config = mcpService.getOwnerConfiguration(ownerId);
        return ResponseEntity.ok(config);
    }

    /**
     * Enable/Disable payments for an owner (MCP override)
     */
    @PostMapping("/configurations/{ownerId}/override")
    public ResponseEntity<RazorpayConfigResponse> mcpOverride(
            @PathVariable UUID ownerId,
            @Valid @RequestBody McpOverrideRequest request) {
        
        User mcp = ApplicationContext.getUser();
        RazorpayConfigResponse result = mcpService.mcpOverride(ownerId, request, mcp.getUserId());
        return ResponseEntity.ok(result);
    }

    /**
     * Force re-verification for an owner
     */
    @PostMapping("/configurations/{ownerId}/force-reverify")
    public ResponseEntity<RazorpayConfigResponse> forceReVerification(@PathVariable UUID ownerId) {
        RazorpayConfigResponse result = mcpService.forceReVerification(ownerId);
        return ResponseEntity.ok(result);
    }

    /**
     * Get MCP dashboard statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<McpRazorpayService.McpStatistics> getStatistics() {
        McpRazorpayService.McpStatistics stats = mcpService.getStatistics();
        return ResponseEntity.ok(stats);
    }
}
