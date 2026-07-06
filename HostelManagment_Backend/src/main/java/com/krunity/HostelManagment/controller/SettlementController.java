package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.dto.SettlementApprovalDto;
import com.krunity.HostelManagment.dto.SettlementCalculationDto;
import com.krunity.HostelManagment.dto.SettlementRequestDto;
import com.krunity.HostelManagment.dto.SettlementResponseDto;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.RoomAllotment;
import com.krunity.HostelManagment.model.SettlementRequest;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.repository.AgreementRepository;
import com.krunity.HostelManagment.service.AllotmentService;
import com.krunity.HostelManagment.service.SettlementService;
import com.krunity.HostelManagment.Utils.ApplicationContext;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private AllotmentService allotmentService;

    @Autowired
    private AgreementRepository agreementRepository;

    @GetMapping("/debug/agreements")
    public ResponseEntity<?> debugAgreements() {
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Debug: Current user ID: {}, Username: {}", currentUser.getUserId(), currentUser.getUsername());
            
            List<Agreement> allAgreements = agreementRepository.findAll();
            log.info("Debug: Total agreements in system: {}", allAgreements.size());
            
            List<Agreement> userAgreements = agreementRepository.findByUserId(currentUser.getUserId());
            log.info("Debug: Agreements for user {}: {}", currentUser.getUserId(), userAgreements.size());
            
            return ResponseEntity.ok(Map.of(
                "currentUserId", currentUser.getUserId(),
                "totalAgreements", allAgreements.size(),
                "userAgreements", userAgreements,
                "allAgreementIds", allAgreements.stream().map(Agreement::getId).toList()
            ));
        } catch (Exception e) {
            log.error("Debug error: ", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/available-agreements")
    public ResponseEntity<List<Agreement>> getAvailableAgreements() {
        User currentUser = ApplicationContext.getUser();
        List<Agreement> agreements = agreementRepository.findByUserId(currentUser.getUserId());
        return ResponseEntity.ok(agreements);
    }

    @PostMapping("/request")
    public ResponseEntity<SettlementResponseDto> initiateSettlement(@Valid @RequestBody SettlementRequestDto requestDto) {
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Settlement request initiated by user: {} for agreement: {}", 
                    currentUser.getUserId(), requestDto.getAgreementId());
            
            SettlementRequest settlement = settlementService.initiateSettlement(requestDto, currentUser.getUserId());
            
            log.info("Settlement request created successfully with ID: {}", settlement.getSettlementId());
            
            // Convert to DTO to avoid Hibernate lazy loading serialization issues
            SettlementResponseDto responseDto = com.krunity.HostelManagment.Mapper.SettlementMapper.toResponseDto(settlement);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Error creating settlement request: ", e);
            throw e;
        }
    }

    @GetMapping("/{settlementId}/calculate")
    public ResponseEntity<SettlementCalculationDto> calculateSettlement(@PathVariable UUID settlementId) {
        SettlementCalculationDto calculation = settlementService.calculateSettlement(settlementId);
        return ResponseEntity.ok(calculation);
    }

    @PostMapping("/{settlementId}/approve")
    public ResponseEntity<SettlementResponseDto> approveSettlement(
            @PathVariable UUID settlementId,
            @Valid @RequestBody SettlementApprovalDto approvalDto) {
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Processing settlement approval for ID: {} by owner: {}", settlementId, currentUser.getUserId());
            
            SettlementRequest settlement = settlementService.approveSettlement(settlementId, approvalDto, currentUser.getUserId());
            
            log.info("Settlement approval processed successfully for ID: {}", settlementId);
            
            // Convert to DTO to avoid Hibernate lazy loading serialization issues
            SettlementResponseDto responseDto = com.krunity.HostelManagment.Mapper.SettlementMapper.toResponseDto(settlement);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Error processing settlement approval: ", e);
            throw e;
        }
    }

    @PostMapping("/{settlementId}/complete")
    public ResponseEntity<SettlementResponseDto> completeSettlement(
            @PathVariable UUID settlementId,
            @RequestBody Map<String, String> payload) {
        try {
            User currentUser = ApplicationContext.getUser();
            String paymentReference = payload.get("paymentReference");
            log.info("Completing settlement for ID: {} with payment reference: {}", settlementId, paymentReference);
            
            SettlementRequest settlement = settlementService.completeSettlement(settlementId, paymentReference, currentUser.getUserId());
            
            log.info("Settlement completed successfully for ID: {}", settlementId);
            
            // Convert to DTO to avoid Hibernate lazy loading serialization issues
            SettlementResponseDto responseDto = com.krunity.HostelManagment.Mapper.SettlementMapper.toResponseDto(settlement);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Error completing settlement: ", e);
            throw e;
        }
    }

    /**
     * POST /api/settlements/allotments/{allotmentId}/confirm-left
     * Owner confirms the tenant has physically vacated.
     * LEFT is applied only when the tenant has also confirmed.
     */
    @PostMapping("/allotments/{allotmentId}/confirm-left")
    public ResponseEntity<?> ownerConfirmLeft(@PathVariable UUID allotmentId) {
        User owner = ApplicationContext.getUser();
        RoomAllotment allotment = allotmentService.markOwnerLeft(allotmentId, owner.getUserId());
        return ResponseEntity.ok(Map.of(
                "allotmentId", allotment.getAllotmentId(),
                "status", allotment.getRoomAllotmentStatus(),
                "tenantMarkedLeft", allotment.isTenantMarkedLeft(),
                "ownerMarkedLeft", allotment.isOwnerMarkedLeft()
        ));
    }

    @GetMapping("/owner")
    public ResponseEntity<List<SettlementResponseDto>> getOwnerSettlements() {
        User currentUser = ApplicationContext.getUser();
        List<SettlementResponseDto> settlements = settlementService.getOwnerSettlements(currentUser.getUserId());
        return ResponseEntity.ok(settlements);
    }

    @GetMapping("/tenant")
    public ResponseEntity<List<SettlementResponseDto>> getTenantSettlements() {
        try {
            User currentUser = ApplicationContext.getUser();
            log.info("Fetching tenant settlements for user: {}", currentUser.getUserId());
            
            List<SettlementResponseDto> settlements = settlementService.getTenantSettlements(currentUser.getUserId());
            
            log.info("Found {} settlements for tenant: {}", settlements.size(), currentUser.getUserId());
            return ResponseEntity.ok(settlements);
        } catch (Exception e) {
            log.error("Error fetching tenant settlements: ", e);
            throw e;
        }
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<SettlementCalculationDto> getSettlement(@PathVariable UUID settlementId) {
        SettlementCalculationDto calculation = settlementService.calculateSettlement(settlementId);
        return ResponseEntity.ok(calculation);
    }
}