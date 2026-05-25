package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.model.TenantPaymentPlan;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import com.krunity.HostelManagment.repository.TenantPaymentPlanRepository;
import com.krunity.HostelManagment.repository.AgreementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class AgreementMigrationService {

    @Autowired
    private TenantPaymentPlanRepository paymentPlanRepository;

    @Autowired
    private AgreementRepository agreementRepository;

    /**
     * Fixes agreement end dates for existing agreements to be based on plan duration
     * instead of last installment date.
     */
    @Transactional
    public void fixAgreementEndDates() {
        List<TenantPaymentPlan> allPlans = paymentPlanRepository.findAll();
        
        int fixedCount = 0;
        for (TenantPaymentPlan plan : allPlans) {
            if (plan.getAgreementId() != null) {
                try {
                    Agreement agreement = agreementRepository.findById(plan.getAgreementId()).orElse(null);
                    if (agreement != null && agreement.getPlanSnapshot() != null) {
                        RoomAgreementPlan planSnapshot = agreement.getPlanSnapshot();
                        
                        if (planSnapshot.getDuration() != null && plan.getStartDate() != null) {
                            // Calculate correct end date based on plan duration
                            int durationValue = planSnapshot.getDuration().getValue();
                            String durationUnit = planSnapshot.getDuration().getUnit();
                            
                            LocalDate correctEndDate;
                            if ("YEAR".equalsIgnoreCase(durationUnit)) {
                                correctEndDate = plan.getStartDate().plusYears(durationValue);
                            } else {
                                // Default to months for "MONTH" or any other unit
                                correctEndDate = plan.getStartDate().plusMonths(durationValue);
                            }
                            
                            // Update if different from current end date
                            if (!correctEndDate.equals(plan.getEndDate())) {
                                LocalDate oldEndDate = plan.getEndDate();
                                plan.setEndDate(correctEndDate);
                                paymentPlanRepository.save(plan);
                                fixedCount++;
                                
                                log.info("Fixed agreement {}: End date changed from {} to {} (Duration: {} {})",
                                    agreement.getId(), oldEndDate, correctEndDate, durationValue, durationUnit);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to fix agreement {}: {}", plan.getAgreementId(), e.getMessage());
                }
            }
        }
        
        log.info("Agreement end date migration completed. Fixed {} agreements.", fixedCount);
    }

    /**
     * Fixes a specific agreement by its ID
     */
    @Transactional
    public boolean fixSpecificAgreement(String agreementId) {
        try {
            Agreement agreement = agreementRepository.findById(agreementId).orElse(null);
            if (agreement == null) {
                log.error("Agreement not found: {}", agreementId);
                return false;
            }

            TenantPaymentPlan plan = paymentPlanRepository.findByAgreementId(agreementId).orElse(null);
            if (plan == null) {
                log.error("Payment plan not found for agreement: {}", agreementId);
                return false;
            }

            RoomAgreementPlan planSnapshot = agreement.getPlanSnapshot();
            if (planSnapshot == null || planSnapshot.getDuration() == null) {
                log.error("No plan snapshot or duration found for agreement: {}", agreementId);
                return false;
            }

            // Calculate correct end date
            int durationValue = planSnapshot.getDuration().getValue();
            String durationUnit = planSnapshot.getDuration().getUnit();
            
            LocalDate correctEndDate;
            if ("YEAR".equalsIgnoreCase(durationUnit)) {
                correctEndDate = plan.getStartDate().plusYears(durationValue);
            } else {
                correctEndDate = plan.getStartDate().plusMonths(durationValue);
            }

            LocalDate oldEndDate = plan.getEndDate();
            plan.setEndDate(correctEndDate);
            paymentPlanRepository.save(plan);

            log.info("Fixed agreement {}: End date changed from {} to {} (Duration: {} {})",
                agreementId, oldEndDate, correctEndDate, durationValue, durationUnit);
            
            return true;
        } catch (Exception e) {
            log.error("Failed to fix agreement {}: {}", agreementId, e.getMessage());
            return false;
        }
    }
}