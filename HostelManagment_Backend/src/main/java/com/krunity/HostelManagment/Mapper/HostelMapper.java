package com.krunity.HostelManagment.Mapper;

import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.CreateHostelRequest;
import com.krunity.HostelManagment.dto.HostelResponse;
import com.krunity.HostelManagment.model.Hostel;

public class HostelMapper {

    public static Hostel toEntity(CreateHostelRequest createHostelRequest) {
            return Hostel.builder()
                    .hostelName(createHostelRequest.getHostelName())
                    .hostelAddress(createHostelRequest.getHostelAddress())
                    .owner(ApplicationContext.getUser())
                    .build();
    }

    public static HostelResponse toDto(Hostel hostel) {
        return HostelResponse.builder()
                .hostelId(hostel.getHostelId().toString())
                .hostelName(hostel.getHostelName())
                .hostelAddress(hostel.getHostelAddress())
                .ownerName(hostel.getOwner().getDisplayName())
                .build();
    }
}
