package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import org.springframework.stereotype.Service;

@Service
public class AllotmentService {

    public final RoomAllotmentRepository roomAllotmentRepository;

    public AllotmentService(RoomAllotmentRepository roomAllotmentRepository) {
        this.roomAllotmentRepository = roomAllotmentRepository;
    }


}
