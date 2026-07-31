package com.krunity.HostelManagment.dto;

import lombok.Data;

@Data
public class SettlementApprovalRequest {
    
    private String ownerNotes;
    
    private boolean updateRoomAvailability = true;
}