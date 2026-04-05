package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.enums.PlanStatus;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RoomAgreementPlanRepository extends MongoRepository<RoomAgreementPlan, String> {
    List<RoomAgreementPlan> findByStatus(PlanStatus status);
    Optional<RoomAgreementPlan> findByIdAndStatus(String id, PlanStatus status);
}

