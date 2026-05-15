package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.ChargeCategory;
import com.krunity.HostelManagment.enums.PaymentStatus;
import com.krunity.HostelManagment.model.OtherCharge;
import com.krunity.HostelManagment.model.Room;
import com.krunity.HostelManagment.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OtherChargeRepository extends JpaRepository<OtherCharge, UUID> {

    // Find charges by owner
    List<OtherCharge> findByOwnerAndActiveTrue(User owner);
    
    Page<OtherCharge> findByOwnerAndActiveTrue(User owner, Pageable pageable);

    // Find charges by tenant
    List<OtherCharge> findByTenantAndActiveTrue(User tenant);
    
    Page<OtherCharge> findByTenantAndActiveTrue(User tenant, Pageable pageable);

    // Find charges by room
    List<OtherCharge> findByRoomAndActiveTrue(Room room);

    // Find charges by hostel and owner
    @Query("SELECT oc FROM OtherCharge oc WHERE oc.hostel.hostelId = :hostelId AND oc.owner = :owner AND oc.active = true")
    List<OtherCharge> findByHostelAndOwner(@Param("hostelId") UUID hostelId, @Param("owner") User owner);

    // Find charges by category
    List<OtherCharge> findByCategoryAndOwnerAndActiveTrue(ChargeCategory category, User owner);

    // Find pending charges for a tenant
    @Query("SELECT oc FROM OtherCharge oc WHERE oc.tenant = :tenant AND oc.paymentStatus IN :statuses AND oc.active = true")
    List<OtherCharge> findByTenantAndPaymentStatusIn(@Param("tenant") User tenant, @Param("statuses") List<PaymentStatus> statuses);

    // Find overdue charges
    @Query("SELECT oc FROM OtherCharge oc WHERE oc.dueDate < :currentDate AND oc.paymentStatus != 'COMPLETED' AND oc.active = true")
    List<OtherCharge> findOverdueCharges(@Param("currentDate") LocalDateTime currentDate);

    // Find charges by room with pending status
    @Query("SELECT oc FROM OtherCharge oc WHERE oc.room = :room AND oc.paymentStatus IN :statuses AND oc.active = true")
    List<OtherCharge> findByRoomAndPaymentStatusIn(@Param("room") Room room, @Param("statuses") List<PaymentStatus> statuses);

    // Summary queries for dashboard
    @Query("SELECT SUM(oc.amount) FROM OtherCharge oc WHERE oc.owner = :owner AND oc.paymentStatus = 'COMPLETED' AND oc.active = true")
    BigDecimal getTotalCollectedByOwner(@Param("owner") User owner);

    @Query("SELECT SUM(oc.amount - COALESCE(oc.paidAmount, 0)) FROM OtherCharge oc WHERE oc.owner = :owner AND oc.paymentStatus != 'COMPLETED' AND oc.active = true")
    BigDecimal getTotalPendingByOwner(@Param("owner") User owner);

    @Query("SELECT SUM(oc.amount - COALESCE(oc.paidAmount, 0)) FROM OtherCharge oc WHERE oc.owner = :owner AND oc.dueDate < :currentDate AND oc.paymentStatus != 'COMPLETED' AND oc.active = true")
    BigDecimal getTotalOverdueByOwner(@Param("owner") User owner, @Param("currentDate") LocalDateTime currentDate);

    // Count queries
    @Query("SELECT COUNT(oc) FROM OtherCharge oc WHERE oc.owner = :owner AND oc.paymentStatus != 'COMPLETED' AND oc.active = true")
    Long countPendingChargesByOwner(@Param("owner") User owner);

    @Query("SELECT COUNT(oc) FROM OtherCharge oc WHERE oc.owner = :owner AND oc.dueDate < :currentDate AND oc.paymentStatus != 'COMPLETED' AND oc.active = true")
    Long countOverdueChargesByOwner(@Param("owner") User owner, @Param("currentDate") LocalDateTime currentDate);
}