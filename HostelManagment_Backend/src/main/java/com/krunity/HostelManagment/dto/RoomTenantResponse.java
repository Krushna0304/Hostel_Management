package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.model.Room;
import lombok.Builder;
import lombok.Data;

import java.sql.Date;
import java.util.List;

@Data
@Builder
public class RoomTenantResponse {
    public  String roomId;
    public  String tenantId;
    public  String tenantName;
//    public  String tenantPlan;
    private Date allotmentDate;
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
