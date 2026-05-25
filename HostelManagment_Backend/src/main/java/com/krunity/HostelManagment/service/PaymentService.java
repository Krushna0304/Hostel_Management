package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.CreateOrderRequest;
import com.krunity.HostelManagment.dto.CreateOrderResponse;
import com.krunity.HostelManagment.dto.RefundRequest;
import com.krunity.HostelManagment.dto.RefundResponse;
import com.krunity.HostelManagment.dto.VerifyPaymentRequest;
import com.krunity.HostelManagment.dto.VerifyPaymentResponse;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.PaymentRequestSchedule;
import com.krunity.HostelManagment.model.TenantPaymentPlan;
import com.krunity.HostelManagment.repository.AgreementRepository;
import com.krunity.HostelManagment.repository.PaymentRequestScheduleRepository;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * PaymentService — Business logic layer for payments.
 *
 * Now supports "Bring Your Own Razorpay" model where each owner has their own credentials.
 * Dynamically creates payment gateway instances based on owner configuration.
 */
@Slf4j
@Service
public class PaymentService {

    private final PaymentGateway defaultPaymentGateway;

    @Autowired
    private OwnerRazorpayService ownerRazorpayService;

    @Autowired
    private AgreementRepository agreementRepository;

    @Autowired
    private PaymentRequestScheduleRepository scheduleRepository;

    // Constructor injection — SOLID: Dependency Inversion Principle
    public PaymentService(PaymentGateway paymentGateway) {
        this.defaultPaymentGateway = paymentGateway;
        log.info("PaymentService initialized with default provider: {}", paymentGateway.getProviderName());
    }

    /**
     * Get payment gateway for a specific owner.
     * If owner has configured their own Razorpay, use that.
     * Otherwise, fall back to default gateway.
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
            log.warn("Owner {} has no Razorpay configuration, using default gateway", ownerId);
            return defaultPaymentGateway;
        } catch (RazorpayException e) {
            log.error("Failed to initialize Razorpay gateway for owner {}: {}", ownerId, e.getMessage());
            throw new RuntimeException("Failed to initialize payment gateway: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a payment order for agreement activation.
     * Amount is in rupees — converted to paise internally.
     * Uses owner-specific Razorpay credentials.
     */
    public CreateOrderResponse createAgreementPaymentOrder(String agreementId, long amountInRupees, String currency) {
        log.info("Creating payment order for agreement: {} amount: ₹{}", agreementId, amountInRupees);

        // Get agreement to identify owner
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new NotFoundException("Agreement not found: " + agreementId));

        UUID ownerId = agreement.getOwnerId();
        if (ownerId == null) {
            throw new IllegalStateException("Agreement has no owner assigned");
        }

        log.info("Using Razorpay credentials for owner: {}", ownerId);

        // Get owner-specific payment gateway
        PaymentGateway gateway = getPaymentGatewayForOwner(ownerId);

        // Shorten receipt ID to fit Razorpay's 40 character limit
        // Agreement IDs can be long, so we take a safe substring
        String shortId = agreementId.length() > 35 ? agreementId.substring(0, 35) : agreementId;
        
        CreateOrderRequest request = CreateOrderRequest.builder()
                .amount(amountInRupees * 100)  // Convert to paise
                .currency(currency != null ? currency : "INR")
                .receiptId("AGR-" + shortId)  // AGR- prefix + up to 35 chars = max 39 chars
                .description("Agreement activation payment for " + agreementId)
                .build();

