package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.*;
import com.krunity.HostelManagment.enums.BillStatus;
import com.krunity.HostelManagment.enums.PaymentStatus;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.enums.TransactionMode;
import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.exception.ConflictException;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
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
    private RoomAllotmentRepository roomAllotmentRepository;

    // A tenant is billed for a month only if they have been allotted the room for
    // at least this many days as of the bill creation date.
    private static final long MIN_OCCUPANCY_DAYS = 15;

    @Autowired
    private CashPaymentOtpService cashPaymentOtpService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

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

            ElectricityBill bill = ElectricityBill.builder()
                    .accountId(billItem.getAccountId())
                    .roomId(account.getRoomId())
                    .ownerId(ownerId)
                    .billMonth(request.getBillMonth())
                    .billYear(request.getBillYear())
                    .totalAmount(billItem.getAmount())
                    .paidAmount(BigDecimal.ZERO)
                    .remainingAmount(billItem.getAmount())
                    .status(BillStatus.PENDING)
                    .notes(billItem.getNotes())
                    .dueDate(LocalDateTime.now().plusDays(30)) // 30 days from creation
                    .build();

            bill = billRepository.save(bill);

            // Split the bill into one PENDING share per eligible tenant.
            createTenantShares(bill, account.getRoomId());

            createdBills.add(bill);
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
        // A tenant sees one entry per share they own; amounts reflect their share.
        List<ElectricityPayment> shares = paymentRepository.findByTenantIdWithBillDetails(tenantId);
        return shares.stream()
                .map(share -> mapShareToBillDto(share.getElectricityBill(), share))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ElectricityBillDto getBillDetails(UUID billId, UUID userId) {
        ElectricityBill bill = billRepository.findById(billId)
                .orElseThrow(() -> new NotFoundException("Bill not found"));

        boolean isOwner = bill.getOwnerId().equals(userId);
        ElectricityPayment tenantShare = paymentRepository
                .findByBillIdAndTenantId(billId, userId).orElse(null);

        // Access: the owner of the bill, or a tenant holding a share of it.
        if (!isOwner && tenantShare == null) {
            throw new ConflictException("You don't have access to this bill");
        }

        ElectricityBillDto billDto;
        if (isOwner) {
            billDto = mapToBillDto(bill);
            // Owner perspective: remaining across all pending bills for the account.
            billDto.setTotalRemainingForRoom(
                    billRepository.sumRemainingAmountByAccountId(bill.getAccountId()));
        } else {
            // Tenant perspective: amounts/status reflect their own share so the
            // payment modal pays exactly that amount.
            billDto = mapShareToBillDto(bill, tenantShare);
        }

        // Payment history. The owner sees every tenant's share; a tenant only sees
        // their own share (no other tenants' payment details).
        List<ElectricityPayment> payments = paymentRepository.findByBillIdWithTenantDetails(billId);
        billDto.setPayments(payments.stream()
                .filter(p -> isOwner || p.getTenantId().equals(userId))
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

        // The tenant pays their own pre-allocated share of the bill.
        ElectricityPayment share = paymentRepository
                .findByBillIdAndTenantId(request.getBillId(), tenantId)
                .orElseThrow(() -> new ConflictException("You don't have a share in this bill"));

        if (share.getStatus() == PaymentStatus.COMPLETED) {
            throw new ConflictException("Your share for this bill is already paid");
        }

        // Each share is paid in full.
        if (request.getAmount().compareTo(share.getAmount()) != 0) {
            throw new ConflictException("Payment amount must equal your share of "
                    + share.getAmount());
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

        // Mark the tenant's share as paid.
        share.setStatus(PaymentStatus.COMPLETED);
        share.setPaymentMode(request.getPaymentMode());
        share.setPaymentReference(request.getPaymentReference());
        share.setRazorpayOrderId(request.getRazorpayOrderId());
        share.setRazorpayPaymentId(request.getRazorpayPaymentId());
        share.setNotes(request.getNotes());
        share.setPaidAt(LocalDateTime.now());
        share = paymentRepository.save(share);

        // Roll the share up into the bill totals; status flips to COMPLETED when
        // every share has been paid (remaining amount hits 0).
        bill.setPaidAmount(bill.getPaidAmount().add(share.getAmount()));
        bill.updateRemainingAmount();
        billRepository.save(bill);

        // Record the transaction so it appears in Payment History.
        recordElectricityTransaction(bill, tenantId, share.getAmount(), request.getPaymentMode(),
                request.getPaymentMode() == TransactionMode.CASH);

        log.info("Share paid by tenant {}. Bill status: {}, Remaining: {}",
                tenantId, bill.getStatus(), bill.getRemainingAmount());

        return mapToPaymentDto(share);
    }

    /**
     * Issues a single cash OTP covering all of a tenant's outstanding electricity
     * bills. One {@code cash_payment_otps} row is created with one {@code payment_otps}
     * row per bill, so the tenant can pay every bill (pay-all) with the same OTP.
     */
    @Transactional
    public String sendPayAllOtp(UUID tenantId) {
        List<ElectricityPayment> pendingShares = paymentRepository.findByTenantIdWithBillDetails(tenantId).stream()
                .filter(s -> s.getStatus() != PaymentStatus.COMPLETED)
                .collect(Collectors.toList());

        if (pendingShares.isEmpty()) {
            throw new ConflictException("No outstanding electricity bills to pay");
        }

        List<String> billIds = pendingShares.stream()
                .map(s -> s.getBillId().toString())
                .distinct()
                .collect(Collectors.toList());

        // Owner of the bills (tenant's electricity bills belong to their room's owner).
        User owner = pendingShares.get(0).getElectricityBill().getRoom().getHostel().getOwner();

        return cashPaymentOtpService.generateAndSend(
                owner, com.krunity.HostelManagment.enums.CashPaymentMethod.ELECTRICITY_BILL, billIds);
    }

    // Owner Collections dashboard

    /**
     * Per-tenant aggregate of electricity dues across all of the owner's bills,
     * for the owner Collections dashboard.
     */
    @Transactional(readOnly = true)
    public List<ElectricityCollectionRowDto> getOwnerCollectionSummary(UUID ownerId) {
        LocalDateTime now = LocalDateTime.now();
        List<ElectricityPayment> shares = paymentRepository.findByOwnerIdWithDetails(ownerId);

        // Group shares by tenant, accumulating amounts.
        java.util.Map<UUID, ElectricityCollectionRowDto.ElectricityCollectionRowDtoBuilder> rows =
                new java.util.LinkedHashMap<>();
        java.util.Map<UUID, BigDecimal[]> sums = new java.util.HashMap<>(); // [total, paid, outstanding, overdue]
        java.util.Map<UUID, Integer> pendingCounts = new java.util.HashMap<>();

        for (ElectricityPayment share : shares) {
            UUID tenantId = share.getTenantId();
            ElectricityBill bill = share.getElectricityBill();
            Room room = bill.getRoom();

            rows.computeIfAbsent(tenantId, id -> ElectricityCollectionRowDto.builder()
                    .tenantId(tenantId)
                    .tenantName(share.getTenant() != null ? share.getTenant().getDisplayName() : "N/A")
                    .hostelName(room != null && room.getHostel() != null ? room.getHostel().getHostelName() : "N/A")
                    .roomNumber(room != null ? room.getRoomNumber() : "N/A"));

            BigDecimal[] s = sums.computeIfAbsent(tenantId,
                    id -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            s[0] = s[0].add(share.getAmount());

            boolean paid = share.getStatus() == PaymentStatus.COMPLETED;
            if (paid) {
                s[1] = s[1].add(share.getAmount());
            } else {
                s[2] = s[2].add(share.getAmount());
                pendingCounts.merge(tenantId, 1, Integer::sum);
                if (bill.getDueDate() != null && bill.getDueDate().isBefore(now)) {
                    s[3] = s[3].add(share.getAmount());
                }
            }
        }

        List<ElectricityCollectionRowDto> result = new ArrayList<>();
        for (java.util.Map.Entry<UUID, ElectricityCollectionRowDto.ElectricityCollectionRowDtoBuilder> e : rows.entrySet()) {
            BigDecimal[] s = sums.get(e.getKey());
            result.add(e.getValue()
                    .totalAmount(s[0])
                    .paidAmount(s[1])
                    .outstandingAmount(s[2])
                    .overdueAmount(s[3])
                    .pendingBills(pendingCounts.getOrDefault(e.getKey(), 0))
                    .build());
        }
        return result;
    }

    /** Electricity payment history (all shares) for one tenant, owner view. */
    @Transactional(readOnly = true)
    public List<ElectricityPaymentDto> getTenantElectricityHistory(UUID ownerId, UUID tenantId) {
        return paymentRepository.findByOwnerIdAndTenantIdWithDetails(ownerId, tenantId).stream()
                .map(this::mapToPaymentDtoWithBill)
                .collect(Collectors.toList());
    }

    /**
     * Owner collects (marks as paid, in cash) every outstanding electricity share
     * for a tenant across the owner's bills. Returns the number of shares collected.
     */
    @Transactional
    public int collectTenantDues(UUID ownerId, UUID tenantId) {
        List<ElectricityPayment> shares = paymentRepository
                .findByOwnerIdAndTenantIdWithDetails(ownerId, tenantId);

        int collected = 0;
        for (ElectricityPayment share : shares) {
            if (share.getStatus() == PaymentStatus.COMPLETED) {
                continue;
            }
            share.setStatus(PaymentStatus.COMPLETED);
            share.setPaymentMode(com.krunity.HostelManagment.enums.TransactionMode.CASH);
            share.setPaidAt(LocalDateTime.now());
            share.setNotes("Collected by owner");
            paymentRepository.save(share);

            ElectricityBill bill = share.getElectricityBill();
            bill.setPaidAmount(bill.getPaidAmount().add(share.getAmount()));
            bill.updateRemainingAmount();
            billRepository.save(bill);

            // Owner-collected cash payment — record the transaction (no OTP).
            recordElectricityTransaction(bill, tenantId, share.getAmount(), TransactionMode.CASH, false);
            collected++;
        }

        log.info("Owner {} collected {} electricity share(s) from tenant {}", ownerId, collected, tenantId);
        return collected;
    }

    // Helper methods

    /**
     * Splits a freshly created bill equally among the room's eligible tenants and
     * persists one PENDING {@link ElectricityPayment} share per tenant.
     *
     * A tenant is eligible when they currently occupy the room (an occupying
     * allotment status) and have been allotted it for at least
     * {@link #MIN_OCCUPANCY_DAYS} days, i.e. {@code today - startDate >= 15}.
     */
    private void createTenantShares(ElectricityBill bill, UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room not found"));

        LocalDate today = LocalDate.now();
        List<UUID> eligibleTenantIds = roomAllotmentRepository
                .findByRoomAndRoomAllotmentStatusIn(room, RoomAllotmentStatus.occupyingStatuses())
                .stream()
                .filter(a -> a.getStartDate() != null
                        && ChronoUnit.DAYS.between(a.getStartDate(), today) >= MIN_OCCUPANCY_DAYS)
                .map(a -> a.getTenant().getUserId())
                .distinct()
                .collect(Collectors.toList());

        if (eligibleTenantIds.isEmpty()) {
            log.info("No eligible tenants for bill {} (room {}); no shares created", bill.getBillId(), roomId);
            return;
        }

        List<BigDecimal> shares = splitAmount(bill.getTotalAmount(), eligibleTenantIds.size());
        for (int i = 0; i < eligibleTenantIds.size(); i++) {
            ElectricityPayment share = ElectricityPayment.builder()
                    .billId(bill.getBillId())
                    .tenantId(eligibleTenantIds.get(i))
                    .amount(shares.get(i))
                    .status(PaymentStatus.PENDING)
                    .build();
            paymentRepository.save(share);
        }

        log.info("Bill {} split into {} share(s)", bill.getBillId(), eligibleTenantIds.size());
    }

    /**
     * Divides {@code total} into {@code parts} amounts of 2-decimal precision that
     * sum back exactly to {@code total}. Any rounding remainder is added to the
     * first share.
     */
    private List<BigDecimal> splitAmount(BigDecimal total, int parts) {
        BigDecimal base = total.divide(BigDecimal.valueOf(parts), 2, RoundingMode.DOWN);
        List<BigDecimal> shares = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            shares.add(base);
        }
        BigDecimal remainder = total.subtract(base.multiply(BigDecimal.valueOf(parts)));
        shares.set(0, shares.get(0).add(remainder));
        return shares;
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

    /** Owner-facing bill: amounts reflect the whole bill across all tenant shares. */
    private ElectricityBillDto mapToBillDto(ElectricityBill bill) {
        return baseBillDto(bill)
                .totalAmount(bill.getTotalAmount())
                .paidAmount(bill.getPaidAmount())
                .remainingAmount(bill.getRemainingAmount())
                .status(bill.getStatus())
                .build();
    }

    /**
     * Tenant-facing bill: amounts and status reflect only the tenant's own share so
     * the existing UI ("Pay {remainingAmount}", status badge) works unchanged.
     */
    private ElectricityBillDto mapShareToBillDto(ElectricityBill bill, ElectricityPayment share) {
        boolean paid = share.getStatus() == PaymentStatus.COMPLETED;
        return baseBillDto(bill)
                .tenantId(share.getTenantId())
                .totalAmount(share.getAmount())
                .paidAmount(paid ? share.getAmount() : BigDecimal.ZERO)
                .remainingAmount(paid ? BigDecimal.ZERO : share.getAmount())
                .status(paid ? BillStatus.COMPLETED : BillStatus.PENDING)
                .build();
    }

    /** Common bill fields shared by the owner and tenant views. */
    private ElectricityBillDto.ElectricityBillDtoBuilder baseBillDto(ElectricityBill bill) {
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
                .billMonth(bill.getBillMonth())
                .billYear(bill.getBillYear())
                .billPeriod(billPeriod)
                .dueDate(bill.getDueDate())
                .notes(bill.getNotes())
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt());
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
                .paidAt(payment.getPaidAt())
                .build();
    }

    /** Like {@link #mapToPaymentDto} but also includes bill period + room (owner history). */
    private ElectricityPaymentDto mapToPaymentDtoWithBill(ElectricityPayment payment) {
        ElectricityPaymentDto dto = mapToPaymentDto(payment);
        ElectricityBill bill = payment.getElectricityBill();
        if (bill != null) {
            dto.setBillPeriod(Month.of(bill.getBillMonth()).name() + " " + bill.getBillYear());
            dto.setRoomNumber(bill.getRoom() != null ? bill.getRoom().getRoomNumber() : null);
        }
        return dto;
    }

    /**
     * Records a tenant→owner {@link Transaction} for a paid electricity share so it
     * shows up in Payment History (mirrors the other-charge payment flow).
     */
    private void recordElectricityTransaction(ElectricityBill bill, UUID tenantId,
                                              java.math.BigDecimal amount, TransactionMode mode,
                                              boolean otpVerified) {
        User tenant = userRepository.findById(tenantId).orElse(null);
        User owner = userRepository.findById(bill.getOwnerId()).orElse(null);
        if (tenant == null || owner == null) {
            log.warn("Skipping electricity transaction record; tenant or owner not found");
            return;
        }
        String period = Month.of(bill.getBillMonth()).name() + " " + bill.getBillYear();
        transactionRepository.save(Transaction.builder()
                .fromUser(tenant)
                .toUser(owner)
                .amount(amount.longValue()) // Transaction.amount is stored in rupees
                .mode(mode)
                .status(TransactionStatus.COMPLETED)
                .reason("Electricity bill payment: " + period)
                .otpVerified(otpVerified)
                .confirmedAt(LocalDateTime.now())
                .build());
    }
}