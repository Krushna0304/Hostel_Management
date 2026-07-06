package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.PaymentOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentOtpRepository extends JpaRepository<PaymentOtp, UUID> {

    // Outstanding (unused) authorisations for a request, newest first.
    List<PaymentOtp> findByRequestIdAndUsedFalseOrderByCreatedAtDesc(String requestId);
}
