package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.ElectricityPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ElectricityPaymentRepository extends JpaRepository<ElectricityPayment, UUID> {
    
    List<ElectricityPayment> findByBillIdOrderByCreatedAtDesc(UUID billId);
    
    List<ElectricityPayment> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    
    @Query("SELECT ep FROM ElectricityPayment ep " +
           "JOIN FETCH ep.tenant t " +
           "WHERE ep.billId = :billId " +
           "ORDER BY ep.createdAt DESC")
    List<ElectricityPayment> findByBillIdWithTenantDetails(@Param("billId") UUID billId);
}