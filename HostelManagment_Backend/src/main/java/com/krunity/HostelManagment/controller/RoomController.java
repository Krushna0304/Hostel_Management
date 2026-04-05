package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.dto.CreateRoomRequest;
import com.krunity.HostelManagment.dto.RoomResponse;
import com.krunity.HostelManagment.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hostels/{hostelId}/{floorId}/rooms")
public class RoomController {

    //CURD operations for Rooms

    private final RoomService roomService;
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }


    @GetMapping
    public ResponseEntity<?> getAllRooms(@PathVariable String hostelId,@PathVariable String floorId) {
        try{
            var roomResponses = roomService.getAllRooms(hostelId,floorId);
            return ResponseEntity.status(200).body(roomResponses);
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error getting rooms from hostel: " + e.getMessage());
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getAllActiveRooms(@PathVariable String hostelId,@PathVariable String floorId) {
        try{
            var roomResponses = roomService.getAllActiveRooms(hostelId,floorId);
            return ResponseEntity.status(200).body(roomResponses);
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error getting rooms from hostel: " + e.getMessage());
        }
    }
    //Add Room to Floor
    @PostMapping
    public ResponseEntity<?> addRoomToFloor(@PathVariable String hostelId,@PathVariable String floorId
            ,@RequestBody @Valid CreateRoomRequest createRoomRequest) {

            roomService.createRoom(hostelId,floorId,createRoomRequest);
            return ResponseEntity.status(201).build();
    }

    //Update Room details
    @PutMapping("/{roomId}")
    public ResponseEntity<?> updateRoomDetails(@PathVariable String hostelId,@PathVariable String roomId
            , @RequestBody @Valid CreateRoomRequest createRoomRequest) {
        try{
            RoomResponse roomResponse = roomService.updateRoom(hostelId,roomId,createRoomRequest);
            return ResponseEntity.status(200).body(roomResponse);
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error creating room to hostel: " + e.getMessage());
        }
    }

    //delete Room from Floor
    @DeleteMapping("/{roomId}")
    public ResponseEntity<?> deleteRoomFromFloor(@PathVariable String hostelId,@PathVariable String roomId) {
        try{
            roomService.deleteRoom(hostelId,roomId);
            return ResponseEntity.status(204).build();
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error deleting room from hostel: " + e.getMessage());
        }
    }

    //get Room details
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoomDetails(@PathVariable String hostelId,@PathVariable String roomId) {
        try{
            RoomResponse roomResponse = roomService.getRoom(hostelId,roomId);
            return ResponseEntity.status(200).body(roomResponse);
        }catch (Exception e){
            return ResponseEntity.status(500).body("Error getting room from hostel: " + e.getMessage());
        }
    }
}
