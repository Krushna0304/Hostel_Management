package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.Mapper.FloorMapper;
import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.CreateFloorRequest;
import com.krunity.HostelManagment.dto.FloorResponse;
import com.krunity.HostelManagment.exception.AlreadyExistException;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.exception.UnauthorizedException;
import com.krunity.HostelManagment.model.Floor;
import com.krunity.HostelManagment.model.Hostel;
import com.krunity.HostelManagment.repository.FloorRepository;
import com.krunity.HostelManagment.repository.HostelRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FloorService {

    public final FloorRepository floorRepository;
    public final HostelRepository hostelRepository;

    public FloorService(FloorRepository floorRepository,HostelRepository hostelRepository) {
        this.floorRepository = floorRepository;
        this.hostelRepository = hostelRepository;
    }

    //create Floor
    public void createFloor(String hostelId,CreateFloorRequest createFloorRequest) {
        UUID uuid = UUID.fromString(hostelId);
        UUID ownerId = ApplicationContext.getUser().getUserId();

        Hostel hostel = hostelRepository.findByHostelIdAndOwner_UserId(uuid, ownerId);
        if (hostel == null) {
            throw new NotFoundException("Hostel not found or unauthorized");
        }

        if(floorRepository.existsByFloorNumberAndHostel_HostelId(createFloorRequest.getFloorNumber(), uuid)) {
            throw new AlreadyExistException("Floor Number already exists");
        }
        Floor floor = FloorMapper.toEntity(createFloorRequest, hostel);
        floorRepository.save(floor);
    }

    //get Floor
    public FloorResponse getFloor(String hostelId,String floorId) {
        UUID uuid = UUID.fromString(floorId);
        UUID ownerId = ApplicationContext.getUser().getUserId();

        Floor floor = floorRepository.findByFloorIdAndHostel_Owner_UserId(uuid,ownerId);
        if(floor == null) {
            throw new UnauthorizedException("Unauthorized to access this floor");
        }

        return FloorMapper.toDto(floor);
    }

    //update Floor
    public FloorResponse updateFloor(String floorId,CreateFloorRequest createFloorRequest) {
        UUID uuid = UUID.fromString(floorId);
        UUID ownerId = ApplicationContext.getUser().getUserId();

        Floor floor = floorRepository.findByFloorIdAndHostel_Owner_UserId(uuid,ownerId);
        if(floor == null) {
            throw new UnauthorizedException("Unauthorized to access this floor");
        }

        floor.setFloorNumber(createFloorRequest.getFloorNumber());

        floorRepository.save(floor);
        return FloorMapper.toDto(floor);
    }

    //delete Floor
    public void deleteFloor(String floorId) {
        UUID uuid = UUID.fromString(floorId);
        UUID ownerId = ApplicationContext.getUser().getUserId();

        long deletedCount = floorRepository.deleteByFloorIdAndHostel_Owner_UserId(uuid,ownerId);
        if (deletedCount == 0) {throw new NotFoundException("Floor not found or unauthorized");}
    }

    public Iterable<FloorResponse> getAllFloorsInHostel(String hostelId) {
        UUID uuid = UUID.fromString(hostelId);
        UUID ownerId = ApplicationContext.getUser().getUserId();

        Hostel hostel = hostelRepository.findByHostelIdAndOwner_UserId(uuid, ownerId);
        if (hostel == null) {
            throw new NotFoundException("Hostel not found or unauthorized");
        }

        var floors = floorRepository.findAllByHostel_HostelIdAndHostel_Owner_UserId(uuid, ownerId);
        return floors.stream().map(FloorMapper::toDto).toList();
    }

}
