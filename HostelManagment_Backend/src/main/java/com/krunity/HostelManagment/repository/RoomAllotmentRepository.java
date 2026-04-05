package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.RoomAllotment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomAllotmentRepository extends JpaRepository<RoomAllotment, Integer> {
}
