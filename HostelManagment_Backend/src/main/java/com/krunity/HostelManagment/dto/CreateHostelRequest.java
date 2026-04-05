package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateHostelRequest {

    @NotBlank(message = "Hostel name is required")
    @Size(max = 100, message = "Hostel name must not exceed 100 characters")
    private String hostelName;

    @NotBlank(message = "Hostel address is required")
    @Size(min = 10, message = "Hostel address must contain at least 10 characters")
    private String hostelAddress;
}

