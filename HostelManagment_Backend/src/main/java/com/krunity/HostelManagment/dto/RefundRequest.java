package com.krunity.HostelManagment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundRequest {
    private String paymentId;   // Provider payment ID to refund
    private long amount;        // Amount to refund in smallest unit (0 = full refund)
    private String reason;
}
