package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.Mapper.RoomAllotmentMapper;
import com.krunity.HostelManagment.Mapper.RoomMapper;
import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.CreateRoomRequest;
import com.krunity.HostelManagment.dto.RoomResponse;
import com.krunity.HostelManagment.dto.RoomTenantResponse;
import com.krunity.HostelManagment.enums.AgreementStatus;
import com.krunity.HostelManagment.enums.RoomType;
import com.krunity.HostelManagment.exception.AlreadyExistException;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;
import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomAllotmentRepository roomAllotmentRepository;
    private final HostelRepository hostelRepository;
    private final FloorRepository floorRepository;
    private final AgreementRepository agreementRepository;
    private final UserRepository userRepository;
    
    public RoomService(HostelRepository hostelRepository, RoomRepository roomRepository, FloorRepository floorRepository, 
                      RoomAllotmentRepository roomAllotmentRepository, AgreementRepository agreementRepository, 
                      UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.floorRepository = floorRepository;
        this.hostelRepository = hostelRepository;
        this.roomAllotmentRepository = roomAllotmentRepository;
        this.agreementRepository = agreementRepository;
        this.userRepository = userRepository;
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

    /**
     * Returns active rooms filtered by roomType.
     * <ul>
     *   <li>{@code roomType=PG_ROOM} — rooms where room_type = 'PG_ROOM' OR room_type IS NULL (backward compat)</li>
     *   <li>{@code roomType=FLAT}    — rooms where room_type = 'FLAT'</li>
     *   <li>{@code roomType=null}    — all active rooms (existing behaviour)</li>
     * </ul>
     *
     * @throws IllegalArgumentException if roomType is not a valid {@link RoomType} value
     */
    public List<RoomResponse> getAllActiveRooms(String hostelId, String floorId, String roomType) {
        if (roomType == null) {
            return getAllActiveRooms(hostelId, floorId);
        }

        RoomType roomTypeEnum;
        try {
            roomTypeEnum = RoomType.valueOf(roomType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid roomType filter value. Allowed: PG_ROOM, FLAT");
        }

        UUID hostelIdUUID = UUID.fromString(hostelId);
        UUID floorIdUUID = UUID.fromString(floorId);
        UUID ownerId = ApplicationContext.getUser().getUserId();

        Hostel hostel = hostelRepository.findByHostelIdAndOwner_UserId(hostelIdUUID, ownerId);
        if (hostel == null) {
            throw new NotFoundException("Hostel not found or unauthorized");
        }

        List<Room> rooms;
        if (roomTypeEnum == RoomType.PG_ROOM) {
            // Include rooms with room_type = 'PG_ROOM' OR room_type IS NULL (req 8.1)
            rooms = roomRepository.findActiveRoomsByHostelAndFloorAndRoomTypeOrNull(hostelIdUUID, floorIdUUID, RoomType.PG_ROOM);
        } else {
            // FLAT: exact match only
            rooms = roomRepository.findActiveRoomsByHostelAndFloorAndRoomType(hostelIdUUID, floorIdUUID, RoomType.FLAT);
        }

        return rooms.stream()
                .map(RoomMapper::toDto)
                .toList();
    }

    public List<RoomTenantResponse> getTenantList(String hostelId, String roomId) {
        UUID hostelIdUUID = UUID.fromString(hostelId);
        UUID roomIdUUID = UUID.fromString(roomId);
        UUID ownerId = ApplicationContext.getUser().getUserId();

        Hostel hostel = hostelRepository.findByHostelIdAndOwner_UserId(hostelIdUUID, ownerId);
        if (hostel == null) {
            throw new NotFoundException("Hostel not found or unauthorized");
        }

        Room room = roomRepository.findById(roomIdUUID)
                .orElseThrow(() -> new NotFoundException("Room not found"));
        
        // Verify room belongs to the hostel
        if (!room.getHostel().getHostelId().equals(hostelIdUUID)) {
            throw new NotFoundException("Room does not belong to the specified hostel");
        }

        List<RoomTenantResponse> tenantResponses = new ArrayList<>();

        if (room.getRoomType() == RoomType.FLAT) {
            // For FLAT rooms, get data from Agreement table
            List<Agreement> agreements = agreementRepository.findByRoomIdAndStatusIn(
                    roomIdUUID, 
                    List.of(AgreementStatus.ACTIVE, AgreementStatus.PENDING_TENANT_ACTION)
            );
            
            for (Agreement agreement : agreements) {
                User tenant = userRepository.findById(agreement.getUserId()).orElse(null);
                if (tenant != null) {
                    tenantResponses.add(RoomTenantResponse.builder()
                            .roomId(roomId)
                            .tenantId(agreement.getUserId().toString())
                            .tenantName(tenant.getDisplayName())
                            .allotmentDate(agreement.getStartDate() != null ? 
                                    java.sql.Date.valueOf(agreement.getStartDate()) : null)
                            .roomAllotmentStatus(agreement.getStatus().name())
                            .coTenantNames(agreement.getCoTenantNames())
                            .agreementType("FLAT")
                            .build());
                }
            }
        } else {
            // For PG_ROOM, get data from RoomAllotment table (existing logic)
            List<RoomAllotment> roomAllotments = roomAllotmentRepository.findByRoom(room);
            tenantResponses = roomAllotments.stream()
                    .map(allotment -> {
                        RoomTenantResponse response = RoomAllotmentMapper.mapToRoomTenantResponse(allotment);
                        response.setAgreementType("ROOM");
                        return response;
                    })
                    .toList();
        }

        return tenantResponses;
    }

    /**
     * Get all rooms for a specific hostel
     */
    public List<RoomResponse> getRoomsByHostel(UUID hostelId) {
        UUID ownerId = ApplicationContext.getUser().getUserId();

        Hostel hostel = hostelRepository.findByHostelIdAndOwner_UserId(hostelId, ownerId);
        if (hostel == null) {
            throw new NotFoundException("Hostel not found or unauthorized");
        }

        List<Room> rooms = roomRepository.findByHostel_HostelId(hostelId);

        return rooms.stream()
                .map(RoomMapper::toDto)
                .toList();
    }
}
