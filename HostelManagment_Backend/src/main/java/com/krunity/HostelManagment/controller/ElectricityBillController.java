package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.*;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.service.ElectricityBillService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/electricity")
public class ElectricityBillController {

    @Autowired
    private ElectricityBillService electricityBillService;

    // Electricity Account Management
    @PostMapping("/accounts")
    public ResponseEntity<ElectricityAccountDto> createElectricityAccount(
            @Valid @RequestBody CreateElectricityAccountRequest request) {
        User currentUser = ApplicationContext.getUser();
        ElectricityAccountDto account = electricityBillService.createElectricityAccount(request, currentUser.getUserId());
        return ResponseEntity.ok(account);
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<ElectricityAccountDto>> getOwnerAccounts() {
        User currentUser = ApplicationContext.getUser();
        List<ElectricityAccountDto> accounts = electricityBillService.getOwnerAccounts(currentUser.getUserId());
        return ResponseEntity.ok(accounts);
    }

    // Electricity Bill Management
    @PostMapping("/bills")
    public ResponseEntity<List<ElectricityBillDto>> createElectricityBills(
            @Valid @RequestBody CreateElectricityBillsRequest request) {
        User currentUser = ApplicationContext.getUser();
        List<ElectricityBillDto> bills = electricityBillService.createElectricityBills(request, currentUser.getUserId());
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/bills/owner")
    public ResponseEntity<List<ElectricityBillDto>> getOwnerBills() {
        User currentUser = ApplicationContext.getUser();
        List<ElectricityBillDto> bills = electricityBillService.getOwnerBills(currentUser.getUserId());
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/bills/tenant")
    public ResponseEntity<List<ElectricityBillDto>> getTenantBills() {
        User currentUser = ApplicationContext.getUser();
        List<ElectricityBillDto> bills = electricityBillService.getTenantBills(currentUser.getUserId());
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/bills/{billId}")
    public ResponseEntity<ElectricityBillDto> getBillDetails(@PathVariable UUID billId) {
        User currentUser = ApplicationContext.getUser();
        ElectricityBillDto bill = electricityBillService.getBillDetails(billId, currentUser.getUserId());
        return ResponseEntity.ok(bill);
    }

    // Payment Management
    @PostMapping("/payments")
    public ResponseEntity<ElectricityPaymentDto> recordPayment(
            @Valid @RequestBody ElectricityPaymentRequest request) {
        User currentUser = ApplicationContext.getUser();
        ElectricityPaymentDto payment = electricityBillService.recordPayment(request, currentUser.getUserId());
        return ResponseEntity.ok(payment);
    }
}