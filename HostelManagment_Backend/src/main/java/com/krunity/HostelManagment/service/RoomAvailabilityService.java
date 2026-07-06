package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.RoomAvailabilityResponse;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.enums.RoomType;
import com.krunity.HostelManagment.exception.ConflictException;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.Room;
import com.krunity.HostelManagment.repository.FloorRepository;
import com.krunity.HostelManagment.repository.RoomAvailabilityRepository;
import com.krunity.HostelManagment.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomAvailabilityService {

    private final RoomAvailabilityRepository roomAvailabilityRepository;
    private final RoomRepository roomRepository;
    private final FloorRepository floorRepository;

    @Transactional(readOnly = true)
    public List<RoomAvailabilityResponse> getAvailableRooms(UUID floorId, LocalDate startDate, LocalDate endDate, RoomType roomType) {
        validateInputs(floorId, startDate);
        if (endDate == null) {
            endDate = startDate;
        }
        floorRepository.findById(floorId)
                .orElseThrow(() -> new NotFoundException("Floor not found with ID: " + floorId));

        return roomAvailabilityRepository.findAvailableRoomsByFloor(
                floorId,
                startDate,
                endDate,
                RoomAllotmentStatus.occupyingStatuses(),
                roomType);
    }

//    @Transactional(readOnly = true)
//    public List<RoomAvailabilityResponse> getAvailableRooms(UUID floorId, LocalDate startDate,LocalDate endDate, RoomType roomType) {
//        return getAvailableRooms(floorId, startDate, endDate, roomType);
//    }

    @Transactional(readOnly = true)
    public int getAvailableBeds(UUID roomId, LocalDate startDate, LocalDate endDate) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room not found with ID: " + roomId));

        if (endDate == null) {
            endDate = startDate != null ? startDate : LocalDate.now();
        }
        if (startDate == null) {
            startDate = LocalDate.now();
        }

        long occupied = roomAvailabilityRepository.countOccupiedBeds(
                roomId,
                startDate,
                endDate,
                RoomAllotmentStatus.occupyingStatuses());

        return room.getTotalBeds() - (int) occupied;
    }

    @Transactional(readOnly = true)
    public int getAvailableBeds(UUID roomId, LocalDate selectedDate) {
        return getAvailableBeds(roomId, selectedDate, null);
    }

    @Transactional(readOnly = true)
    public void validateRoomHasBeds(UUID roomId, LocalDate startDate, LocalDate endDate, int bedsRequired) {
        int available = getAvailableBeds(roomId, startDate, endDate);
        if (available < bedsRequired) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new NotFoundException("Room not found with ID: " + roomId));
            throw new ConflictException(String.format(
                    "Room %s has only %d bed(s) available from %s to %s, but %d required.",
                    room.getRoomNumber(), available, startDate, endDate, bedsRequired));
        }
    }

    @Transactional(readOnly = true)
    public void validateRoomHasBeds(UUID roomId, LocalDate startDate, int bedsRequired) {
        validateRoomHasBeds(roomId, startDate, null, bedsRequired);
    }

    private void validateInputs(UUID floorId, LocalDate startDate) {
        if (floorId == null) {
            throw new IllegalArgumentException("floorId is required");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
    }
}
