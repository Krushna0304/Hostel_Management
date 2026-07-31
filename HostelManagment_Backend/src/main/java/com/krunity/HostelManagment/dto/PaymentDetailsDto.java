package com.krunity.HostelManagment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentDetailsDto {
    
    @NotNull(message = "Payment amount is required")
    private BigDecimal paymentAmount;
    
    @NotBlank(message = "Payment reference is required")
    private String paymentReference;
    
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
    
    private String transactionId;
}