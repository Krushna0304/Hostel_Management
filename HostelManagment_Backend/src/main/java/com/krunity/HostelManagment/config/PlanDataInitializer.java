package com.krunity.HostelManagment.config;

import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import com.krunity.HostelManagment.model.plan.*;
import com.krunity.HostelManagment.repository.RoomAgreementPlanRepository;
import com.krunity.HostelManagment.enums.PlanStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

@Component
public class PlanDataInitializer implements CommandLineRunner {
    
    @Autowired
    private RoomAgreementPlanRepository planRepository;
    
    @Override
    public void run(String... args) {
        // Check if default plan already exists
        if (planRepository.findById("PLAN_STANDARD_MONTHLY_V2").isPresent()) {
            return; // Plan already exists, skip initialization
        }
        
        // Create the default Standard Monthly Room Plan
        RoomAgreementPlan defaultPlan = createDefaultPlan();
        planRepository.save(defaultPlan);
    }
    
    private RoomAgreementPlan createDefaultPlan() {
        // Create nested structures
        Duration duration = Duration.builder()
                .unit("MONTH")
                .value(12)
                .minimumStayMonths(3)
                .build();
        
        PaymentModel paymentModel = PaymentModel.builder()
                .mode("MONTHLY")
                .paymentTiming("PREPAID")
                .installments(12)
                .dueDayOfMonth(5)
                .build();
        
        RentDetails rentDetails = RentDetails.builder()
                .monthlyRent(new BigDecimal("8500"))
                .currency("INR")
                .build();
        
        SecurityDeposit securityDeposit = SecurityDeposit.builder()
                .amount(new BigDecimal("17000"))
                .refundable(true)
                .refundProcessingDays(15)
                .build();
        
        DeepCleaningOnExit deepCleaningOnExit = DeepCleaningOnExit.builder()
                .applicable(true)
                .amount(new BigDecimal("1500"))
                .build();
        
        CleaningCharges cleaningCharges = CleaningCharges.builder()
                .included(true)
                .cleaningFrequency("WEEKLY")
                .deepCleaningOnExit(deepCleaningOnExit)
                .build();
        
        MaintenanceCharges maintenanceCharges = MaintenanceCharges.builder()
                .included(true)
                .covers(Arrays.asList(
                        "Electrical fittings",
                        "Plumbing",
                        "Furniture wear and tear"
                ))
                .build();
        
        ElectricityCharges electricityCharges = ElectricityCharges.builder()
                .type("METERED")
                .ratePerUnit(new BigDecimal("10"))
                .build();
        
        WaterCharges waterCharges = WaterCharges.builder()
                .type("FIXED")
                .monthlyAmount(new BigDecimal("300"))
                .build();
        
        UtilityCharges utilityCharges = UtilityCharges.builder()
                .electricity(electricityCharges)
                .water(waterCharges)
                .build();
        
        Charges charges = Charges.builder()
                .securityDeposit(securityDeposit)
                .cleaningCharges(cleaningCharges)
                .maintenanceCharges(maintenanceCharges)
                .utilityCharges(utilityCharges)
                .build();
        
        // Create free facilities
        FreeFacilities freeFacilities = FreeFacilities.builder()
                .included(true)
                .facilities(Arrays.asList(
                        Facility.builder().name("Wi-Fi").description("Unlimited shared broadband internet").availability("24x7").build(),
                        Facility.builder().name("Bed & Mattress").description("Single bed with standard mattress").build(),
                        Facility.builder().name("Cupboard").description("Personal lockable cupboard").build(),
                        Facility.builder().name("Study Table").description("Dedicated study table with chair").build(),
                        Facility.builder().name("Fan & Lighting").description("Ceiling fan and LED lights").build(),
                        Facility.builder().name("RO Drinking Water").description("Filtered drinking water").build(),
                        Facility.builder().name("Common Refrigerator").description("Shared refrigerator in common area").build(),
                        Facility.builder().name("Washing Machine").description("Shared washing machine (usage rules apply)").build(),
                        Facility.builder().name("Parking").description("Two-wheeler parking (subject to availability)").build(),
                        Facility.builder().name("Power Backup").description("Limited power backup for lights & fans").build()
                ))
                .build();
        
        LatePaymentPenalty latePaymentPenalty = LatePaymentPenalty.builder()
                .type("PER_DAY")
                .amount(new BigDecimal("100"))
                .maxAmount(new BigDecimal("2500"))
                .build();
        
        LatePaymentPolicy latePaymentPolicy = LatePaymentPolicy.builder()
                .gracePeriodDays(5)
                .penalty(latePaymentPenalty)
                .build();
        
        QuietHours quietHours = QuietHours.builder()
                .from("22:00")
                .to("06:00")
                .build();
        
        HouseRules houseRules = HouseRules.builder()
                .smokingAllowed(false)
                .petsAllowed(false)
                .quietHours(quietHours)
                .build();
        
        RulesAndRegulations rulesAndRegulations = RulesAndRegulations.builder()
                .houseRules(houseRules)
                .facilityUsageRules(Arrays.asList(
                        "Washing machine usage as per allotted time",
                        "Parking is first-come-first-serve",
                        "Internet misuse may lead to restriction",
                        "Damage to facility will be charged"
                ))
                .build();
        
        Restrictions restrictions = Restrictions.builder()
                .commercialUsageAllowed(false)
                .illegalActivitiesProhibited(true)
                .build();
        
        EarlyExitPenalty earlyExitPenalty = EarlyExitPenalty.builder()
                .type("MONTH_RENT")
                .value(1)
                .build();
        
        TenantCancellation tenantCancellation = TenantCancellation.builder()
                .allowed(true)
                .noticePeriodDays(30)
                .earlyExitPenalty(earlyExitPenalty)
                .build();
        
        AgreementCancellationRules agreementCancellationRules = AgreementCancellationRules.builder()
                .tenantCancellation(tenantCancellation)
                .build();
        
        Legal legal = Legal.builder()
                .agreementLock(true)
                .modificationAllowedAfterSign(false)
                .jurisdiction("Nagpur, Maharashtra")
                .build();
        
        PlanAudit audit = PlanAudit.builder()
                .createdAt(Instant.now())
                .build();
        
        return RoomAgreementPlan.builder()
                .id("PLAN_STANDARD_MONTHLY_V2")
                .planType("ROOM_AGREEMENT")
                .planName("Standard Monthly Room Plan")
                .status(PlanStatus.ACTIVE)
                .duration(duration)
                .paymentModel(paymentModel)
                .rentDetails(rentDetails)
                .charges(charges)
                .freeFacilities(freeFacilities)
                .latePaymentPolicy(latePaymentPolicy)
                .rulesAndRegulations(rulesAndRegulations)
                .restrictions(restrictions)
                .agreementCancellationRules(agreementCancellationRules)
                .legal(legal)
                .audit(audit)
                .build();
    }
}

