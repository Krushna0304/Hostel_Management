package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.PlanStatus;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomAgreementPlanRepository extends MongoRepository<RoomAgreementPlan, String> {
    // Global (system) plans — ownerId is null
    List<RoomAgreementPlan> findByStatus(PlanStatus status);
    Optional<RoomAgreementPlan> findByIdAndStatus(String id, PlanStatus status);

    // Owner-specific plans
    List<RoomAgreementPlan> findByOwnerIdAndStatus(UUID ownerId, PlanStatus status);

    // Plans visible to an owner = their own plans + global plans (ownerId is null)
    List<RoomAgreementPlan> findByOwnerIdIsNullAndStatus(PlanStatus status);

    // Delete owner's own plan
    void deleteByIdAndOwnerId(String id, UUID ownerId);

    // Check ownership
    Optional<RoomAgreementPlan> findByIdAndOwnerId(String id, UUID ownerId);

    // planType-filtered queries for global plans (ownerId is null)
    // PG_ROOM filter: planType = 'PG_ROOM' OR planType is absent/null
    @Query("{ 'ownerId': null, 'status': ?0, $or: [ { 'planType': 'PG_ROOM' }, { 'planType': { $exists: false } }, { 'planType': null } ] }")
    List<RoomAgreementPlan> findGlobalActiveByPlanTypePgRoom(PlanStatus status);

    @Query("{ 'ownerId': null, 'status': ?0, 'planType': 'FLAT' }")
    List<RoomAgreementPlan> findGlobalActiveByPlanTypeFlat(PlanStatus status);

    // planType-filtered queries for owner-specific plans
    @Query("{ 'ownerId': ?0, 'status': ?1, $or: [ { 'planType': 'PG_ROOM' }, { 'planType': { $exists: false } }, { 'planType': null } ] }")
    List<RoomAgreementPlan> findByOwnerIdAndStatusAndPlanTypePgRoom(UUID ownerId, PlanStatus status);

    @Query("{ 'ownerId': ?0, 'status': ?1, 'planType': 'FLAT' }")
    List<RoomAgreementPlan> findByOwnerIdAndStatusAndPlanTypeFlat(UUID ownerId, PlanStatus status);
}

