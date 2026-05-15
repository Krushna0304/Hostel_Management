package com.krunity.HostelManagment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundResponse {
    private String refundId;
    private String paymentId;
    private long amount;
    private String status;
}