        return gateway.createOrder(request);
    }

    /**
     * Creates a payment order for installment payment.
     * Amount is in rupees — converted to paise internally.
     * Uses owner-specific Razorpay credentials.
     */
    public CreateOrderResponse createInstallmentPaymentOrder(String scheduleId, long amountInRupees, String currency) {
        log.info("Creating payment order for installment: {} amount: ₹{}", scheduleId, amountInRupees);

        // Get schedule to identify owner via agreement
        PaymentRequestSchedule schedule = scheduleRepository.findById(UUID.fromString(scheduleId))
                .orElseThrow(() -> new NotFoundException("Payment schedule not found: " + scheduleId));

        TenantPaymentPlan plan = schedule.getTenantPaymentPlan();
        String agreementId = plan.getAgreementId();
        
        if (agreementId == null) {
            throw new IllegalStateException("Payment plan has no agreement assigned");
        }

        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new NotFoundException("Agreement not found: " + agreementId));

        UUID ownerId = agreement.getOwnerId();
        if (ownerId == null) {
            throw new IllegalStateException("Agreement has no owner assigned");
        }

        log.info("Using Razorpay credentials for owner: {}", ownerId);

        // Get owner-specific payment gateway
        PaymentGateway gateway = getPaymentGatewayForOwner(ownerId);

        // Shorten receipt ID to fit Razorpay's 40 character limit
        // Use last 32 chars of UUID (remove hyphens for compactness)
        String shortId = scheduleId.replace("-", "").substring(0, 32);
        
        CreateOrderRequest request = CreateOrderRequest.builder()
                .amount(amountInRupees * 100)  // Convert to paise
                .currency(currency != null ? currency : "INR")
                .receiptId("I-" + shortId)  // I- prefix + 32 chars = 34 chars total
                .description("Installment payment for " + scheduleId)
                .build();

        return gateway.createOrder(request);
    }

    /**
     * Creates a payment order for settlement payment.
     * Amount is in rupees — converted to paise internally.
     * Uses owner-specific Razorpay credentials.
     */
    public CreateOrderResponse createSettlementPaymentOrder(UUID settlementId, long amountInRupees, String currency) {
        log.info("Creating payment order for settlement: {} amount: ₹{}", settlementId, amountInRupees);

        // For settlement payments, we need to get the owner from the settlement
        // We'll need to inject SettlementService or SettlementRepository to get the settlement details
        // For now, let's use a simple approach and get it via the settlement service
        
        // Get settlement to identify owner via agreement
        // Note: This requires adding SettlementRepository dependency
        
        // For now, use default gateway - this should be updated to use owner-specific gateway
        // when we have access to settlement details
        PaymentGateway gateway = defaultPaymentGateway;

        // Shorten receipt ID to fit Razorpay's 40 character limit
        String shortId = settlementId.toString().replace("-", "").substring(0, 32);
        
        CreateOrderRequest request = CreateOrderRequest.builder()
                .amount(amountInRupees * 100)  // Convert to paise
                .currency(currency != null ? currency : "INR")
                .receiptId("S-" + shortId)  // S- prefix + 32 chars = 34 chars total
                .description("Settlement payment for " + settlementId)
                .build();

        return gateway.createOrder(request);
    }

    /**
     * Verifies payment after frontend completes the Razorpay/Stripe checkout.
     * Uses owner-specific credentials for verification.
     */
    public VerifyPaymentResponse verifyAgreementPayment(VerifyPaymentRequest request) {
        log.info("Verifying payment for agreement: {} orderId: {}", request.getAgreementId(), request.getOrderId());
        
        // Get agreement to identify owner
        Agreement agreement = agreementRepository.findById(request.getAgreementId())
                .orElseThrow(() -> new NotFoundException("Agreement not found: " + request.getAgreementId()));

        UUID ownerId = agreement.getOwnerId();
        if (ownerId == null) {
            throw new IllegalStateException("Agreement has no owner assigned");
        }

        // Get owner-specific payment gateway
        PaymentGateway gateway = getPaymentGatewayForOwner(ownerId);
        
        return gateway.verifyPayment(request);
    }

    /**
     * Verifies installment payment after frontend completes the checkout.
     * Uses owner-specific credentials for verification.
     */
    public VerifyPaymentResponse verifyInstallmentPayment(VerifyPaymentRequest request) {
        log.info("Verifying installment payment orderId: {}", request.getOrderId());
        
        // For installment payments, we need to get the owner from the schedule
        // The scheduleId should be passed in the request
        if (request.getScheduleId() == null) {
            throw new IllegalArgumentException("scheduleId is required for installment payment verification");
        }

        PaymentRequestSchedule schedule = scheduleRepository.findById(UUID.fromString(request.getScheduleId()))
                .orElseThrow(() -> new NotFoundException("Payment schedule not found: " + request.getScheduleId()));

        TenantPaymentPlan plan = schedule.getTenantPaymentPlan();
        String agreementId = plan.getAgreementId();
        
        if (agreementId == null) {
            throw new IllegalStateException("Payment plan has no agreement assigned");
        }

        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new NotFoundException("Agreement not found: " + agreementId));

        UUID ownerId = agreement.getOwnerId();
        if (ownerId == null) {
            throw new IllegalStateException("Agreement has no owner assigned");
        }

        // Get owner-specific payment gateway
        PaymentGateway gateway = getPaymentGatewayForOwner(ownerId);
        
        return gateway.verifyPayment(request);
    }

    /**
     * Initiates a refund for a payment.
     * Note: For owner-specific refunds, you need to pass the ownerId.
     * This method uses the default gateway for backward compatibility.
     */
    public RefundResponse refundPayment(String paymentId, long amountInRupees, String reason) {
        log.info("Initiating refund for paymentId: {} amount: ₹{}", paymentId, amountInRupees);

        RefundRequest request = RefundRequest.builder()
                .paymentId(paymentId)
                .amount(amountInRupees * 100)  // Convert to paise
                .reason(reason)
                .build();

        return defaultPaymentGateway.refund(request);
    }

    /**
     * Initiates a refund for a payment using owner-specific credentials.
     */
    public RefundResponse refundPaymentForOwner(UUID ownerId, String paymentId, long amountInRupees, String reason) {
        log.info("Initiating refund for owner {} paymentId: {} amount: ₹{}", ownerId, paymentId, amountInRupees);

        PaymentGateway gateway = getPaymentGatewayForOwner(ownerId);

        RefundRequest request = RefundRequest.builder()
                .paymentId(paymentId)
                .amount(amountInRupees * 100)  // Convert to paise
                .reason(reason)
                .build();

        return gateway.refund(request);
    }

    public String getActiveProvider() {
        return defaultPaymentGateway.getProviderName();
    }
}
