package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.BillStatus;
import com.krunity.HostelManagment.model.ElectricityBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectricityBillRepository extends JpaRepository<ElectricityBill, UUID> {
    
    List<ElectricityBill> findByOwnerIdOrderByBillYearDescBillMonthDesc(UUID ownerId);
    
    List<ElectricityBill> findByTenantIdOrderByBillYearDescBillMonthDesc(UUID tenantId);
    
    List<ElectricityBill> findByOwnerIdAndBillMonthAndBillYear(UUID ownerId, Integer billMonth, Integer billYear);
    
    Optional<ElectricityBill> findByAccountIdAndBillMonthAndBillYear(UUID accountId, Integer billMonth, Integer billYear);
    
    List<ElectricityBill> findByStatusIn(List<BillStatus> statuses);
    
    @Query("SELECT eb FROM ElectricityBill eb " +
           "JOIN FETCH eb.electricityAccount ea " +
           "JOIN FETCH ea.room r " +
           "JOIN FETCH r.hostel " +
           "WHERE eb.ownerId = :ownerId " +
           "ORDER BY eb.billYear DESC, eb.billMonth DESC")
    List<ElectricityBill> findByOwnerIdWithAccountAndRoomDetails(@Param("ownerId") UUID ownerId);
    
    @Query("SELECT eb FROM ElectricityBill eb " +
           "JOIN FETCH eb.electricityAccount ea " +
           "JOIN FETCH ea.room r " +
           "JOIN FETCH r.hostel " +
           "WHERE eb.tenantId = :tenantId " +
           "ORDER BY eb.billYear DESC, eb.billMonth DESC")
    List<ElectricityBill> findByTenantIdWithAccountAndRoomDetails(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(eb.remainingAmount), 0) FROM ElectricityBill eb WHERE eb.accountId = :accountId")
    java.math.BigDecimal sumRemainingAmountByAccountId(@Param("accountId") UUID accountId);
}