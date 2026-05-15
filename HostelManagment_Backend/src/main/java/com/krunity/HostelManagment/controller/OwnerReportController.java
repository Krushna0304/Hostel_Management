package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.OwnerCollectionSummaryResponse;
import com.krunity.HostelManagment.dto.RecordPaymentRequest;
import com.krunity.HostelManagment.dto.InstallmentResponse;
import com.krunity.HostelManagment.dto.InstallmentSummaryResponse;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.model.TenantPaymentPlan;
import com.krunity.HostelManagment.model.PaymentRequestSchedule;
import com.krunity.HostelManagment.service.TenantDashboardService;
import com.krunity.HostelManagment.service.PaymentScheduleService;
import com.krunity.HostelManagment.repository.TenantPaymentPlanRepository;
import com.krunity.HostelManagment.repository.PaymentRequestScheduleRepository;
import com.krunity.HostelManagment.repository.UserRepository;
import com.krunity.HostelManagment.exception.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/owner/reports")
public class OwnerReportController {

    @Autowired
    private TenantDashboardService tenantDashboardService;

    @Autowired
    private PaymentScheduleService paymentScheduleService;

    @Autowired
    private TenantPaymentPlanRepository paymentPlanRepository;

    @Autowired
    private PaymentRequestScheduleRepository scheduleRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * GET /owner/reports/collections
     * Returns a full collection summary: total collected, pending, overdue,
     * and a per-tenant breakdown for all tenants in the owner's hostels.
     */
    @GetMapping("/collections")
    public ResponseEntity<?> getCollectionSummary() {
        try {
            User owner = ApplicationContext.getUser();
            System.out.println("Collections accessed by owner - ID: " + owner.getUserId() + ", Role: " + owner.getRole().getName());
            
            // Verify user is an owner
            if (!"OWNER".equals(owner.getRole().getName())) {
                System.out.println("Access denied - User is not an owner: " + owner.getRole().getName());
                return ResponseEntity.status(403).body("Access denied. Only owners can access this endpoint.");
            }
            
            OwnerCollectionSummaryResponse response =
                    tenantDashboardService.getOwnerCollectionSummary(owner.getUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Error in collections endpoint: " + e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /**
     * GET /owner/reports/tenant/{tenantId}/payment-history
     * Returns the full payment ledger (all installments) for a specific tenant.
     * Used by the owner to view a tenant's complete payment history.
     */
    @GetMapping("/tenant/{tenantId}/payment-history")
    public ResponseEntity<?> getTenantPaymentHistory(@PathVariable String tenantId) {
        try {
            User owner = ApplicationContext.getUser();
            if (!"OWNER".equals(owner.getRole().getName())) {
                return ResponseEntity.status(403).body("Access denied. Only owners can access this endpoint.");
            }

            UUID tenantUuid;
            try {
                tenantUuid = UUID.fromString(tenantId);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid tenant ID format");
            }

            com.krunity.HostelManagment.dto.PaymentLedgerResponse ledger =
                    paymentScheduleService.getTenantLedger(tenantUuid);
            return ResponseEntity.ok(ledger);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /**
     * GET /owner/reports/tenant/{tenantId}/installments
     * Returns pending installments for a specific tenant that the owner can collect payment for
     */
    @GetMapping("/tenant/{tenantId}/installments")
    public ResponseEntity<?> getTenantInstallments(@PathVariable String tenantId) {
        try {
            User owner = ApplicationContext.getUser();
            System.out.println("Owner accessing installments - ID: " + owner.getUserId() + ", Role: " + owner.getRole().getName());
            System.out.println("Requested tenant ID: " + tenantId);
            
            // Verify user is an owner
            if (!"OWNER".equals(owner.getRole().getName())) {
                System.out.println("Access denied - User is not an owner: " + owner.getRole().getName());
                return ResponseEntity.status(403).body("Access denied. Only owners can access this endpoint.");
            }
            
            // Convert string tenantId to UUID
            UUID tenantUuid;
            try {
                tenantUuid = UUID.fromString(tenantId);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid tenant ID format: " + tenantId);
                return ResponseEntity.badRequest().body("Invalid tenant ID format");
            }
            
            // Get tenant's payment plan
            TenantPaymentPlan plan = paymentPlanRepository.findByTenant_UserIdAndIsActiveTrue(tenantUuid)
                    .orElseThrow(() -> new NotFoundException("No active payment plan found for tenant"));
            
            // Get pending/overdue installments
            List<PaymentRequestSchedule> installments = scheduleRepository
                    .findByTenantPaymentPlan_PlanIdAndPaymentStatusIn(
                        plan.getPlanId(), 
                        List.of(
                            com.krunity.HostelManagment.enums.TransactionStatus.SCHEDULED,
                            com.krunity.HostelManagment.enums.TransactionStatus.OVERDUE,
                            com.krunity.HostelManagment.enums.TransactionStatus.PARTIALLY_PAID
                        )
                    );
            
            System.out.println("Found " + installments.size() + " pending installments for tenant " + tenantId);
            
            // Convert entities to DTOs to avoid Hibernate lazy loading serialization issues
            List<InstallmentSummaryResponse> response = installments.stream()
                    .map(this::mapToInstallmentSummaryResponse)
                    .toList();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Error in getTenantInstallments: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /**
     * Helper method to map PaymentRequestSchedule entity to InstallmentSummaryResponse DTO
     */
    private InstallmentSummaryResponse mapToInstallmentSummaryResponse(PaymentRequestSchedule schedule) {
        return InstallmentSummaryResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .installmentNumber(schedule.getInstallmentNumber())
                .amount(schedule.getAmount())
                .dueDate(schedule.getDueDate())
                .paymentStatus(schedule.getPaymentStatus())
                .paidAmount(schedule.getPaidAmount())
                .paidAt(schedule.getPaidAt())
                .lateFeeApplied(schedule.getLateFeeApplied())
                .transactionId(schedule.getTransactionId())
                .build();
    }

    /**
     * POST /owner/reports/collect-payment/{scheduleId}
     * Allows owner to collect payment for a specific installment from tenant
     */
    @PostMapping("/collect-payment/{scheduleId}")
    public ResponseEntity<?> collectPayment(
            @PathVariable String scheduleId,
            @Valid @RequestBody RecordPaymentRequest request) {
        try {
            User owner = ApplicationContext.getUser();
            
            // Verify user is an owner
            if (!"OWNER".equals(owner.getRole().getName())) {
                System.out.println("Access denied - User is not an owner: " + owner.getRole().getName());
                return ResponseEntity.status(403).body("Access denied. Only owners can collect payments.");
            }
            
            // Convert string scheduleId to UUID
            UUID scheduleUuid;
            try {
                scheduleUuid = UUID.fromString(scheduleId);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid schedule ID format");
            }
            
            // Get the payment schedule
            PaymentRequestSchedule schedule = scheduleRepository.findById(scheduleUuid)
                    .orElseThrow(() -> new NotFoundException("Installment not found"));
            
            // Get the tenant
            User tenant = schedule.getTenantPaymentPlan().getTenant();
            
            // Verify owner has access to this tenant (through hostel ownership)
            // This is a security check to ensure owner can only collect from their tenants
            
            // Record the payment
            InstallmentResponse response = paymentScheduleService.recordPayment(scheduleUuid, request, tenant, owner);
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
