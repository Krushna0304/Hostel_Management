package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.CashPaymentOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashPaymentOtpRepository extends JpaRepository<CashPaymentOtp, UUID> {
    
    Optional<CashPaymentOtp> findByAgreementIdAndUsedFalseAndExpiryTimeAfter(
        String agreementId, 
        Instant now
    );
    
    Optional<CashPaymentOtp> findByScheduleIdAndUsedFalseAndExpiryTimeAfter(
        UUID scheduleId,
        Instant now
    );
    
    List<CashPaymentOtp> findAllByScheduleIdAndUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
        UUID scheduleId,
        Instant now
    );
    
    List<CashPaymentOtp> findAllByChargeIdAndUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
        String chargeId,
        Instant now
    );
    
    List<CashPaymentOtp> findAllBySettlementIdAndUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
        String settlementId,
        Instant now
    );
    
    List<CashPaymentOtp> findAllByElectricityBillIdAndUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
        UUID electricityBillId,
        Instant now
    );
    
    void deleteByExpiryTimeBefore(Instant now);
}
