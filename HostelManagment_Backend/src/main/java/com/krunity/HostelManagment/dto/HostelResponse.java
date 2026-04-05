package com.krunity.HostelManagment.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class HostelResponse {
    private String hostelId;
    private String hostelName;
    private String hostelAddress;
    private String ownerName;
//    private String ownerEmail;
//    private String ownerPhone;
//    private String hostelEmail;
//    private String hostelPhone;
//    private String hostelPassword;
//    private String hostelStatus;
//    private String hostelType;

}
