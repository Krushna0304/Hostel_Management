package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.Mapper.FloorMapper;
import com.krunity.HostelManagment.Mapper.HostelMapper;
import com.krunity.HostelManagment.Mapper.RoomMapper;
import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.*;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.exception.UnauthorizedException;
import com.krunity.HostelManagment.model.Floor;
import com.krunity.HostelManagment.model.Hostel;
import com.krunity.HostelManagment.model.Room;
import com.krunity.HostelManagment.repository.FloorRepository;
import com.krunity.HostelManagment.repository.HostelRepository;
import com.krunity.HostelManagment.repository.RoomRepository;
import org.apache.catalina.Host;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.List;

@Service
public class HostelService {

    private final HostelRepository hostelRepository;

    private final FloorRepository floorRepository;
    private final RoomRepository roomRepository;


    public HostelService(HostelRepository hostelRepository, FloorRepository floorRepository,RoomRepository roomRepository ) {
        this.hostelRepository = hostelRepository;
        this.floorRepository = floorRepository;
        this.roomRepository = roomRepository;
    }
    public void createHostel(CreateHostelRequest createHostelRequest) {
        Hostel hostel = HostelMapper.toEntity(createHostelRequest);
        hostelRepository.save(hostel);
    }

    public void deleteHostel(String hostelId) {
        UUID uuid = UUID.fromString(hostelId);
        UUID userId = ApplicationContext.getUser().getUserId();

        long deletedCount = hostelRepository.deleteByHostelIdAndOwner_UserId(
                uuid,
                userId
        );
        if (deletedCount == 0) {throw new NotFoundException("Hostel not found or unauthorized");}
    }


    public HostelResponse updateHostel(String hostelId,CreateHostelRequest createHostelRequest) {

        UUID uuid = UUID.fromString(hostelId);
        UUID userId = ApplicationContext.getUser().getUserId();

        Hostel hostel = hostelRepository.findByHostelIdAndOwner_UserId(uuid,userId);

        hostel.setHostelName(createHostelRequest.getHostelName());
        hostel.setHostelAddress(createHostelRequest.getHostelAddress());
        hostel.setOwner(ApplicationContext.getUser());

        hostelRepository.save(hostel);
        return HostelMapper.toDto(hostel);
    }

    public HostelResponse getHostel(String hostelId) {
        UUID uuid = UUID.fromString(hostelId);
        UUID userId = ApplicationContext.getUser().getUserId();

        Hostel hostel = hostelRepository.findByHostelIdAndOwner_UserId(uuid,userId);
        if(hostel == null) {
            throw new NotFoundException("Hostel not found");
        }
        return HostelMapper.toDto(hostel);
    }

    public List<HostelResponse> getAllHostelsForCurrentUser() {
        UUID userId = ApplicationContext.getUser().getUserId();

        List<Hostel> hostel = hostelRepository.findByOwner_UserId(userId);
        if(hostel == null) {
            throw new NotFoundException("Hostel not found");
        }

        List <HostelResponse> hostelResponses = hostel.stream()
                .map(HostelMapper::toDto)
                .toList();
        return hostelResponses;
    }

}
