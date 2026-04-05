package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.repository.PasswordResetTokenRepository;
import com.krunity.HostelManagment.repository.UserRepository;
import com.krunity.HostelManagment.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PasswordResetService {
    
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private static final int TOKEN_EXPIRY_HOURS = 24;
    
    @Transactional
    public String generatePasswordResetToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        
        // Invalidate existing tokens
        tokenRepository.findByUser_UserIdAndUsedFalseAndExpiryDateAfter(userId, Instant.now())
                .ifPresent(token -> {
                    token.setUsed(true);
                    tokenRepository.save(token);
                });
        
        // Generate new token
        String token = UUID.randomUUID().toString().replace("-", "");
        Agreement.PasswordResetToken resetToken = Agreement.PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiryDate(Instant.now().plusSeconds(TOKEN_EXPIRY_HOURS * 3600))
                .used(false)
                .build();
        
        tokenRepository.save(resetToken);
        return token;
    }
    
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Agreement.PasswordResetToken resetToken = tokenRepository
                .findByTokenAndUsedFalseAndExpiryDateAfter(token, Instant.now())
                .orElseThrow(() -> new NotFoundException("Invalid or expired token"));
        
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
    
    public boolean validateToken(String token) {
        return tokenRepository.findByTokenAndUsedFalseAndExpiryDateAfter(token, Instant.now())
                .isPresent();
    }
}

