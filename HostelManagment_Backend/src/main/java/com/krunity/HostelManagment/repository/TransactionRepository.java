package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
}

