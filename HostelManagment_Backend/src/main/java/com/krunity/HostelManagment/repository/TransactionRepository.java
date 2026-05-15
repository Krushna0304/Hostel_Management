package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByPlanId_PlanId(UUID planId);

    List<Transaction> findByFromUser_UserId(UUID userId);

    List<Transaction> findByToUser_UserId(UUID userId);

    List<Transaction> findByFromUser_UserIdAndStatus(UUID userId, TransactionStatus status);

    /** All transactions where the user is either the sender or the receiver, newest first */
    @Query("SELECT t FROM Transaction t WHERE t.fromUser.userId = :userId OR t.toUser.userId = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findAllByUserId(@Param("userId") UUID userId);
}

