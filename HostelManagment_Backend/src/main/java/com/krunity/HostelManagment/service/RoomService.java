package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.Mapper.RoomMapper;
import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.CreateRoomRequest;
import com.krunity.HostelManagment.dto.RoomResponse;
import com.krunity.HostelManagment.exception.AlreadyExistException;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.Floor;
import com.krunity.HostelManagment.model.Hostel;
import com.krunity.HostelManagment.model.Room;
import com.krunity.HostelManagment.repository.FloorRepository;
import com.krunity.HostelManagment.repository.HostelRepository;
import com.krunity.HostelManagment.repository.RoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final HostelRepository hostelRepository;
    private final FloorRepository floorRepository;
    public RoomService(HostelRepository hostelRepository,RoomRepository roomRepository,FloorRepository floorRepository) {
        this.roomRepository = roomRepository;
        this.floorRepository = floorRepository;
        this.hostelRepository = hostelRepository;
    }

    @Transactional
    public void createRoom(String hostelId, String floorId,CreateRoomRequest createRoomRequest) {
        UUID hostelUUID = UUID.fromString(hostelId);
        UUID floorUUID = UUID.fromString(floorId);
        UUID ownerId = ApplicationContext.getUser().getUserId();

        Hostel hostel = hostelRepository.findByHostelIdAndOwner_UserId(hostelUUID, ownerId);
        if (hostel == null) {
            throw new NotFoundException("Hostel not found or unauthorized");
        }

        Floor floor = floorRepository.findById(floorUUID)
                .orElseThrow(() -> new NotFoundException("Floor not found"));

        if(roomRepository.existsByRoomNumberAndFloor_FloorId(createRoomRequest.getRoomNumber(),floorUUID))
            throw new AlreadyExistException("Room Number already exists");

        floor.setTotalRooms(floor.getTotalRooms() + 1);
        Room room = RoomMapper.toEntity(createRoomRequest,hostel,floor);
        roomRepository.save(room);
    }

    public RoomResponse getRoom(String hostelId,String roomId) {
        UUID hostelIdUUID = UUID.fromString(hostelId);
        UUID ownerId = ApplicationContext.getUser().getUserId();

        Hostel hostel = hostelRepository.findByHostelIdAndOwner_UserId(hostelIdUUID, ownerId);
        if (hostel == null) {
            throw new NotFoundException("Hostel not found or unauthorized");
        }

        UUID uuid = UUID.fromString(roomId);
        Room room = roomRepository.findById(uuid).orElseThrow(() -> new NotFoundException("Room not found"));

        return RoomMapper.toDto(room);
    }

    public RoomResponse updateRoom(String hostelId,String roomId,CreateRoomRequest createRoomRequest) {
        UUID hostelIdUUID = UUID.fromString(hostelId);
        UUID ownerId = ApplicationContext.getUser().getUserId();

        Hostel hostel = hostelRepository.findByHostelIdAndOwner_UserId(hostelIdUUID, ownerId);
        if (hostel == null) {
            throw new NotFoundException("Hostel not found or unauthorized");
        }

        Room room = roomRepository.findById(UUID.fromString(roomId))
                .orElseThrow(() -> new NotFoundException("Room not found"));

        room.setRoomNumber(createRoomRequest.getRoomNumber());

        roomRepository.save(room);
        return RoomMapper.toDto(room);
    }

    public void deleteRoom(String hostelId,String roomId) {
        UUID hostelIdUUID = UUID.fromString(hostelId);
        UUID ownerId = ApplicationContext.getUser().getUserId();

        Hostel hostel = hostelRepository.findByHostelIdAndOwner_UserId(hostelIdUUID, ownerId);
        if (hostel == null) {
            throw new NotFoundException("Hostel not found or unauthorized");
        }


        UUID uuid = UUID.fromString(roomId);

        long deletedCount = roomRepository.deleteByRoomId(uuid);
        if (deletedCount == 0) {throw new NotFoundException("Room not found or unauthorized");}
    }


    public List<RoomResponse> getAllRooms(String hostelId,String floorId) {
        UUID hostelIdUUID = UUID.fromString(hostelId);
        UUID floorIdUUID = UUID.fromString(floorId);
        UUID ownerId = ApplicationContext.getUser().getUserId();

        Hostel hostel = hostelRepository.findByHostelIdAndOwner_UserId(hostelIdUUID, ownerId);
        if (hostel == null) {
            throw new NotFoundException("Hostel not found or unauthorized");
        }

        List<Room> rooms = roomRepository.findByHostel_HostelIdAndFloor_FloorId(hostelIdUUID,floorIdUUID);

        List<RoomResponse> roomResponses = rooms.stream()
                .map(RoomMapper::toDto)
                .toList();
        return roomResponses;
    }
    public List<RoomResponse> getAllActiveRooms(String hostelId,String floorId) {
        UUID hostelIdUUID = UUID.fromString(hostelId);
        UUID floorIdUUID = UUID.fromString(floorId);
        UUID ownerId = ApplicationContext.getUser().getUserId();

        Hostel hostel = hostelRepository.findByHostelIdAndOwner_UserId(hostelIdUUID, ownerId);
        if (hostel == null) {
            throw new NotFoundException("Hostel not found or unauthorized");
        }

        List<Room> rooms = roomRepository.findByHostel_HostelIdAndFloor_FloorIdAndIsActive(hostelIdUUID,floorIdUUID,true);

        List<RoomResponse> roomResponses = rooms.stream()
                .map(RoomMapper::toDto)
                .toList();
        return roomResponses;
    }
}
