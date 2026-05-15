package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.CreateOrderRequest;
import com.krunity.HostelManagment.dto.CreateOrderResponse;
import com.krunity.HostelManagment.dto.VerifyPaymentRequest;
import com.krunity.HostelManagment.dto.VerifyPaymentResponse;
import com.krunity.HostelManagment.enums.PaymentStatus;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.enums.TransactionMode;
import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.*;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtherChargePaymentService {

    private final OtherChargeRepository otherChargeRepository;
    private final OtherChargeInstallmentRepository installmentRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final RoomAllotmentRepository roomAllotmentRepository;
    
    @Autowired
    private OwnerRazorpayService ownerRazorpayService;

    /**
     * Create payment order for other charge (full payment)
     */
    @Transactional
    public CreateOrderResponse createOtherChargePaymentOrder(UUID chargeId, Long amount, String currency, UUID tenantId) {
        log.info("Creating payment order for other charge: {} by tenant: {}", chargeId, tenantId);

        OtherCharge charge = otherChargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found"));

        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        // Validate tenant can pay this charge
        validateTenantCanPayCharge(charge, tenant);

        // Validate amount
        BigDecimal chargeAmount = getChargeAmountForTenant(charge, tenant);
        if (!chargeAmount.equals(BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100)))) {
            throw new IllegalArgumentException("Invalid payment amount");
        }

        // Get owner for payment gateway selection
        UUID ownerId = charge.getOwner().getUserId();

        // Create payment order using existing payment service pattern
        CreateOrderRequest request = CreateOrderRequest.builder()
                .amount(amount)
                .currency(currency != null ? currency : "INR")
                .receiptId("OTHER_CHARGE_" + chargeId.toString().substring(0, 20))
                .description("Payment for " + charge.getChargeName())
                .build();

        // Use owner-specific payment gateway
        PaymentGateway gateway = getPaymentGatewayForOwner(ownerId);
        return gateway.createOrder(request);
    }

    /**
     * Create payment order for other charge installment
     */
    @Transactional
    public CreateOrderResponse createOtherChargeInstallmentPaymentOrder(UUID installmentId, Long amount, String currency, UUID tenantId) {
        log.info("Creating payment order for other charge installment: {} by tenant: {}", installmentId, tenantId);

        OtherChargeInstallment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new IllegalArgumentException("Installment not found"));

        // Validate tenant can pay this installment
        if (!installment.getTenant().getUserId().equals(tenantId)) {
            throw new IllegalArgumentException("Not authorized to pay this installment");
        }

        // Validate amount
        BigDecimal installmentAmount = installment.getRemainingAmount();
        if (!installmentAmount.equals(BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100)))) {
            throw new IllegalArgumentException("Invalid payment amount");
        }

        // Get owner for payment gateway selection
        UUID ownerId = installment.getOtherCharge().getOwner().getUserId();

        // Create payment order
        CreateOrderRequest request = CreateOrderRequest.builder()
                .amount(amount)
                .currency(currency != null ? currency : "INR")
                .receiptId("INST_" + installmentId.toString().substring(0, 25))
                .description("Installment payment for " + installment.getOtherCharge().getChargeName())
                .build();

        // Use owner-specific payment gateway
        PaymentGateway gateway = getPaymentGatewayForOwner(ownerId);
        return gateway.createOrder(request);
    }

    /**
     * Verify payment for other charge
     */
    @Transactional
    public VerifyPaymentResponse verifyOtherChargePayment(VerifyPaymentRequest request, UUID chargeId, UUID tenantId) {
        log.info("Verifying payment for other charge: {} by tenant: {}", chargeId, tenantId);

        OtherCharge charge = otherChargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found"));

        // Get owner for payment gateway selection
        UUID ownerId = charge.getOwner().getUserId();

        // Verify payment with payment gateway
        PaymentGateway gateway = getPaymentGatewayForOwner(ownerId);
        VerifyPaymentResponse verificationResponse = gateway.verifyPayment(request);

        if (verificationResponse.isVerified()) {
            // Get the charge amount for this tenant
            User tenant = userRepository.findById(tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
            BigDecimal chargeAmount = getChargeAmountForTenant(charge, tenant);
            
            // Update charge payment status
            processOtherChargePayment(chargeId, tenantId, request.getOrderId(), chargeAmount);
        }

        return verificationResponse;
    }

    /**
     * Verify payment for other charge installment
     */
    @Transactional
    public VerifyPaymentResponse verifyOtherChargeInstallmentPayment(VerifyPaymentRequest request, UUID installmentId, UUID tenantId) {
        log.info("Verifying payment for other charge installment: {} by tenant: {}", installmentId, tenantId);

        OtherChargeInstallment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new IllegalArgumentException("Installment not found"));

        // Get owner for payment gateway selection
        UUID ownerId = installment.getOtherCharge().getOwner().getUserId();

        // Verify payment with payment gateway
        PaymentGateway gateway = getPaymentGatewayForOwner(ownerId);
        VerifyPaymentResponse verificationResponse = gateway.verifyPayment(request);

        if (verificationResponse.isVerified()) {
            // Get the installment amount
            BigDecimal installmentAmount = installment.getRemainingAmount();
            
            // Update installment payment status
            processOtherChargeInstallmentPayment(installmentId, tenantId, request.getOrderId(), installmentAmount);
        }

        return verificationResponse;
    }

    /**
     * Record cash payment for other charge (Owner collection)
     */
    @Transactional
    public void recordOtherChargeCashPayment(UUID chargeId, UUID tenantId, BigDecimal amount, UUID ownerId, String notes) {
        log.info("Recording cash payment for other charge: {} by tenant: {} collected by owner: {}", chargeId, tenantId, ownerId);

        OtherCharge charge = otherChargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found"));

        // Verify owner authorization
        if (!charge.getOwner().getUserId().equals(ownerId)) {
            throw new IllegalArgumentException("Not authorized to collect payment for this charge");
        }

        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        validateTenantCanPayCharge(charge, tenant);

        // Process payment
        processOtherChargePayment(chargeId, tenantId, "CASH_" + System.currentTimeMillis(), amount);

        log.info("Cash payment recorded for other charge: {}", chargeId);
    }

    /**
     * Record cash payment for other charge installment (Owner collection)
     */
    @Transactional
    public void recordOtherChargeInstallmentCashPayment(UUID installmentId, BigDecimal amount, UUID tenantId, String notes) {
        log.info("Recording cash payment for other charge installment: {} by tenant: {}", installmentId, tenantId);

        OtherChargeInstallment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new IllegalArgumentException("Installment not found"));

        // Get owner ID from the installment's parent charge
        UUID ownerId = installment.getOtherCharge().getOwner().getUserId();

        // Process payment
        processOtherChargeInstallmentPayment(installmentId, tenantId, 
                "CASH_" + System.currentTimeMillis(), amount);

        log.info("Cash payment recorded for other charge installment: {}", installmentId);
    }

    private void processOtherChargePayment(UUID chargeId, UUID tenantId, String transactionId, BigDecimal amount) {
        OtherCharge charge = otherChargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found"));

        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        // Update charge payment status
        BigDecimal currentPaid = charge.getPaidAmount() != null ? charge.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newPaidAmount = currentPaid.add(amount);

        charge.setPaidAmount(newPaidAmount);
        charge.setPaidDate(LocalDateTime.now());

        // Update payment status
        if (newPaidAmount.compareTo(charge.getAmount()) >= 0) {
            charge.setPaymentStatus(PaymentStatus.COMPLETED);
        } else {
            charge.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        otherChargeRepository.save(charge);

        // Create transaction record
        createTransactionRecord(charge, tenant, amount, transactionId);

        log.info("Processed payment for other charge: {} amount: {}", chargeId, amount);
    }

    private void processOtherChargeInstallmentPayment(UUID installmentId, UUID tenantId, String transactionId, BigDecimal amount) {
        OtherChargeInstallment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new IllegalArgumentException("Installment not found"));

        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        // Update installment payment status
        BigDecimal currentPaid = installment.getPaidAmount() != null ? installment.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newPaidAmount = currentPaid.add(amount);

        installment.setPaidAmount(newPaidAmount);
        installment.setPaidDate(LocalDateTime.now());
        installment.setTransactionId(transactionId);

        // Update payment status
        if (newPaidAmount.compareTo(installment.getAmount()) >= 0) {
            installment.setPaymentStatus(PaymentStatus.COMPLETED);
        } else {
            installment.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        installmentRepository.save(installment);

        // Update parent charge status
        updateParentChargeStatus(installment.getOtherCharge());

        // Create transaction record
        createInstallmentTransactionRecord(installment, tenant, amount, transactionId);

        log.info("Processed installment payment: {} amount: {}", installmentId, amount);
    }

    private void updateParentChargeStatus(OtherCharge charge) {
        List<OtherChargeInstallment> installments = installmentRepository.findByOtherChargeOrderByInstallmentNumber(charge);
        
        BigDecimal totalPaid = installments.stream()
                .map(inst -> inst.getPaidAmount() != null ? inst.getPaidAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        charge.setPaidAmount(totalPaid);

        boolean allCompleted = installments.stream()
                .allMatch(inst -> inst.getPaymentStatus() == PaymentStatus.COMPLETED);

        if (allCompleted) {
            charge.setPaymentStatus(PaymentStatus.COMPLETED);
            charge.setPaidDate(LocalDateTime.now());
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            charge.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        otherChargeRepository.save(charge);
    }

    private BigDecimal getChargeAmountForTenant(OtherCharge charge, User tenant) {
        if (charge.isTenantSpecific()) {
            return charge.getAmount();
        } else if (charge.isRoomBased()) {
            // Calculate split amount based on current room occupancy
            List<RoomAllotment> allotments = roomAllotmentRepository.findByRoomAndRoomAllotmentStatus(charge.getRoom(), RoomAllotmentStatus.CONFIRMED);
            if (allotments.isEmpty()) {
                throw new IllegalArgumentException("No active tenants in room");
            }
            return charge.getAmount().divide(BigDecimal.valueOf(allotments.size()), 2, java.math.RoundingMode.HALF_UP);
        }
        throw new IllegalArgumentException("Invalid charge type");
    }

    private void validateTenantCanPayCharge(OtherCharge charge, User tenant) {
        if (charge.isTenantSpecific()) {
            if (!charge.getTenant().getUserId().equals(tenant.getUserId())) {
                throw new IllegalArgumentException("Not authorized to pay this charge");
            }
        } else if (charge.isRoomBased()) {
            // Check if tenant is currently in the room
            List<RoomAllotment> allotments = roomAllotmentRepository.findByRoomAndRoomAllotmentStatus(charge.getRoom(), RoomAllotmentStatus.CONFIRMED);
            boolean tenantInRoom = allotments.stream()
                    .anyMatch(allotment -> allotment.getTenant().getUserId().equals(tenant.getUserId()));
            if (!tenantInRoom) {
                throw new IllegalArgumentException("Tenant is not currently in the charged room");
            }
        }
    }

    private void createTransactionRecord(OtherCharge charge, User tenant, BigDecimal amount, String transactionId) {
        // Create transaction record for other charge payment
        Transaction transaction = Transaction.builder()
                .fromUser(tenant)
                .toUser(charge.getOwner())
                .amount(amount.multiply(BigDecimal.valueOf(100)).longValue()) // Convert to paise
                .mode(transactionId.startsWith("CASH_") ? TransactionMode.CASH : TransactionMode.ONLINE)
                .status(TransactionStatus.COMPLETED)
                .reason("Payment for other charge: " + charge.getChargeName())
                .otpVerified(true)
                .confirmedAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        log.info("Created transaction record for other charge payment: {} amount: {}", charge.getChargeId(), amount);
    }

    private void createInstallmentTransactionRecord(OtherChargeInstallment installment, User tenant, BigDecimal amount, String transactionId) {
        // Create transaction record for installment payment
        Transaction transaction = Transaction.builder()
                .fromUser(tenant)
                .toUser(installment.getOtherCharge().getOwner())
                .amount(amount.multiply(BigDecimal.valueOf(100)).longValue()) // Convert to paise
                .mode(transactionId.startsWith("CASH_") ? TransactionMode.CASH : TransactionMode.ONLINE)
                .status(TransactionStatus.COMPLETED)
                .reason("Installment payment for: " + installment.getOtherCharge().getChargeName())
                .otpVerified(true)
                .confirmedAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        log.info("Created transaction record for installment payment: {} amount: {}", installment.getInstallmentId(), amount);
    }

    /**
     * Get payment gateway for a specific owner (following PaymentService pattern)
     */
    private PaymentGateway getPaymentGatewayForOwner(UUID ownerId) {
        try {
            // Check if owner has payments enabled
            if (!ownerRazorpayService.isPaymentsEnabled(ownerId)) {
                throw new IllegalStateException(
                    "Payments are currently unavailable. Please contact the hostel owner to enable payment processing."
                );
            }

            // Get owner's Razorpay credentials
            OwnerRazorpayService.RazorpayCredentials credentials = 
                ownerRazorpayService.getDecryptedCredentials(ownerId);

            // Create owner-specific gateway
            return new OwnerRazorpayGateway(ownerId, credentials.keyId, credentials.keySecret);

        } catch (NotFoundException e) {
            log.warn("Owner {} has no Razorpay configuration", ownerId);
            throw new IllegalStateException("Payment gateway not configured for this owner");
        } catch (RazorpayException e) {
            log.error("Failed to initialize Razorpay gateway for owner {}: {}", ownerId, e.getMessage());
            throw new RuntimeException("Failed to initialize payment gateway: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to get payment gateway for owner {}: {}", ownerId, e.getMessage());
            throw new RuntimeException("Failed to initialize payment gateway: " + e.getMessage(), e);
        }
    }
}