package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.Mapper.AgreementMapper;
import com.krunity.HostelManagment.dto.AcceptAgreementRequest;
import com.krunity.HostelManagment.dto.AcceptAgreementResponse;
import com.krunity.HostelManagment.dto.AgreementCardResponse;
import com.krunity.HostelManagment.dto.AgreementResponse;
import com.krunity.HostelManagment.dto.CreateFlatAgreementRequest;
import com.krunity.HostelManagment.dto.CreateRoomAgreementRequest;
import com.krunity.HostelManagment.dto.QrActivationResponse;
import com.krunity.HostelManagment.dto.QrCodeResponse;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.model.Room;
import com.krunity.HostelManagment.model.Floor;
import com.krunity.HostelManagment.model.Hostel;
import com.krunity.HostelManagment.service.AgreementService;
import com.krunity.HostelManagment.service.RoomAgreementPlanService;
import com.krunity.HostelManagment.repository.UserRepository;
import com.krunity.HostelManagment.repository.RoomRepository;
import com.krunity.HostelManagment.repository.FloorRepository;
import com.krunity.HostelManagment.repository.HostelRepository;
import com.krunity.HostelManagment.Utils.QrCodeGenerator;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/agreements")
public class AgreementController {
    
    @Autowired
    private AgreementService agreementService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private FloorRepository floorRepository;
    
    @Autowired
    private HostelRepository hostelRepository;
    
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
        
        // Mark plan as in use
        planService.markPlanAsInUse(request.getPlanId());
        
        QrActivationResponse response = new QrActivationResponse();
        response.setAgreementId(created.getId());
        response.setQrToken(created.getQrToken());
        response.setExpiry(created.getQrExpiry());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/flat")
    public ResponseEntity<QrActivationResponse> createFlatAgreement(
            @Valid @RequestBody CreateFlatAgreementRequest request) {
        QrActivationResponse response = agreementService.createFlatAgreement(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
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
            String frontendUrl = "https://hostel-management-dashboard.onrender.com";
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
                .map(agreement -> {
                    // Fetch related entities
                    User tenant = null;
                    Room room = null;
                    Floor floor = null;
                    Hostel hostel = null;
                    
                    try {
                        // Fetch tenant details
                        if (agreement.getUserId() != null) {
                            tenant = userRepository.findById(agreement.getUserId()).orElse(null);
                        }
                        
                        // Fetch room and related entities
                        if (agreement.getRoomId() != null) {
                            Optional<Room> roomOpt = roomRepository.findById(agreement.getRoomId());
                            if (roomOpt.isPresent()) {
                                room = roomOpt.get();
                                floor = room.getFloor();
                                hostel = floor != null ? floor.getHostel() : null;
                            }
                        }
                    } catch (Exception e) {
                        // Log error but continue with basic mapping
                        log.error("Error fetching agreement details: {}", e.getMessage());
                    }
                    
                    return AgreementMapper.toResponseWithDetails(agreement, tenant, room, floor, hostel);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/cards")
    public ResponseEntity<List<AgreementCardResponse>> getAgreementCards() {
        List<Agreement> agreements = agreementService.getAllAgreements();
        List<AgreementCardResponse> responses = agreements.stream()
                .map(agreement -> {
                    // Fetch only necessary related entities for card display
                    User tenant = null;
                    Room room = null;
                    Floor floor = null;
                    Hostel hostel = null;
                    
                    try {
                        // Fetch tenant details
                        if (agreement.getUserId() != null) {
                            tenant = userRepository.findById(agreement.getUserId()).orElse(null);
                        }
                        
                        // Fetch room and related entities
                        if (agreement.getRoomId() != null) {
                            Optional<Room> roomOpt = roomRepository.findById(agreement.getRoomId());
                            if (roomOpt.isPresent()) {
                                room = roomOpt.get();
                                floor = room.getFloor();
                                hostel = floor != null ? floor.getHostel() : null;
                            }
                        }
                    } catch (Exception e) {
                        // Log error but continue with basic mapping
                        log.error("Error fetching agreement card details: {}", e.getMessage());
                    }
                    
                    return AgreementMapper.toCardResponse(agreement, tenant, room, floor, hostel);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgreementResponse> getAgreementById(@PathVariable String id) {
        Optional<Agreement> agreementOpt = agreementService.getAgreementById(id);
        
        if (agreementOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Agreement agreement = agreementOpt.get();
        
        // Fetch related entities for complete details
        User tenant = null;
        Room room = null;
        Floor floor = null;
        Hostel hostel = null;
        
        try {
            // Fetch tenant details
            if (agreement.getUserId() != null) {
                tenant = userRepository.findById(agreement.getUserId()).orElse(null);
            }
            
            // Fetch room and related entities
            if (agreement.getRoomId() != null) {
                Optional<Room> roomOpt = roomRepository.findById(agreement.getRoomId());
                if (roomOpt.isPresent()) {
                    room = roomOpt.get();
                    floor = room.getFloor();
                    hostel = floor != null ? floor.getHostel() : null;
                }
            }
        } catch (Exception e) {
            // Log error but continue with basic mapping
            log.error("Error fetching agreement details for id {}: {}", id, e.getMessage());
        }
        
        AgreementResponse response = AgreementMapper.toResponseWithDetails(agreement, tenant, room, floor, hostel);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<AcceptAgreementResponse> acceptAgreement(
            @PathVariable String id,
            @Valid @RequestBody AcceptAgreementRequest request) {
        Agreement activated = agreementService.activateAgreement(id, request);
        AcceptAgreementResponse response = new AcceptAgreementResponse();
        response.setAgreementId(activated.getId());
        response.setStatus(activated.getStatus());
        response.setActivatedAt(activated.getActivatedAt());
        response.setPasswordResetToken(activated.getPasswordResetToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AgreementResponse> rejectAgreement(@PathVariable String id) {
        Agreement rejected = agreementService.rejectAgreement(id);
        AgreementResponse response = AgreementMapper.toResponse(rejected);
        return ResponseEntity.ok(response);
    }
}
