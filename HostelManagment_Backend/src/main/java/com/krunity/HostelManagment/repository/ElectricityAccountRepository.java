package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.ElectricityAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectricityAccountRepository extends JpaRepository<ElectricityAccount, UUID> {
    
    List<ElectricityAccount> findByOwnerIdAndIsActiveTrue(UUID ownerId);
    
    Optional<ElectricityAccount> findByRoomIdAndIsActiveTrue(UUID roomId);
    
    Optional<ElectricityAccount> findByAccountNumberAndIsActiveTrue(String accountNumber);
    
    boolean existsByRoomIdAndIsActiveTrue(UUID roomId);
    
    boolean existsByAccountNumberAndIsActiveTrue(String accountNumber);
    
    @Query("SELECT ea FROM ElectricityAccount ea " +
           "JOIN FETCH ea.room r " +
           "JOIN FETCH r.hostel " +
           "WHERE ea.ownerId = :ownerId AND ea.isActive = true " +
           "ORDER BY r.roomNumber")
    List<ElectricityAccount> findByOwnerIdWithRoomDetails(@Param("ownerId") UUID ownerId);
}