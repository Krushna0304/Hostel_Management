package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.dto.*;
import com.krunity.HostelManagment.service.HostelService;
import com.krunity.HostelManagment.service.RoomService;
import com.krunity.HostelManagment.service.UserService;
import com.krunity.HostelManagment.Utils.ApplicationContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/hostels")
public class HostelController {
    
    private final HostelService hostelService;
    private final RoomService roomService;
    private final UserService userService;
    
    public HostelController(HostelService hostelService, RoomService roomService, UserService userService) {
        this.hostelService = hostelService;
        this.roomService = roomService;
        this.userService = userService;
    }

    // Get all hostels for the current owner
    @GetMapping
    public ResponseEntity<?> getAllHostels() {
        List<HostelResponse> hostels = hostelService.getAllHostelsForCurrentUser();
        return ResponseEntity.ok(hostels);
    }

    //CURD operations for Hostel,Floors,Rooms Done by only logged-in user

    //create Hostel
    @PostMapping
    public ResponseEntity<?> createHostel(@RequestBody @Valid CreateHostelRequest createHostelRequest) {
        hostelService.createHostel(createHostelRequest);
        return ResponseEntity.status(201).build();
    }

    //Get Hostel
    @GetMapping("/{hostelId}")
    public ResponseEntity<?> getHostel(@PathVariable String hostelId) {
        HostelResponse hostelResponse = hostelService.getHostel(hostelId);
        return ResponseEntity.status(200).body(hostelResponse);
    }

    //update Hostel
    @PutMapping("/{hostelId}")
    public ResponseEntity<?> updateHostel(@PathVariable String hostelId, CreateHostelRequest createHostelRequest) {
        HostelResponse hostelResponse = hostelService.updateHostel(hostelId,createHostelRequest);
        return ResponseEntity.status(200).body(hostelResponse);
    }

    //delete Hostel
    @DeleteMapping("/{hostelId}")
    public ResponseEntity<?> deleteHostel(@PathVariable String hostelId) {
        hostelService.deleteHostel(hostelId);
        return ResponseEntity.status(204).build();
    }
}

/**
 * Owner-specific endpoints for hostels
 */
@RestController
@RequestMapping("/owner/hostels")
class OwnerHostelController {
    
    private final HostelService hostelService;
    private final RoomService roomService;
    private final UserService userService;
    
    public OwnerHostelController(HostelService hostelService, RoomService roomService, UserService userService) {
        this.hostelService = hostelService;
        this.roomService = roomService;
        this.userService = userService;
    }

    /**
     * Get all hostels for the current owner
     */
    @GetMapping
    public ResponseEntity<List<HostelResponse>> getOwnerHostels() {
        List<HostelResponse> hostels = hostelService.getAllHostelsForCurrentUser();
        return ResponseEntity.ok(hostels);
    }

    /**
     * Get all rooms for a specific hostel
     */
    @GetMapping("/{hostelId}/rooms")
    public ResponseEntity<List<RoomResponse>> getHostelRooms(@PathVariable String hostelId) {
        UUID hostelUuid = UUID.fromString(hostelId);
        UUID ownerId = ApplicationContext.getUser().getUserId();
        
        // Verify owner has access to this hostel
        hostelService.getHostel(hostelId); // This will throw exception if not authorized
        
        List<RoomResponse> rooms = roomService.getRoomsByHostel(hostelUuid);
        return ResponseEntity.ok(rooms);
    }

    /**
     * Get all tenants for a specific hostel
     */
    @GetMapping("/{hostelId}/tenants")
    public ResponseEntity<List<UserResponse>> getHostelTenants(@PathVariable String hostelId) {
        UUID hostelUuid = UUID.fromString(hostelId);
        UUID ownerId = ApplicationContext.getUser().getUserId();
        
        // Verify owner has access to this hostel
        hostelService.getHostel(hostelId); // This will throw exception if not authorized
        
        List<UserResponse> tenants = userService.getTenantsByHostel(hostelUuid);
        return ResponseEntity.ok(tenants);
    }
}
