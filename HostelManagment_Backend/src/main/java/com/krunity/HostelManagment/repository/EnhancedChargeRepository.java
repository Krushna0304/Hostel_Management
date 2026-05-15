package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.ChargeCategory;
import com.krunity.HostelManagment.model.EnhancedChargeEntity;
import com.krunity.HostelManagment.model.plan.PaymentTiming;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EnhancedChargeRepository extends JpaRepository<EnhancedChargeEntity, UUID> {

    List<EnhancedChargeEntity> findByPlanId(String planId);

    List<EnhancedChargeEntity> findByPlanIdAndCategory(String planId, ChargeCategory category);

    List<EnhancedChargeEntity> findByPlanIdAndTiming(String planId, PaymentTiming timing);

    List<EnhancedChargeEntity> findByPlanIdAndApplicableTrue(String planId);

    List<EnhancedChargeEntity> findByPlanIdAndRefundableTrue(String planId);
}