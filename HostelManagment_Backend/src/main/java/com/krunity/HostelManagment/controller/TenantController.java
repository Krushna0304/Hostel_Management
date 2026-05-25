package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.InstallmentResponse;
import com.krunity.HostelManagment.dto.PaymentLedgerResponse;
import com.krunity.HostelManagment.dto.RecordPaymentRequest;
import com.krunity.HostelManagment.dto.TenantDashboardResponse;
import com.krunity.HostelManagment.model.RoomAllotment;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.repository.AgreementRepository;
import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import com.krunity.HostelManagment.service.PaymentScheduleService;
import com.krunity.HostelManagment.service.TenantDashboardService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/tenant")
public class TenantController {

    @Autowired
    private TenantDashboardService tenantDashboardService;

    @Autowired
    private PaymentScheduleService paymentScheduleService;

    @Autowired
    private RoomAllotmentRepository roomAllotmentRepository;

    @Autowired
    private AgreementRepository agreementRepository;

    /**
     * GET /tenant/dashboard
     * Returns the tenant's room allotment info + payment plan summary + next due installment.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            User tenant = ApplicationContext.getUser();
            TenantDashboardResponse response = tenantDashboardService.getTenantDashboard(tenant.getUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /**
     * GET /tenant/payment-schedule
     * Returns the full installment schedule with status for the logged-in tenant.
     */
    @GetMapping("/payment-schedule")
    public ResponseEntity<?> getPaymentSchedule() {
        try {
            User tenant = ApplicationContext.getUser();
            PaymentLedgerResponse ledger = paymentScheduleService.getTenantLedger(tenant.getUserId());
            return ResponseEntity.ok(ledger);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /**
     * GET /tenant/agreement
     * Returns the full agreement (with planSnapshot) for the logged-in tenant.
     */
    @GetMapping("/agreement")
    public ResponseEntity<?> getMyAgreement() {
        User tenant = ApplicationContext.getUser();

        // Get active allotment to find agreementId
        RoomAllotment allotment = roomAllotmentRepository
                .findByTenant_UserIdAndRoomAllotmentStatus(
                        tenant.getUserId(),
                        com.krunity.HostelManagment.enums.RoomAllotmentStatus.CONFIRMED)
                .orElseThrow(() -> new com.krunity.HostelManagment.exception.NotFoundException("No active allotment found"));

        String agreementId = allotment.getAgreementId();

        com.krunity.HostelManagment.model.Agreement agreement =
                agreementRepository.findById(agreementId)
                        .orElseThrow(() -> new com.krunity.HostelManagment.exception.NotFoundException("Agreement not found"));

        return ResponseEntity.ok(com.krunity.HostelManagment.Mapper.AgreementMapper.toResponse(agreement));
    }

    /**
     * GET /tenant/agreements
     * Returns all agreements for the logged-in tenant (for settlement purposes).
     */
    @GetMapping("/agreements")
    public ResponseEntity<?> getTenantAgreements() {
        User tenant = ApplicationContext.getUser();
        
        // Find all agreements for this tenant
        java.util.List<com.krunity.HostelManagment.model.Agreement> agreements = 
                agreementRepository.findByUserId(tenant.getUserId());
        
        java.util.List<com.krunity.HostelManagment.dto.AgreementResponse> responses = 
                agreements.stream()
                    .map(com.krunity.HostelManagment.Mapper.AgreementMapper::toResponse)
                    .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    @PostMapping("/pay/{scheduleId}")
    public ResponseEntity<?> recordPayment(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody RecordPaymentRequest request) {
        try {
            log.debug("=== TENANT PAYMENT REQUEST START ===");
            log.debug("Schedule ID: {}", scheduleId);
            log.debug("Request Amount: {}", request.getAmount());
            log.debug("Payment Mode: {}", request.getPaymentMode());
            log.debug("OTP provided: {}", request.getOtp() != null ? "Yes (length: " + request.getOtp().length() + ")" : "No");
            log.debug("Razorpay Order ID: {}", request.getRazorpayOrderId());
            
            User tenant = ApplicationContext.getUser();
            log.debug("Tenant: {} (ID: {})", tenant.getUsername(), tenant.getUserId());

            // Resolve the owner from the tenant's allotment
            RoomAllotment allotment = roomAllotmentRepository
                    .findByTenant_UserIdAndRoomAllotmentStatus(
                            tenant.getUserId(),
                            com.krunity.HostelManagment.enums.RoomAllotmentStatus.CONFIRMED)
                    .orElseThrow(() -> new RuntimeException("No active allotment found"));

            User owner = allotment.getRoom().getHostel().getOwner();
            log.debug("Owner: {} (ID: {})", owner.getUsername(), owner.getUserId());

            log.debug("Calling PaymentScheduleService.recordPayment...");
            InstallmentResponse response = paymentScheduleService.recordPayment(scheduleId, request, tenant, owner);
            log.debug("Payment recorded successfully!");
            log.debug("=== TENANT PAYMENT REQUEST END ===");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.debug("=== PAYMENT VALIDATION ERROR ===");
            log.error("Payment validation error for schedule {}", scheduleId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.debug("=== PAYMENT SYSTEM ERROR ===");
            log.error("Payment system error for schedule {}", scheduleId, e);
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
