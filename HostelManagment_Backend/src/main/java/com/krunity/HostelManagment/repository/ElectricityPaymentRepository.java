package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.ElectricityPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectricityPaymentRepository extends JpaRepository<ElectricityPayment, UUID> {

    List<ElectricityPayment> findByBillIdOrderByCreatedAtDesc(UUID billId);

    List<ElectricityPayment> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    // A tenant has at most one share per bill.
    Optional<ElectricityPayment> findByBillIdAndTenantId(UUID billId, UUID tenantId);

    boolean existsByBillIdAndTenantId(UUID billId, UUID tenantId);

    @Query("SELECT ep FROM ElectricityPayment ep " +
           "JOIN FETCH ep.tenant t " +
           "WHERE ep.billId = :billId " +
           "ORDER BY ep.createdAt DESC")
    List<ElectricityPayment> findByBillIdWithTenantDetails(@Param("billId") UUID billId);

    // All shares belonging to a tenant, with the parent bill + account + room + hostel
    // eagerly loaded for the tenant's bill list.
    @Query("SELECT ep FROM ElectricityPayment ep " +
           "JOIN FETCH ep.electricityBill eb " +
           "JOIN FETCH eb.electricityAccount ea " +
           "JOIN FETCH ea.room r " +
           "JOIN FETCH r.hostel " +
           "WHERE ep.tenantId = :tenantId " +
           "ORDER BY eb.billYear DESC, eb.billMonth DESC")
    List<ElectricityPayment> findByTenantIdWithBillDetails(@Param("tenantId") UUID tenantId);

    // All shares across an owner's bills, with tenant + room/hostel details, for the
    // owner Collections dashboard.
    @Query("SELECT ep FROM ElectricityPayment ep " +
           "JOIN FETCH ep.electricityBill eb " +
           "JOIN FETCH eb.electricityAccount ea " +
           "JOIN FETCH ea.room r " +
           "JOIN FETCH r.hostel " +
           "JOIN FETCH ep.tenant t " +
           "WHERE eb.ownerId = :ownerId " +
           "ORDER BY eb.billYear DESC, eb.billMonth DESC")
    List<ElectricityPayment> findByOwnerIdWithDetails(@Param("ownerId") UUID ownerId);

    // A single tenant's shares within an owner's bills.
    @Query("SELECT ep FROM ElectricityPayment ep " +
           "JOIN FETCH ep.electricityBill eb " +
           "JOIN FETCH eb.electricityAccount ea " +
           "JOIN FETCH ea.room r " +
           "JOIN FETCH r.hostel " +
           "JOIN FETCH ep.tenant t " +
           "WHERE eb.ownerId = :ownerId AND ep.tenantId = :tenantId " +
           "ORDER BY eb.billYear DESC, eb.billMonth DESC")
    List<ElectricityPayment> findByOwnerIdAndTenantIdWithDetails(@Param("ownerId") UUID ownerId,
                                                                 @Param("tenantId") UUID tenantId);
}