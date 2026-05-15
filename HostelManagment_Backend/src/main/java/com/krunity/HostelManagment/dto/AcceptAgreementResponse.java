package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.AgreementStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class AcceptAgreementResponse {
    private String agreementId;
    private AgreementStatus status;
    private Instant activatedAt;
    // Returned so the frontend can call /api/password-reset/reset in the final step
    private String passwordResetToken;
}
