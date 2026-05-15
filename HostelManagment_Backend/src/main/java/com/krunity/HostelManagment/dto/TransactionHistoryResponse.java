package com.krunity.HostelManagment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TransactionHistoryResponse {

    private UUID transactionId;

    /** SENT = current user paid someone | RECEIVED = current user received money */
    private String direction;

    /** The other party's display name */
    private String counterpartyName;

    /** The other party's role (OWNER / TENANT) */
    private String counterpartyRole;

    private Long amount;

    /** CASH or ONLINE */
    private String mode;

    /** COMPLETED, FAILED, PENDING … */
    private String status;

    /** Human-readable description, e.g. "Installment #2 payment" */
    private String reason;

    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;

    private UUID transactionRef;   // same as transactionId, kept for UI convenience
}
