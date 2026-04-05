package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.Agreement;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.Optional;

public interface AgreementRepository extends MongoRepository<Agreement, String> {
    Optional<Agreement> findByQrTokenAndQrUsedFalse(String token);
    Optional<Agreement> findByQrTokenAndQrUsedFalseAndQrExpiryAfter(String token, Instant now);
}
