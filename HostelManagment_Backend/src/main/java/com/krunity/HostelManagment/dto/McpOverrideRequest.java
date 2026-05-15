package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class McpOverrideRequest {
    
    @NotNull(message = "Disabled status is required")
    private Boolean disabled;
    
    @NotBlank(message = "Reason is required")
    private String reason;
}
