package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.OwnerRazorpayConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OwnerRazorpayConfigRepository extends JpaRepository<OwnerRazorpayConfig, UUID> {
    
    Optional<OwnerRazorpayConfig> findByOwner_UserId(UUID ownerId);
    
    boolean existsByOwner_UserId(UUID ownerId);
}
