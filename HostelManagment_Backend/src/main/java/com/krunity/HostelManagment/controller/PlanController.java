package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.dto.*;
import com.krunity.HostelManagment.service.EnhancedBillingCalculationService;
import com.krunity.HostelManagment.service.InstallmentDistributionService;
import com.krunity.HostelManagment.service.PaymentCalculationService;
import com.krunity.HostelManagment.service.RoomAgreementPlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    @Autowired
    private RoomAgreementPlanService planService;
    
    @Autowired
    private PaymentCalculationService paymentCalculationService;
    
    @Autowired
    private EnhancedBillingCalculationService enhancedBillingCalculationService;
    
    @Autowired
    private InstallmentDistributionService installmentDistributionService;

    /** GET /api/plans/active — returns global + owner's own plans, optionally filtered by planType */
    @GetMapping("/active")
    public ResponseEntity<?> getActivePlans(
            @RequestParam(required = false) String planType) {
        try {
            return ResponseEntity.ok(planService.getActivePlans(planType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** GET /api/plans/my — returns only plans created by the logged-in owner */
    @GetMapping("/my")
    public ResponseEntity<List<PlanResponse>> getMyPlans() {
        return ResponseEntity.ok(planService.getMyPlans());
    }

    /** GET /api/plans/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getPlanById(@PathVariable String id) {
        return ResponseEntity.ok(planService.getPlanResponseById(id));
    }

    /** POST /api/plans — create a new owner-specific plan */
    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(@RequestBody CreatePlanRequest request) {
        return ResponseEntity.status(201).body(planService.createPlan(request));
    }

    /** PUT /api/plans/{id} — update owner's plan (only if not in use) */
    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> updatePlan(@PathVariable String id, @Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity.ok(planService.updatePlan(id, request));
    }

    /** DELETE /api/plans/{id} — soft-delete (deactivate) owner's plan */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable String id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }

    /** POST /api/plans/{id}/activate — activate owner's plan */
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activatePlan(@PathVariable String id) {
        planService.activatePlan(id);
        return ResponseEntity.ok().build();
    }

    /** POST /api/plans/{id}/deactivate — deactivate owner's plan */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivatePlan(@PathVariable String id) {
        planService.deactivatePlan(id);
        return ResponseEntity.ok().build();
    }
    
    /** GET /api/plans/{id}/payment-breakdown — get payment breakdown for a plan */
    @GetMapping("/{id}/payment-breakdown")
    public ResponseEntity<PaymentBreakdownResponse> getPaymentBreakdown(@PathVariable String id) {
        var plan = planService.getPlanById(id);
        var breakdown = paymentCalculationService.calculatePaymentBreakdown(plan);
        
        PaymentBreakdownResponse response = new PaymentBreakdownResponse();
        response.setAgreementTimeRefundable(breakdown.getAgreementTimeRefundable());
        response.setAgreementTimeNonRefundable(breakdown.getAgreementTimeNonRefundable());
        response.setInstallmentAmount(breakdown.getInstallmentAmount());
        response.setEndTimeRefundable(breakdown.getEndTimeRefundable());
        response.setEndTimeNonRefundable(breakdown.getEndTimeNonRefundable());
        response.setTotalAgreementTime(breakdown.getTotalAgreementTime());
        response.setTotalEndTime(breakdown.getTotalEndTime());
        
        return ResponseEntity.ok(response);
    }
    
    /** GET /api/plans/{id}/enhanced-breakdown — get enhanced billing breakdown for a plan */
    @GetMapping("/{id}/enhanced-breakdown")
    public ResponseEntity<EnhancedBillingBreakdown> getEnhancedBreakdown(@PathVariable String id) {
        var plan = planService.getPlanById(id);
        var breakdown = enhancedBillingCalculationService.calculateComprehensiveBilling(plan);
        return ResponseEntity.ok(breakdown);
    }
    
    /** POST /api/plans/calculate-installments — calculate installment distribution */
    @PostMapping("/calculate-installments")
    public ResponseEntity<InstallmentDistribution> calculateInstallments(
            @Valid @RequestBody InstallmentCalculationRequest request
    ) {
        InstallmentDistribution distribution;
        
        if ("EQUAL".equals(request.getDistributionStrategy())) {
            distribution = installmentDistributionService.calculateEqualDistribution(
                request.getTotalMonths(), 
                request.getNumberOfInstallments()
            );
        } else {
            distribution = installmentDistributionService.calculateDistribution(
                request.getTotalMonths(), 
                request.getNumberOfInstallments()
            );
        }
        
        return ResponseEntity.ok(distribution);
    }
}


