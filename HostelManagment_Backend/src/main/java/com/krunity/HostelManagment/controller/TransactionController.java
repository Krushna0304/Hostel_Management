package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.TransactionHistoryResponse;
import com.krunity.HostelManagment.model.Transaction;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * TransactionController
 *
 * Provides payment history for the currently authenticated user (Owner or Tenant).
 * Returns every transaction where the user is either the sender (SENT) or receiver (RECEIVED).
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * GET /api/transactions/my-history
     *
     * Returns all transactions involving the current user, newest first.
     * Works for both OWNER and TENANT roles.
     *
     * Each entry includes:
     *  - direction: SENT (user paid) | RECEIVED (user was paid)
     *  - counterpartyName / counterpartyRole: the other party
     *  - amount, mode (CASH/ONLINE), status, reason, timestamps
     */
    @GetMapping("/my-history")
    public ResponseEntity<?> getMyHistory() {
        try {
            User currentUser = ApplicationContext.getUser();
            UUID userId = currentUser.getUserId();

            List<Transaction> transactions = transactionRepository.findAllByUserId(userId);

            List<TransactionHistoryResponse> response = transactions.stream()
                    .map(tx -> mapToResponse(tx, userId))
                    .toList();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // ── mapper ────────────────────────────────────────────────────────────────

    private TransactionHistoryResponse mapToResponse(Transaction tx, UUID currentUserId) {
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
