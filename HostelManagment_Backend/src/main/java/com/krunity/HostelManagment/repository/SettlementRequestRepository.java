package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.SettlementStatus;
import com.krunity.HostelManagment.model.SettlementRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementRequestRepository extends JpaRepository<SettlementRequest, UUID> {
    
    Optional<SettlementRequest> findByAgreementId(String agreementId);
    
    List<SettlementRequest> findByOwnerUserIdAndStatusIn(UUID ownerId, List<SettlementStatus> statuses);
    
    List<SettlementRequest> findByTenantUserIdAndStatusIn(UUID tenantId, List<SettlementStatus> statuses);
    
    @Query("SELECT sr FROM SettlementRequest sr " +
           "LEFT JOIN FETCH sr.owner " +
           "LEFT JOIN FETCH sr.tenant " +
           "LEFT JOIN FETCH sr.room " +
           "WHERE sr.owner.userId = :ownerId " +
           "ORDER BY sr.createdAt DESC")
    List<SettlementRequest> findByOwnerOrderByCreatedAtDesc(@Param("ownerId") UUID ownerId);
    
    @Query("SELECT sr FROM SettlementRequest sr " +
           "LEFT JOIN FETCH sr.owner " +
           "LEFT JOIN FETCH sr.tenant " +
           "LEFT JOIN FETCH sr.room " +
           "WHERE sr.tenant.userId = :tenantId " +
           "ORDER BY sr.createdAt DESC")
    List<SettlementRequest> findByTenantOrderByCreatedAtDesc(@Param("tenantId") UUID tenantId);
    
    boolean existsByAgreementIdAndStatusNotIn(String agreementId, List<SettlementStatus> excludeStatuses);
}