package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.*;
import com.krunity.HostelManagment.enums.BillStatus;
import com.krunity.HostelManagment.exception.ConflictException;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ElectricityBillService {

    @Autowired
    private ElectricityAccountRepository accountRepository;

    @Autowired
    private ElectricityBillRepository billRepository;

    @Autowired
    private ElectricityPaymentRepository paymentRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private AgreementRepository agreementRepository;

    @Autowired
    private CashPaymentOtpService cashPaymentOtpService;

    // Electricity Account Management
    @Transactional
    public ElectricityAccountDto createElectricityAccount(CreateElectricityAccountRequest request, UUID ownerId) {
        log.info("Creating electricity account for room: {} by owner: {}", request.getRoomId(), ownerId);

        // Validate room exists and belongs to owner
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new NotFoundException("Room not found"));

        if (!room.getHostel().getOwner().getUserId().equals(ownerId)) {
            throw new ConflictException("You can only create accounts for your own rooms");
        }

        // Check if account already exists for this room
        if (accountRepository.existsByRoomIdAndIsActiveTrue(request.getRoomId())) {
            throw new ConflictException("Electricity account already exists for this room");
        }

        // Check if account number is unique
        if (accountRepository.existsByAccountNumberAndIsActiveTrue(request.getAccountNumber())) {
            throw new ConflictException("Account number already exists");
        }

        ElectricityAccount account = ElectricityAccount.builder()
                .roomId(request.getRoomId())
                .accountNumber(request.getAccountNumber())
                .ownerId(ownerId)
                .isActive(true)
                .build();

        account = accountRepository.save(account);

        return mapToAccountDto(account, room);
    }

    @Transactional(readOnly = true)
    public List<ElectricityAccountDto> getOwnerAccounts(UUID ownerId) {
        List<ElectricityAccount> accounts = accountRepository.findByOwnerIdWithRoomDetails(ownerId);
        return accounts.stream()
                .map(account -> mapToAccountDto(account, account.getRoom()))
                .collect(Collectors.toList());
    }

    // Electricity Bill Management
    @Transactional
    public List<ElectricityBillDto> createElectricityBills(CreateElectricityBillsRequest request, UUID ownerId) {
        log.info("Creating electricity bills for month: {}/{} by owner: {}", request.getBillMonth(), request.getBillYear(), ownerId);

        List<ElectricityBill> createdBills = new ArrayList<>();

        for (CreateElectricityBillsRequest.BillItem billItem : request.getBills()) {
            // Validate account belongs to owner
            ElectricityAccount account = accountRepository.findById(billItem.getAccountId())
                    .orElseThrow(() -> new NotFoundException("Electricity account not found"));

            if (!account.getOwnerId().equals(ownerId)) {
                throw new ConflictException("You can only create bills for your own accounts");
            }

            // Check if bill already exists for this period
            if (billRepository.findByAccountIdAndBillMonthAndBillYear(
                    billItem.getAccountId(), request.getBillMonth(), request.getBillYear()).isPresent()) {
                throw new ConflictException("Bill already exists for account " + account.getAccountNumber() + 
                        " for " + Month.of(request.getBillMonth()).name() + " " + request.getBillYear());
            }

            // Get current tenant for the room
            UUID tenantId = getCurrentTenantForRoom(account.getRoomId());

            ElectricityBill bill = ElectricityBill.builder()
                    .accountId(billItem.getAccountId())
                    .roomId(account.getRoomId())
                    .ownerId(ownerId)
                    .tenantId(tenantId)
                    .billMonth(request.getBillMonth())
                    .billYear(request.getBillYear())
                    .totalAmount(billItem.getAmount())
                    .paidAmount(BigDecimal.ZERO)
                    .remainingAmount(billItem.getAmount())
                    .status(BillStatus.PENDING)
                    .notes(billItem.getNotes())
                    .dueDate(LocalDateTime.now().plusDays(30)) // 30 days from creation
                    .build();

            createdBills.add(billRepository.save(bill));
        }

        return createdBills.stream()
                .map(this::mapToBillDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ElectricityBillDto> getOwnerBills(UUID ownerId) {
        List<ElectricityBill> bills = billRepository.findByOwnerIdWithAccountAndRoomDetails(ownerId);
        return bills.stream()
                .map(this::mapToBillDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ElectricityBillDto> getTenantBills(UUID tenantId) {
        List<ElectricityBill> bills = billRepository.findByTenantIdWithAccountAndRoomDetails(tenantId);
        return bills.stream()
                .map(this::mapToBillDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ElectricityBillDto getBillDetails(UUID billId, UUID userId) {
        ElectricityBill bill = billRepository.findById(billId)
                .orElseThrow(() -> new NotFoundException("Bill not found"));

        // Check if user has access to this bill
        if (!bill.getOwnerId().equals(userId) && !bill.getTenantId().equals(userId)) {
            throw new ConflictException("You don't have access to this bill");
        }

        ElectricityBillDto billDto = mapToBillDto(bill);

        // Add tenant phone number
        if (bill.getTenant() != null) {
            billDto.setTenantPhone(bill.getTenant().getPhoneNumber());
        }

        // Calculate total remaining amount for this room/account (all pending bills)
        BigDecimal totalRemaining = billRepository.sumRemainingAmountByAccountId(bill.getAccountId());
        billDto.setTotalRemainingForRoom(totalRemaining);
        
        // Add payment history
        List<ElectricityPayment> payments = paymentRepository.findByBillIdWithTenantDetails(billId);
        billDto.setPayments(payments.stream()
                .map(this::mapToPaymentDto)
                .collect(Collectors.toList()));

        return billDto;
    }

    // Payment Management
    @Transactional
    public ElectricityPaymentDto recordPayment(ElectricityPaymentRequest request, UUID tenantId) {
        log.info("Recording electricity payment for bill: {} by tenant: {}", request.getBillId(), tenantId);

        ElectricityBill bill = billRepository.findById(request.getBillId())
                .orElseThrow(() -> new NotFoundException("Bill not found"));

        // Validate tenant has access to this bill
        if (!bill.getTenantId().equals(tenantId)) {
            throw new ConflictException("You can only pay your own bills");
        }

        // Validate payment amount doesn't exceed remaining amount
        if (request.getAmount().compareTo(bill.getRemainingAmount()) > 0) {
            throw new ConflictException("Payment amount cannot exceed remaining amount");
        }

        // Verify OTP for cash payments
        if (request.getPaymentMode() == com.krunity.HostelManagment.enums.TransactionMode.CASH) {
            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                throw new ConflictException("OTP is required for cash payments");
            }
            boolean otpValid = cashPaymentOtpService.verifyElectricityOtp(request.getBillId(), request.getOtp().trim());
            if (!otpValid) {
                throw new ConflictException("Invalid or expired OTP. Please request a new OTP.");
            }
        }

        // Create payment record
        ElectricityPayment payment = ElectricityPayment.builder()
                .billId(request.getBillId())
                .tenantId(tenantId)
                .amount(request.getAmount())
                .paymentMode(request.getPaymentMode())
                .paymentReference(request.getPaymentReference())
                .razorpayOrderId(request.getRazorpayOrderId())
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .notes(request.getNotes())
                .build();

        payment = paymentRepository.save(payment);

        // Update bill amounts and status
        bill.setPaidAmount(bill.getPaidAmount().add(request.getAmount()));
        bill.updateRemainingAmount();
        billRepository.save(bill);

        log.info("Payment recorded successfully. Bill status: {}, Remaining: {}", 
                bill.getStatus(), bill.getRemainingAmount());

        return mapToPaymentDto(payment);
    }

    // Helper methods
    private UUID getCurrentTenantForRoom(UUID roomId) {
        return agreementRepository.findActiveAgreementByRoomId(roomId)
                .map(Agreement::getUserId)
                .orElse(null);
    }

    private ElectricityAccountDto mapToAccountDto(ElectricityAccount account, Room room) {
        return ElectricityAccountDto.builder()
                .accountId(account.getAccountId())
                .roomId(account.getRoomId())
                .roomNumber(room != null ? room.getRoomNumber() : "N/A")
                .hostelId(room != null && room.getHostel() != null ? 
                        room.getHostel().getHostelId() : null)
                .hostelName(room != null && room.getHostel() != null ? 
                        room.getHostel().getHostelName() : "Unknown")
                .accountNumber(account.getAccountNumber())
                .ownerId(account.getOwnerId())
                .isActive(account.getIsActive())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private ElectricityBillDto mapToBillDto(ElectricityBill bill) {
        String billPeriod = Month.of(bill.getBillMonth()).name() + " " + bill.getBillYear();
        
        return ElectricityBillDto.builder()
                .billId(bill.getBillId())
                .accountId(bill.getAccountId())
                .accountNumber(bill.getElectricityAccount() != null ? 
                        bill.getElectricityAccount().getAccountNumber() : "N/A")
                .roomId(bill.getRoomId())
                .roomNumber(bill.getRoom() != null ? bill.getRoom().getRoomNumber() : "N/A")
                .hostelId(bill.getRoom() != null && bill.getRoom().getHostel() != null ? 
                        bill.getRoom().getHostel().getHostelId() : null)
                .hostelName(bill.getRoom() != null && bill.getRoom().getHostel() != null ? 
                        bill.getRoom().getHostel().getHostelName() : "Unknown")
                .ownerId(bill.getOwnerId())
                .tenantId(bill.getTenantId())
                .tenantName(bill.getTenant() != null ? bill.getTenant().getDisplayName() : "No Tenant")
                .billMonth(bill.getBillMonth())
                .billYear(bill.getBillYear())
                .billPeriod(billPeriod)
                .totalAmount(bill.getTotalAmount())
                .paidAmount(bill.getPaidAmount())
                .remainingAmount(bill.getRemainingAmount())
                .status(bill.getStatus())
                .dueDate(bill.getDueDate())
                .notes(bill.getNotes())
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt())
                .build();
    }

    private ElectricityPaymentDto mapToPaymentDto(ElectricityPayment payment) {
        return ElectricityPaymentDto.builder()
                .paymentId(payment.getPaymentId())
                .billId(payment.getBillId())
                .tenantId(payment.getTenantId())
                .tenantName(payment.getTenant() != null ? payment.getTenant().getDisplayName() : "N/A")
                .amount(payment.getAmount())
                .paymentMode(payment.getPaymentMode())
                .status(payment.getStatus())
                .paymentReference(payment.getPaymentReference())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}