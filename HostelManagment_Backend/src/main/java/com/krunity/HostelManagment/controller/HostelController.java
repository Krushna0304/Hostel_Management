package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.dto.*;
import com.krunity.HostelManagment.service.HostelService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/hostels")
public class HostelController {
    // Get all hostels for the current owner
    @GetMapping
    public ResponseEntity<?> getAllHostels() {
        try {
            List<HostelResponse> hostels = hostelService.getAllHostelsForCurrentUser();
             return ResponseEntity.ok(hostels);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching hostels: " + e.getMessage());
        }
    }

    //CURD operations for Hostel,Floors,Rooms Done by only logged-in user

    private final HostelService hostelService;
    public HostelController(HostelService hostelService) {
        this.hostelService = hostelService;
    }

    //create Hostel
    @PostMapping
    public ResponseEntity<?> createHostel(@RequestBody @Valid CreateHostelRequest createHostelRequest) {
        try{
            hostelService.createHostel(createHostelRequest);
            return ResponseEntity.status(201).build();
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error creating hostel: " + e.getMessage());
        }
    }

    //Get Hostel
    @GetMapping("/{hostelId}")
    public ResponseEntity<?> getHostel(@PathVariable String hostelId) {
        try{
            HostelResponse hostelResponse = hostelService.getHostel(hostelId);
            return ResponseEntity.status(200).body(hostelResponse);
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error creating hostel: " + e.getMessage());
        }
    }

    //update Hostel
    @PutMapping("/{hostelId}")
    public ResponseEntity<?> updateHostel(@PathVariable String hostelId, CreateHostelRequest createHostelRequest) {
        try{
            HostelResponse hostelResponse = hostelService.updateHostel(hostelId,createHostelRequest);
            return ResponseEntity.status(200).body(hostelResponse);
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error creating hostel: " + e.getMessage());
        }
    }

    //delete Hostel
    @DeleteMapping("/{hostelId}")
    public ResponseEntity<?> deleteHostel(@PathVariable String hostelId) {
        try{
            hostelService.deleteHostel(hostelId);
            return ResponseEntity.status(204).build();
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error deleting hostel: " + e.getMessage());
        }
    }
}
