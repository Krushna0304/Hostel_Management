package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.dto.RoomAvailabilityResponse;
import com.krunity.HostelManagment.enums.RoomType;
import com.krunity.HostelManagment.service.RoomAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomAvailabilityController {

    private final RoomAvailabilityService roomAvailabilityService;

    @GetMapping("/available")
    public ResponseEntity<List<RoomAvailabilityResponse>> getAvailableRooms(
            @RequestParam UUID floorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String roomType) {

        RoomType parsedRoomType = null;
        if (roomType != null && !roomType.isBlank()) {
            try {
                parsedRoomType = RoomType.valueOf(roomType.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Invalid roomType. Allowed values: PG_ROOM, FLAT");
            }
        }

        List<RoomAvailabilityResponse> rooms = roomAvailabilityService.getAvailableRooms(floorId, startDate, endDate , parsedRoomType);
        return ResponseEntity.ok(rooms);
    }
}
