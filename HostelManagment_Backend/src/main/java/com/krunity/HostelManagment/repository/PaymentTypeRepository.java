package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTypeRepository extends JpaRepository<PaymentType, UUID> {
    Optional<PaymentType> findByTypeNameIgnoreCase(String typeName);
}
