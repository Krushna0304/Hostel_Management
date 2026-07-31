package com.krunity.HostelManagment.Mapper;

import com.krunity.HostelManagment.dto.TransactionHistoryResponse;
import com.krunity.HostelManagment.model.Transaction;
import com.krunity.HostelManagment.model.User;

import java.util.UUID;

public class TransactionMapper {

    public static TransactionHistoryResponse mapToResponse(Transaction tx, UUID currentUserId) {
        boolean isSender = tx.getFromUser().getUserId().equals(currentUserId);

        User counterparty = isSender ? tx.getToUser() : tx.getFromUser();

        return TransactionHistoryResponse.builder()
                .transactionId(tx.getTransactionId())
                .transactionRef(tx.getTransactionId())
                .direction(isSender ? "SENT" : "RECEIVED")
                .counterpartyName(counterparty.getDisplayName())
                .counterpartyRole(counterparty.getRole().getName())
                .amount(tx.getAmount())
                .mode(tx.getMode().name())
                .status(tx.getStatus().name())
                .reason(tx.getReason())
                .createdAt(tx.getCreatedAt())
                .confirmedAt(tx.getConfirmedAt())
                .build();
    }
}
