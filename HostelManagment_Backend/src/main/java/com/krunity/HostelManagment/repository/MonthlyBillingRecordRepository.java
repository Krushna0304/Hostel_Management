package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.BillingStatus;
import com.krunity.HostelManagment.model.MonthlyBillingRecord;
import com.krunity.HostelManagment.model.TenantPaymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MonthlyBillingRecordRepository extends JpaRepository<MonthlyBillingRecord, UUID> {

    List<MonthlyBillingRecord> findByPaymentPlan(TenantPaymentPlan paymentPlan);

    List<MonthlyBillingRecord> findByPaymentPlan_PlanId(UUID planId);

    List<MonthlyBillingRecord> findByPaymentPlanAndStatus(TenantPaymentPlan paymentPlan, BillingStatus status);

    List<MonthlyBillingRecord> findByAssignedInstallmentId(UUID installmentId);

    List<MonthlyBillingRecord> findByBillingMonthBetween(LocalDate startDate, LocalDate endDate);

    List<MonthlyBillingRecord> findByPaymentPlanAndMonthNumberBetween(
            TenantPaymentPlan paymentPlan, 
            Integer startMonth, 
            Integer endMonth
    );
}