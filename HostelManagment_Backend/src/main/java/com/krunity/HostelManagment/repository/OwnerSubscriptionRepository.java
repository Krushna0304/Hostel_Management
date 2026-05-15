package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.OwnerSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OwnerSubscriptionRepository extends JpaRepository<OwnerSubscription, UUID> {
    
    Optional<OwnerSubscription> findByOwner_UserIdAndIsActiveTrue(UUID ownerId);
    
    boolean existsByOwner_UserIdAndIsActiveTrue(UUID ownerId);
}
