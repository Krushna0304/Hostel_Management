package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.dto.PlanResponse;
import com.krunity.HostelManagment.service.RoomAgreementPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {
    
    @Autowired
    private RoomAgreementPlanService planService;
    
    @GetMapping("/active")
    public ResponseEntity<List<PlanResponse>> getActivePlans() {
        List<PlanResponse> plans = planService.getActivePlans();
        return ResponseEntity.ok(plans);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getPlanById(@PathVariable String id) {
        try {
            PlanResponse plan = planService.getPlanResponseById(id);
            return ResponseEntity.ok(plan);
        } catch (com.krunity.HostelManagment.exception.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

