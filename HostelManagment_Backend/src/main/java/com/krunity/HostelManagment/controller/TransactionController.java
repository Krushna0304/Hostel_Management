package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.dto.TransactionHistoryResponse;
import com.krunity.HostelManagment.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/my-history")
    public ResponseEntity<?> getMyHistory() {
        try{
            List<TransactionHistoryResponse> response = transactionService.getMyHistory();
            return ResponseEntity.ok(response);
        }catch(Exception ex){
            return ResponseEntity.status(500).body(ex.getMessage());
        }
    }
}
