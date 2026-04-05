package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.TransactionMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AcceptAgreementRequest {
    @NotNull(message = "Payment mode is required")
    private TransactionMode paymentMode;

    private String otp; // For cash payments
}

