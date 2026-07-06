package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.CashPaymentOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface CashPaymentOtpRepository extends JpaRepository<CashPaymentOtp, UUID> {

    void deleteByExpiryTimeBefore(Instant now);
}
