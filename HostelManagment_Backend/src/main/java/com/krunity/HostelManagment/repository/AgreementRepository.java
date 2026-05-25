package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.AgreementStatus;
import com.krunity.HostelManagment.model.Agreement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgreementRepository extends MongoRepository<Agreement, String> {
    Optional<Agreement> findByQrTokenAndQrUsedFalse(String token);
    Optional<Agreement> findByQrTokenAndQrUsedFalseAndQrExpiryAfter(String token, Instant now);
    List<Agreement> findByOwnerId(UUID ownerId);
    List<Agreement> findByUserId(UUID userId);
    Optional<Agreement> findByIdAndOwnerId(String id, UUID ownerId);
    List<Agreement> findByRoomIdAndStatusIn(UUID roomId, List<AgreementStatus> statuses);
    Optional<Agreement> findByRoomIdAndStatus(UUID roomId, AgreementStatus status);
    
    // Helper method to find active agreement for a room
    default Optional<Agreement> findActiveAgreementByRoomId(UUID roomId) {
        return findByRoomIdAndStatus(roomId, AgreementStatus.ACTIVE);
    }
}
