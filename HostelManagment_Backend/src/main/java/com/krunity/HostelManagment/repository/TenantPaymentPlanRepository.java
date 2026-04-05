package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.TenantPaymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantPaymentPlanRepository extends JpaRepository<TenantPaymentPlan, UUID> {
}

