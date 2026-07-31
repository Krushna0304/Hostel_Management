package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.ExtendAllotmentStatus;
import com.krunity.HostelManagment.model.ExtendAllotmentRequest;
import com.krunity.HostelManagment.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExtendAllotmentRequestRepository extends JpaRepository<ExtendAllotmentRequest, UUID> {

    /**
     * Find extension requests by tenant ordered by creation date descending
     */
    List<ExtendAllotmentRequest> findByTenantOrderByCreatedAtDesc(User tenant);

    /**
     * Find extension requests by owner ordered by creation date descending
     */
    List<ExtendAllotmentRequest> findByOwnerOrderByCreatedAtDesc(User owner);

    /**
     * Find pending owner approval requests for a specific owner
     */
    @Query("""
        SELECT e FROM ExtendAllotmentRequest e
        WHERE e.owner = :owner
          AND e.status = 'PENDING_OWNER_APPROVAL'
          AND (e.expiresAt IS NULL OR e.expiresAt > CURRENT_TIMESTAMP)
        ORDER BY e.createdAt DESC
        """)
    List<ExtendAllotmentRequest> findPendingOwnerApprovalRequests(@Param("owner") User owner);

    /**
     * Check if tenant has any active extension request for a specific agreement
     */
    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
        FROM ExtendAllotmentRequest e
        WHERE e.tenant = :tenant
          AND e.currentAgreementId = :agreementId
          AND e.status IN ('PENDING_OWNER_APPROVAL', 'APPROVED_PENDING_PAYMENT')
          AND (e.expiresAt IS NULL OR e.expiresAt > CURRENT_TIMESTAMP)
        """)
    Boolean hasActiveTenantExtensionRequest(@Param("tenant") User tenant, 
                                          @Param("agreementId") String agreementId);

    /**
     * Find requests by status
     */
    List<ExtendAllotmentRequest> findByStatusOrderByCreatedAtDesc(ExtendAllotmentStatus status);

    /**
     * Find expired requests that need cleanup
     */
    @Query("""
        SELECT e FROM ExtendAllotmentRequest e
        WHERE e.status IN ('PENDING_OWNER_APPROVAL', 'APPROVED_PENDING_PAYMENT')
          AND e.expiresAt IS NOT NULL
          AND e.expiresAt < :currentTime
        """)
    List<ExtendAllotmentRequest> findExpiredRequests(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find extension requests by tenant and status
     */
    List<ExtendAllotmentRequest> findByTenantAndStatusOrderByCreatedAtDesc(User tenant, ExtendAllotmentStatus status);

    /**
     * Find extension requests by owner and status
     */
    List<ExtendAllotmentRequest> findByOwnerAndStatusOrderByCreatedAtDesc(User owner, ExtendAllotmentStatus status);

    /**
     * Find requests that are about to expire (for notifications)
     */
    @Query("""
        SELECT e FROM ExtendAllotmentRequest e
        WHERE e.status IN ('PENDING_OWNER_APPROVAL', 'APPROVED_PENDING_PAYMENT')
          AND e.expiresAt IS NOT NULL
          AND e.expiresAt BETWEEN :startTime AND :endTime
        """)
    List<ExtendAllotmentRequest> findRequestsExpiringBetween(@Param("startTime") LocalDateTime startTime,
                                                           @Param("endTime") LocalDateTime endTime);

    /**
     * Count active extension requests for a tenant
     */
    @Query("""
        SELECT COUNT(e) FROM ExtendAllotmentRequest e
        WHERE e.tenant = :tenant
          AND e.status IN ('PENDING_OWNER_APPROVAL', 'APPROVED_PENDING_PAYMENT', 'PAYMENT_COMPLETED', 'AGREEMENT_CREATED', 'ACTIVE')
        """)
    Long countActiveTenantExtensionRequests(@Param("tenant") User tenant);

    /**
     * Count pending approval requests for an owner
     */
    @Query("""
        SELECT COUNT(e) FROM ExtendAllotmentRequest e
        WHERE e.owner = :owner
          AND e.status = 'PENDING_OWNER_APPROVAL'
          AND (e.expiresAt IS NULL OR e.expiresAt > CURRENT_TIMESTAMP)
        """)
    Long countPendingApprovalRequestsForOwner(@Param("owner") User owner);
}