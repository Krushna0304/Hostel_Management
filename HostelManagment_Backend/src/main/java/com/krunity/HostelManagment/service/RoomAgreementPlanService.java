package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.Mapper.RoomAgreementPlanMapper;
import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.CreatePlanRequest;
import com.krunity.HostelManagment.dto.PlanResponse;
import com.krunity.HostelManagment.enums.PlanStatus;
import com.krunity.HostelManagment.enums.PlanType;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.exception.UnauthorizedException;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.model.plan.Charges;
import com.krunity.HostelManagment.model.plan.PlanAudit;
import com.krunity.HostelManagment.repository.RoomAgreementPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomAgreementPlanService {

    @Autowired
    private RoomAgreementPlanRepository planRepository;

    @Autowired
    private PaymentCalculationService paymentCalculationService;

    /**
     * Returns plans visible to the logged-in owner:
     * - Global/system plans (ownerId is null) where isActive = true
     * - Plans created by this owner where isActive = true
     *
     * @param planType optional filter — "PG_ROOM", "FLAT", or null (returns all)
     *                 "PG_ROOM" includes plans whose planType is PG_ROOM or absent/null (backward compat)
     *                 "FLAT"    includes only plans whose planType is FLAT
     * @throws IllegalArgumentException if planType is not a valid PlanType value
     */
    public List<PlanResponse> getActivePlans(String planType) {
        User owner = ApplicationContext.getUser();

        // Validate planType if provided
        PlanType resolvedPlanType = null;
        if (planType != null && !planType.isBlank()) {
            try {
                resolvedPlanType = PlanType.valueOf(planType.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid planType filter value. Allowed: PG_ROOM, FLAT");
            }
        }

        List<RoomAgreementPlan> plans = new ArrayList<>();

        if (resolvedPlanType == null) {
            // No filter — return all active plans that are also isActive = true
            plans.addAll(planRepository.findByOwnerIdIsNullAndStatusAndIsActive(PlanStatus.ACTIVE, true));
            if (owner != null) {
                plans.addAll(planRepository.findByOwnerIdAndStatusAndIsActive(owner.getUserId(), PlanStatus.ACTIVE, true));
            }
        } else if (resolvedPlanType == PlanType.PG_ROOM) {
            // PG_ROOM: include plans where planType = 'PG_ROOM' OR planType is absent/null AND isActive = true
            plans.addAll(planRepository.findGlobalActiveByPlanTypePgRoomAndIsActive(PlanStatus.ACTIVE, true));
            if (owner != null) {
                plans.addAll(planRepository.findByOwnerIdAndStatusAndPlanTypePgRoomAndIsActive(owner.getUserId(), PlanStatus.ACTIVE, true));
            }
        } else {
            // FLAT: include only plans where planType = 'FLAT' AND isActive = true
            plans.addAll(planRepository.findGlobalActiveByPlanTypeFlatAndIsActive(PlanStatus.ACTIVE, true));
            if (owner != null) {
                plans.addAll(planRepository.findByOwnerIdAndStatusAndPlanTypeFlatAndIsActive(owner.getUserId(), PlanStatus.ACTIVE, true));
            }
        }

        return plans.stream()
                .map(RoomAgreementPlanMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Backward-compatible overload — returns all active plans with no filter.
     */
    public List<PlanResponse> getActivePlans() {
        return getActivePlans(null);
    }

    /**
     * Returns only plans created by the logged-in owner (for the Plans management page).
     * Includes both active and inactive plans for management purposes.
     */
    public List<PlanResponse> getMyPlans() {
        User owner = ApplicationContext.getUser();
        return planRepository.findByOwnerIdAndStatus(owner.getUserId(), PlanStatus.ACTIVE)
                .stream()
                .map(RoomAgreementPlanMapper::toDto)
                .collect(Collectors.toList());
    }

    public PlanResponse getPlanResponseById(String planId) {
        return RoomAgreementPlanMapper.toDto(getPlanById(planId));
    }

    public RoomAgreementPlan getPlanById(String planId) {
        return planRepository.findById(planId)
                .filter(p -> p.getStatus() == PlanStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Plan not found with ID: " + planId));
    }

    /**
     * Mark a plan as in use (inUseFlag = 1) when it's used in an agreement
     */
    public void markPlanAsInUse(String planId) {
        RoomAgreementPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Plan not found with ID: " + planId));
        
        plan.setInUseFlag(1);
        planRepository.save(plan);
    }

    /**
     * Update a plan owned by the logged-in owner.
     * Only allows updates if the plan is not in use (inUseFlag = 0).
     */
    public PlanResponse updatePlan(String planId, CreatePlanRequest request) {
        User owner = ApplicationContext.getUser();

        RoomAgreementPlan plan = planRepository.findByIdAndOwnerId(planId, owner.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Plan not found or you don't have permission to edit it"));

        // Check if plan is in use
        if (plan.getInUseFlag() != null && plan.getInUseFlag() == 1) {
            throw new IllegalStateException("Cannot edit plan that is currently in use by agreements");
        }

        // Update plan fields
        plan.setPlanName(request.getPlanName());
        plan.setPlanType(request.getPlanType().name());
        plan.setRentDetails(request.getRentDetails());
        plan.setDuration(request.getDuration());
        plan.setPaymentModel(request.getPaymentModel());
        plan.setCharges(request.getCharges());
        plan.setFreeFacilities(request.getFreeFacilities());
        plan.setLatePaymentPolicy(request.getLatePaymentPolicy());
        plan.setRulesAndRegulations(request.getRulesAndRegulations());
        plan.setRestrictions(request.getRestrictions());
        plan.setAgreementCancellationRules(request.getAgreementCancellationRules());
        plan.setLegal(request.getLegal());
        plan.setCustomFields(request.getCustomFields());
        
        // Update audit info
        if (plan.getAudit() != null) {
            plan.getAudit().setUpdatedAt(Instant.now());
        } else {
            plan.setAudit(PlanAudit.builder()
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());
        }

        RoomAgreementPlan updatedPlan = planRepository.save(plan);
        return RoomAgreementPlanMapper.toDto(updatedPlan);
    }

    /**
     * Creates a new plan owned by the logged-in owner.
     */
    public PlanResponse createPlan(CreatePlanRequest request) {
        User owner = ApplicationContext.getUser();

        RoomAgreementPlan plan = RoomAgreementPlan.builder()
                .planName(request.getPlanName())
                .planType(request.getPlanType().name())
                .status(PlanStatus.ACTIVE)
                .ownerId(owner.getUserId())
                .inUseFlag(0) // New plan, not in use yet
                .isActive(true) // New plan is active by default
                .rentDetails(request.getRentDetails())
                .duration(request.getDuration())
                .paymentModel(request.getPaymentModel())
                .charges(request.getCharges())
                .freeFacilities(request.getFreeFacilities())
                .latePaymentPolicy(request.getLatePaymentPolicy())
                .rulesAndRegulations(request.getRulesAndRegulations())
                .restrictions(request.getRestrictions())
                .agreementCancellationRules(request.getAgreementCancellationRules())
                .legal(request.getLegal())
                .customFields(request.getCustomFields())
                .audit(PlanAudit.builder()
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
                .build();

        // Calculate and populate payment plan
        plan = calculateAndPopulatePaymentPlan(plan);

        RoomAgreementPlan saved = planRepository.save(plan);
        return RoomAgreementPlanMapper.toDto(saved);
    }

    /**
     * Calculate and populate the payment plan structure in the charges
     */
    private RoomAgreementPlan calculateAndPopulatePaymentPlan(RoomAgreementPlan plan) {
        if (plan.getCharges() == null) {
            plan.setCharges(new Charges());
        }

        // Calculate payment breakdown using existing service
        PaymentCalculationService.PaymentBreakdown breakdown = 
            paymentCalculationService.calculatePaymentBreakdown(plan);

        // Calculate installment details
        int totalMonths = 12; // Default
        int numberOfInstallments = 12; // Default
        
        if (plan.getDuration() != null && plan.getDuration().getValue() != null) {
            totalMonths = plan.getDuration().getValue();
        }
        
        if (plan.getPaymentModel() != null && plan.getPaymentModel().getInstallments() != null) {
            numberOfInstallments = plan.getPaymentModel().getInstallments();
        }
        
        int monthsPerInstallment = Math.max(1, totalMonths / numberOfInstallments);
        BigDecimal installmentAmount = breakdown.getInstallmentAmount().multiply(BigDecimal.valueOf(monthsPerInstallment));

        // Create payment plan structure
        Charges.PaymentPlan.AgreementActivationAmount activationAmount = 
            Charges.PaymentPlan.AgreementActivationAmount.builder()
                .firstInstallment(installmentAmount)
                .refundableDeposits(breakdown.getAgreementTimeRefundable())
                .oneTimeCharges(breakdown.getAgreementTimeNonRefundable())
                .totalActivationAmount(installmentAmount.add(breakdown.getAgreementTimeRefundable()).add(breakdown.getAgreementTimeNonRefundable()))
                .build();

        Charges.PaymentPlan.InstallmentDetails installmentDetails = 
            Charges.PaymentPlan.InstallmentDetails.builder()
                .installmentAmount(installmentAmount)
                .numberOfInstallments(numberOfInstallments)
                .monthsPerInstallment(monthsPerInstallment)
                .totalMonthlyAmount(breakdown.getInstallmentAmount())
                .paymentFrequency(plan.getPaymentModel() != null ? plan.getPaymentModel().getMode() : "MONTHLY")
                .build();

        Charges.PaymentPlan.ExitSettlement exitSettlement = 
            Charges.PaymentPlan.ExitSettlement.builder()
                .estimatedRefund(breakdown.getAgreementTimeRefundable())
                .potentialDeductions(breakdown.getEndTimeRefundable())
                .netRefundEstimate(breakdown.getAgreementTimeRefundable().subtract(breakdown.getEndTimeRefundable()))
                .build();

        Charges.PaymentPlan paymentPlan = Charges.PaymentPlan.builder()
            .agreementActivationAmount(activationAmount)
            .installmentDetails(installmentDetails)
            .exitSettlement(exitSettlement)
            .build();

        // Update charges with payment plan
        plan.getCharges().setPaymentPlan(paymentPlan);

        return plan;
    }

    /**
     * Soft-deletes (deactivates) a plan owned by the logged-in owner.
     * Sets isActive = false instead of changing status.
     * Only allows deletion if the plan is not in use (inUseFlag = 0).
     */
    public void deletePlan(String planId) {
        User owner = ApplicationContext.getUser();

        RoomAgreementPlan plan = planRepository.findByIdAndOwnerId(planId, owner.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Plan not found or you don't have permission to delete it"));

        // Check if plan is in use
        if (plan.getInUseFlag() != null && plan.getInUseFlag() == 1) {
            throw new IllegalStateException("Cannot delete plan that is currently in use by agreements");
        }

        // Set isActive to false instead of changing status
        plan.setIsActive(false);
        planRepository.save(plan);
    }

    /**
     * Activates a plan owned by the logged-in owner.
     * Sets isActive = true.
     */
    public void activatePlan(String planId) {
        User owner = ApplicationContext.getUser();

        RoomAgreementPlan plan = planRepository.findByIdAndOwnerId(planId, owner.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Plan not found or you don't have permission to activate it"));

        plan.setIsActive(true);
        planRepository.save(plan);
    }

    /**
     * Deactivates a plan owned by the logged-in owner.
     * Sets isActive = false.
     * Only allows deactivation if the plan is not in use (inUseFlag = 0).
     */
    public void deactivatePlan(String planId) {
        User owner = ApplicationContext.getUser();

        RoomAgreementPlan plan = planRepository.findByIdAndOwnerId(planId, owner.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Plan not found or you don't have permission to deactivate it"));

        // Check if plan is in use
        if (plan.getInUseFlag() != null && plan.getInUseFlag() == 1) {
            throw new IllegalStateException("Cannot deactivate plan that is currently in use by agreements");
        }

        plan.setIsActive(false);
        planRepository.save(plan);
    }
}


