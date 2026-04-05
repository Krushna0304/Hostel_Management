package com.krunity.HostelManagment.dto;

import lombok.Data;

@Data
public class QrCodeResponse {
    private String qrCodeBase64;
    private String activationUrl;
    private String qrToken;
}

