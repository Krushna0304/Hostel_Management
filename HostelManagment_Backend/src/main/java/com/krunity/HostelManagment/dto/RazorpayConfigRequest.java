package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RazorpayConfigRequest {
    
    @NotBlank(message = "Razorpay Key ID is required")
    @Pattern(regexp = "^rzp_(test|live)_[A-Za-z0-9]{14}$", 
             message = "Invalid Razorpay Key ID format")
    private String keyId;
    
    @NotBlank(message = "Razorpay Key Secret is required")
    private String keySecret;
}
