package com.krunity.HostelManagment.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class QrActivationResponse {
    private String agreementId;
    private String qrToken;
    private Instant expiry;
}

