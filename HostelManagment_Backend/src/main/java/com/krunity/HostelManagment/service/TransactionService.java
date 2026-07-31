package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.Mapper.TransactionMapper;
import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.TransactionHistoryResponse;
import com.krunity.HostelManagment.model.Transaction;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public List<TransactionHistoryResponse>  getMyHistory() throws Exception{

            User currentUser = ApplicationContext.getUser();
            UUID userId = currentUser.getUserId();

            List<Transaction> transactions = transactionRepository.findAllByUserId(userId);

            List<TransactionHistoryResponse> response = transactions.stream()
                    .map(tx -> TransactionMapper.mapToResponse(tx, userId))
                    .toList();

            return response;
    }
}
