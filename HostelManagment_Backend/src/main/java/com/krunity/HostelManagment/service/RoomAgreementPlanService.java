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
     * - Global/system plans (ownerId is null)
     * - Plans created by this owner
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
            // No filter — return all active plans (existing behaviour)
            plans.addAll(planRepository.findByOwnerIdIsNullAndStatus(PlanStatus.ACTIVE));
            if (owner != null) {
                plans.addAll(planRepository.findByOwnerIdAndStatus(owner.getUserId(), PlanStatus.ACTIVE));
            }
        } else if (resolvedPlanType == PlanType.PG_ROOM) {
            // PG_ROOM: include plans where planType = 'PG_ROOM' OR planType is absent/null
            plans.addAll(planRepository.findGlobalActiveByPlanTypePgRoom(PlanStatus.ACTIVE));
            if (owner != null) {
                plans.addAll(planRepository.findByOwnerIdAndStatusAndPlanTypePgRoom(owner.getUserId(), PlanStatus.ACTIVE));
            }
        } else {
            // FLAT: include only plans where planType = 'FLAT'
            plans.addAll(planRepository.findGlobalActiveByPlanTypeFlat(PlanStatus.ACTIVE));
            if (owner != null) {
                plans.addAll(planRepository.findByOwnerIdAndStatusAndPlanTypeFlat(owner.getUserId(), PlanStatus.ACTIVE));
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
     * Creates a new plan owned by the logged-in owner.
     */
    public PlanResponse createPlan(CreatePlanRequest request) {
        User owner = ApplicationContext.getUser();

        RoomAgreementPlan plan = RoomAgreementPlan.builder()
                .planName(request.getPlanName())
                .planType(request.getPlanType().name())
                .status(PlanStatus.ACTIVE)
                .ownerId(owner.getUserId())
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
     */
    public void deletePlan(String planId) {
        User owner = ApplicationContext.getUser();

        RoomAgreementPlan plan = planRepository.findByIdAndOwnerId(planId, owner.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Plan not found or you don't have permission to delete it"));

        plan.setStatus(PlanStatus.INACTIVE);
        planRepository.save(plan);
    }
}


