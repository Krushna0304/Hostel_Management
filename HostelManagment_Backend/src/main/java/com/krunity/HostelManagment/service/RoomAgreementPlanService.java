package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.Mapper.RoomAgreementPlanMapper;
import com.krunity.HostelManagment.dto.PlanResponse;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import com.krunity.HostelManagment.repository.RoomAgreementPlanRepository;
import com.krunity.HostelManagment.enums.PlanStatus;
import com.krunity.HostelManagment.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomAgreementPlanService {
    
    @Autowired
    private RoomAgreementPlanRepository planRepository;
    
    public List<PlanResponse> getActivePlans() {
        List<RoomAgreementPlan> plans = planRepository.findByStatus(PlanStatus.ACTIVE);
        return plans.stream()
                .map(RoomAgreementPlanMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public PlanResponse getPlanResponseById(String planId) {
        return RoomAgreementPlanMapper.toDto(getPlanById(planId));
    }


    public RoomAgreementPlan getPlanById(String planId) {
        return planRepository.findByIdAndStatus(planId, PlanStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Plan not found with ID: " + planId));

    }
//    public RoomAgreementPlan savePlan(RoomAgreementPlan plan) {
//        return planRepository.save(plan);
//    }
    

}

