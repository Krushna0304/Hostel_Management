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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
    @PostMapping("/pay/{scheduleId}")
    public ResponseEntity<?> recordPayment(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody RecordPaymentRequest request) {
        try {
            System.out.println("=== TENANT PAYMENT REQUEST START ===");
            System.out.println("Schedule ID: " + scheduleId);
            System.out.println("Request Amount: " + request.getAmount());
            System.out.println("Payment Mode: " + request.getPaymentMode());
            System.out.println("OTP provided: " + (request.getOtp() != null ? "Yes (length: " + request.getOtp().length() + ")" : "No"));
            System.out.println("Razorpay Order ID: " + request.getRazorpayOrderId());
            
            User tenant = ApplicationContext.getUser();
            System.out.println("Tenant: " + tenant.getUsername() + " (ID: " + tenant.getUserId() + ")");

            // Resolve the owner from the tenant's allotment
            RoomAllotment allotment = roomAllotmentRepository
                    .findByTenant_UserIdAndRoomAllotmentStatus(
                            tenant.getUserId(),
                            com.krunity.HostelManagment.enums.RoomAllotmentStatus.CONFIRMED)
                    .orElseThrow(() -> new RuntimeException("No active allotment found"));

            User owner = allotment.getRoom().getHostel().getOwner();
            System.out.println("Owner: " + owner.getUsername() + " (ID: " + owner.getUserId() + ")");

            System.out.println("Calling PaymentScheduleService.recordPayment...");
            InstallmentResponse response = paymentScheduleService.recordPayment(scheduleId, request, tenant, owner);
            System.out.println("Payment recorded successfully!");
            System.out.println("=== TENANT PAYMENT REQUEST END ===");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println("=== PAYMENT VALIDATION ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.out.println("=== PAYMENT SYSTEM ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
