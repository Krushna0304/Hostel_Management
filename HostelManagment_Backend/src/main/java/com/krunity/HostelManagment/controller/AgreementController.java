package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.Mapper.AgreementMapper;
import com.krunity.HostelManagment.dto.AcceptAgreementRequest;
import com.krunity.HostelManagment.dto.AgreementResponse;
import com.krunity.HostelManagment.dto.CreateRoomAgreementRequest;
import com.krunity.HostelManagment.dto.QrActivationResponse;
import com.krunity.HostelManagment.dto.QrCodeResponse;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import com.krunity.HostelManagment.service.AgreementService;
import com.krunity.HostelManagment.service.RoomAgreementPlanService;
import com.krunity.HostelManagment.Utils.QrCodeGenerator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agreements")
public class AgreementController {
    
    @Autowired
    private AgreementService agreementService;
    
    @Autowired
    private RoomAgreementPlanService planService;

    @PostMapping("/room")
    public ResponseEntity<QrActivationResponse> createRoomAgreement(
            @Valid @RequestBody CreateRoomAgreementRequest request) {
        // Fetch the plan and create a snapshot
        RoomAgreementPlan planSnapshot = planService.getPlanById(request.getPlanId());
        
        // Create agreement with plan snapshot
        Agreement agreement = AgreementMapper.toEntity(request, planSnapshot);
        Agreement created = agreementService.createAgreement(agreement);
        
        QrActivationResponse response = new QrActivationResponse();
        response.setAgreementId(created.getId());
        response.setQrToken(created.getQrToken());
        response.setExpiry(created.getQrExpiry());
        
        return ResponseEntity.ok(response);
    }


    //What is the use of this
    @GetMapping("/qr/{token}")
    public ResponseEntity<AgreementResponse> getAgreementByQrToken(@PathVariable String token) {
        Optional<Agreement> agreementOpt = agreementService.validateQrToken(token);
        
        if (agreementOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        AgreementResponse response = AgreementMapper.toResponse(agreementOpt.get());
        return ResponseEntity.ok(response);
    }

    //What is the use of this
    @GetMapping("/qr/{token}/image")
    public ResponseEntity<QrCodeResponse> getQrCodeImage(@PathVariable String token) {
        Optional<Agreement> agreementOpt = agreementService.validateQrToken(token);
        
        if (agreementOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Agreement agreement = agreementOpt.get();
        try {
            String frontendUrl = "http://localhost:3000";
            String activationUrl = frontendUrl + "/tenant/activate?token=" + agreement.getQrToken();
            String qrCodeBase64 = QrCodeGenerator.generateQrCodeBase64(activationUrl);
            
            QrCodeResponse response = new QrCodeResponse();
            response.setQrCodeBase64(qrCodeBase64);
            response.setActivationUrl(activationUrl);
            response.setQrToken(agreement.getQrToken());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<AgreementResponse>> getAllAgreements() {
        List<Agreement> agreements = agreementService.getAllAgreements();
        List<AgreementResponse> responses = agreements.stream()
                .map(AgreementMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<AgreementResponse> acceptAgreement(
            @PathVariable String id,
            @Valid @RequestBody AcceptAgreementRequest request) {
        Agreement activated = agreementService.activateAgreement(id, request);
        AgreementResponse response = AgreementMapper.toResponse(activated);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AgreementResponse> rejectAgreement(@PathVariable String id) {
        Agreement rejected = agreementService.rejectAgreement(id);
        AgreementResponse response = AgreementMapper.toResponse(rejected);
        return ResponseEntity.ok(response);
    }
}
