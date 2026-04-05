package com.krunity.HostelManagment.dto;

import lombok.Data;

@Data
public class LoginRequest {
    String username;
    String password;
    String role;
}
