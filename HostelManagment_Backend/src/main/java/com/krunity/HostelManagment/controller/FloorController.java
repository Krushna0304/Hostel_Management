package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.dto.CreateFloorRequest;
import com.krunity.HostelManagment.dto.FloorResponse;
import com.krunity.HostelManagment.service.FloorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hostels/{hostelId}/floors")
public class FloorController {

    //Curd operations for Floors and Rooms can be added here

    private final FloorService floorService;
    public  FloorController(FloorService floorService){
        this.floorService = floorService;
    }


    //Add Floor to Hostel
    @PostMapping
    public ResponseEntity<?> addFloorToHostel(@PathVariable String hostelId,@RequestBody @Valid CreateFloorRequest createFloorRequest) {
            floorService.createFloor(hostelId,createFloorRequest);
            return ResponseEntity.status(201).build();
    }

    @GetMapping("/{floorId}")
    public ResponseEntity<?> getFloorToHostel(@PathVariable String hostelId,@PathVariable String floorId) {

        try{
            FloorResponse floorResponse = floorService.getFloor(hostelId,floorId);
            return ResponseEntity.status(200).body(floorResponse);
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error getting floor from hostel: " + e.getMessage());
        }
    }

    @PutMapping("/{floorId}")
    public ResponseEntity<?> updateFloorToHostel(@PathVariable String hostelId,@PathVariable String floorId,@RequestBody @Valid CreateFloorRequest createFloorRequest) {
        try{
            FloorResponse floorResponse = floorService.updateFloor(floorId,createFloorRequest);
            return ResponseEntity.status(200).body(floorResponse );
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error updating floor to hostel: " + e.getMessage());
        }
    }

    @DeleteMapping("/{floorId}")
    public ResponseEntity<?> deleteFloorFromHostel(@PathVariable String hostelId,@PathVariable String floorId) {
        try{
            floorService.deleteFloor(floorId);
            return ResponseEntity.status(204).build();
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error deleting floor from hostel: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllFloorsInHostel(@PathVariable String hostelId) {
        try{
            var floors = floorService.getAllFloorsInHostel(hostelId);
            return ResponseEntity.status(200).body(floors);
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error getting floors from hostel: " + e.getMessage());
        }

    }
}
