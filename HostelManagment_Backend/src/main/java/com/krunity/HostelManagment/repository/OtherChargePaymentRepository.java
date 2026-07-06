package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.OtherChargePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtherChargePaymentRepository extends JpaRepository<OtherChargePayment, UUID> {

    List<OtherChargePayment> findByChargeId(UUID chargeId);

    Optional<OtherChargePayment> findByChargeIdAndTenantId(UUID chargeId, UUID tenantId);

    boolean existsByChargeId(UUID chargeId);
}
