package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.Floor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface FloorRepository extends JpaRepository<Floor, UUID> {
    int deleteByFloorIdAndHostel_Owner_UserId(UUID floorId,UUID ownerId);
    Floor findByFloorIdAndHostel_Owner_UserId(UUID hostelId, UUID ownerId);
    List<Floor> findAllByHostel_HostelIdAndHostel_Owner_UserId(UUID hostelId, UUID ownerId);
    boolean existsByFloorNumberAndHostel_HostelId(Integer floorNumber, UUID hostelId);
}
