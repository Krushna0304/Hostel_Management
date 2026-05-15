package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.Hostel;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;
@Repository
public interface HostelRepository extends JpaRepository<Hostel, UUID> {

    int deleteByHostelIdAndOwner_UserId(UUID hostelId, UUID ownerId);

    Hostel findByHostelIdAndOwner_UserId(UUID hostelId, UUID ownerId);

    List<Hostel> findByOwner_UserId(UUID ownerId);
    
    boolean existsByHostelNameAndOwner_UserId(String hostelName, UUID ownerId);
}
