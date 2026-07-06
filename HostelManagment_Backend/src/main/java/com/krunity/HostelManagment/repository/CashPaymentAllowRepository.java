package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.CashPaymentAllow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashPaymentAllowRepository extends JpaRepository<CashPaymentAllow, UUID> {

    List<CashPaymentAllow> findByOwnerId(UUID ownerId);

    Optional<CashPaymentAllow> findByOwnerIdAndMethodName(UUID ownerId, String methodName);

    boolean existsByOwnerIdAndMethodName(UUID ownerId, String methodName);
}
