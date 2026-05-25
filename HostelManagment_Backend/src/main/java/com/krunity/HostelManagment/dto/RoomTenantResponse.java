package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.model.Room;
import lombok.Builder;
import lombok.Data;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RoomTenantResponse {
    public  String roomId;
    public  String tenantId;
    public  String tenantName;
    public  String phoneNumber;
    public  String planName;
    private Date allotmentDate;
    private LocalDate agreementEndDate; // For FLAT agreements
    private String roomAllotmentStatus;
    
    // For FLAT rooms - co-tenant information
    private List<String> coTenantNames;
    private String agreementType; // "FLAT" or "ROOM"
}

//@Data
//@Builder
//class RoomTenantDetailedResponse extends RoomTenantResponse {
//    private Long tenantId;
//    private String tenantName;
//    private String tenantEmail;
//    private String tenantPhone;
//    private String roomNumber;
//    private String hostelName;
//    private String allotmentStatus;
//}
