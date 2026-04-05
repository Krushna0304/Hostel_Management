package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<Agreement.PasswordResetToken, UUID> {
    Optional<Agreement.PasswordResetToken> findByTokenAndUsedFalseAndExpiryDateAfter(String token, Instant now);
    Optional<Agreement.PasswordResetToken> findByUser_UserIdAndUsedFalseAndExpiryDateAfter(UUID userId, Instant now);
}

