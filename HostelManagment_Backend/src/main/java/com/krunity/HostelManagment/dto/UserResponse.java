package com.krunity.HostelManagment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID userId;
    private String displayName;
    private String username;
    private String phoneNumber;
    private String role;
    private boolean isActive;
}
