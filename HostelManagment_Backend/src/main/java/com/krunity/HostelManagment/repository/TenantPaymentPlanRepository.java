package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.TenantPaymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantPaymentPlanRepository extends JpaRepository<TenantPaymentPlan, UUID> {

    Optional<TenantPaymentPlan> findByAgreementId(String agreementId);

    List<TenantPaymentPlan> findByTenant_UserId(UUID tenantId);

    Optional<TenantPaymentPlan> findByTenant_UserIdAndIsActiveTrue(UUID tenantId);
}

