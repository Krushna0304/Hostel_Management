package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.dto.CreateOrderResponse;
import com.krunity.HostelManagment.dto.RefundResponse;
import com.krunity.HostelManagment.dto.VerifyPaymentRequest;
import com.krunity.HostelManagment.dto.VerifyPaymentResponse;
import com.krunity.HostelManagment.service.PaymentService;
import com.krunity.HostelManagment.service.OtherChargePaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * PaymentController — API layer for payment operations.
 *
 * Calls ONLY PaymentService — never touches gateway implementations directly.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OtherChargePaymentService otherChargePaymentService;

    public PaymentController(PaymentService paymentService, OtherChargePaymentService otherChargePaymentService) {
        this.paymentService = paymentService;
        this.otherChargePaymentService = otherChargePaymentService;
    }

    /**
     * POST /api/payments/create-order
     *
     * Called by frontend before opening the payment modal.
     * Returns orderId + providerKey needed to initialize the payment SDK.
     *
     * Request body:
     * {
     *   "agreementId": "abc123",
     *   "amount": 8500,
     *   "currency": "INR"
     * }
     */
    @PostMapping("/create-order")
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderApiRequest request) {
        log.info("POST /api/payments/create-order for agreement: {}", request.getAgreementId());
        CreateOrderResponse response = paymentService.createAgreementPaymentOrder(
                request.getAgreementId(),
                request.getAmount(),
                request.getCurrency()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payments/create-installment-order
     *
     * Called by frontend before opening the payment modal for installment payment.
     * Returns orderId + providerKey needed to initialize the payment SDK.
     *
     * Request body:
     * {
     *   "scheduleId": "uuid",
     *   "amount": 8500,
     *   "currency": "INR"
     * }
     */
    @PostMapping("/create-installment-order")
    public ResponseEntity<CreateOrderResponse> createInstallmentOrder(@Valid @RequestBody CreateInstallmentOrderApiRequest request) {
        log.info("POST /api/payments/create-installment-order for schedule: {}", request.getScheduleId());
        CreateOrderResponse response = paymentService.createInstallmentPaymentOrder(
                request.getScheduleId(),
                request.getAmount(),
                request.getCurrency()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payments/verify-payment
     *
     * Called by frontend after payment is completed in the provider's UI.
     * Verifies signature and returns verification status.
     *
     * Request body:
     * {
     *   "orderId": "order_xxx",
     *   "paymentId": "pay_xxx",
     *   "signature": "abc123...",
     *   "agreementId": "abc123"
     * }
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<VerifyPaymentResponse> verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        log.info("POST /api/payments/verify-payment for orderId: {}", request.getOrderId());
        VerifyPaymentResponse response = paymentService.verifyAgreementPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payments/verify-other-charge-payment
     *
     * Called by frontend after other charge payment is completed in the provider's UI.
     * Verifies signature and returns verification status.
     *
     * Request body:
     * {
     *   "orderId": "order_xxx",
     *   "paymentId": "pay_xxx",
     *   "signature": "abc123...",
     *   "chargeId": "uuid",
     *   "tenantId": "uuid"
     * }
     */
    @PostMapping("/verify-other-charge-payment")
    public ResponseEntity<VerifyPaymentResponse> verifyOtherChargePayment(@Valid @RequestBody VerifyOtherChargePaymentRequest request) {
        log.info("POST /api/payments/verify-other-charge-payment for orderId: {}", request.getOrderId());
        
        // Create VerifyPaymentRequest from the request
        VerifyPaymentRequest verifyRequest = new VerifyPaymentRequest();
        verifyRequest.setOrderId(request.getOrderId());
        verifyRequest.setPaymentId(request.getPaymentId());
        verifyRequest.setSignature(request.getSignature());
        
        VerifyPaymentResponse response = otherChargePaymentService.verifyOtherChargePayment(
                verifyRequest, request.getChargeId(), request.getTenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payments/verify-other-charge-installment-payment
     *
     * Called by frontend after other charge installment payment is completed in the provider's UI.
     * Verifies signature and returns verification status.
     *
     * Request body:
     * {
     *   "orderId": "order_xxx",
     *   "paymentId": "pay_xxx",
     *   "signature": "abc123...",
     *   "installmentId": "uuid",
     *   "tenantId": "uuid"
     * }
     */
    @PostMapping("/verify-other-charge-installment-payment")
    public ResponseEntity<VerifyPaymentResponse> verifyOtherChargeInstallmentPayment(@Valid @RequestBody VerifyOtherChargeInstallmentPaymentRequest request) {
        log.info("POST /api/payments/verify-other-charge-installment-payment for orderId: {}", request.getOrderId());
        
        // Create VerifyPaymentRequest from the request
        VerifyPaymentRequest verifyRequest = new VerifyPaymentRequest();
        verifyRequest.setOrderId(request.getOrderId());
        verifyRequest.setPaymentId(request.getPaymentId());
        verifyRequest.setSignature(request.getSignature());
        
        VerifyPaymentResponse response = otherChargePaymentService.verifyOtherChargeInstallmentPayment(
                verifyRequest, request.getInstallmentId(), request.getTenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payments/record-other-charge-cash-payment
     *
     * Called by owner to record cash payment for other charge.
     *
     * Request body:
     * {
     *   "chargeId": "uuid",
     *   "tenantId": "uuid",
     *   "amount": 8500,
     *   "ownerId": "uuid",
     *   "notes": "Cash payment received"
     * }
     */
    @PostMapping("/record-other-charge-cash-payment")
    public ResponseEntity<Map<String, String>> recordOtherChargeCashPayment(@Valid @RequestBody RecordOtherChargeCashPaymentRequest request) {
        log.info("POST /api/payments/record-other-charge-cash-payment for charge: {}", request.getChargeId());
        
        otherChargePaymentService.recordOtherChargeCashPayment(
                request.getChargeId(),
                request.getTenantId(),
                request.getAmount(),
                request.getOwnerId(),
                request.getNotes()
        );
        
        return ResponseEntity.ok(Map.of("message", "Cash payment recorded successfully"));
    }

    /**
     * POST /api/payments/record-other-charge-installment-cash-payment
     *
     * Called by owner to record cash payment for other charge installment.
     *
     * Request body:
     * {
     *   "installmentId": "uuid",
     *   "amount": 8500,
     *   "ownerId": "uuid",
     *   "notes": "Cash payment received"
     * }
     */
    @PostMapping("/record-other-charge-installment-cash-payment")
    public ResponseEntity<Map<String, String>> recordOtherChargeInstallmentCashPayment(@Valid @RequestBody RecordOtherChargeInstallmentCashPaymentRequest request) {
        log.info("POST /api/payments/record-other-charge-installment-cash-payment for installment: {}", request.getInstallmentId());
        
        otherChargePaymentService.recordOtherChargeInstallmentCashPayment(
                request.getInstallmentId(),
                request.getAmount(),
                request.getOwnerId(),
                request.getNotes()
        );
        
        return ResponseEntity.ok(Map.of("message", "Cash payment recorded successfully"));
    }

    /**
     * POST /api/payments/create-other-charge-order
     *
     * Called by frontend before opening the payment modal for other charge payment.
     * Returns orderId + providerKey needed to initialize the payment SDK.
     *
     * Request body:
     * {
     *   "chargeId": "uuid",
     *   "amount": 8500,
     *   "currency": "INR",
     *   "tenantId": "uuid"
     * }
     */
    @PostMapping("/create-other-charge-order")
    public ResponseEntity<CreateOrderResponse> createOtherChargeOrder(@Valid @RequestBody CreateOtherChargeOrderApiRequest request) {
        log.info("POST /api/payments/create-other-charge-order for charge: {}", request.getChargeId());
        CreateOrderResponse response = otherChargePaymentService.createOtherChargePaymentOrder(
                request.getChargeId(),
                request.getAmount(),
                request.getCurrency(),
                request.getTenantId()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payments/create-other-charge-installment-order
     *
     * Called by frontend before opening the payment modal for other charge installment payment.
     * Returns orderId + providerKey needed to initialize the payment SDK.
     *
     * Request body:
     * {
     *   "installmentId": "uuid",
     *   "amount": 8500,
     *   "currency": "INR",
     *   "tenantId": "uuid"
     * }
     */
    @PostMapping("/create-other-charge-installment-order")
    public ResponseEntity<CreateOrderResponse> createOtherChargeInstallmentOrder(@Valid @RequestBody CreateOtherChargeInstallmentOrderApiRequest request) {
        log.info("POST /api/payments/create-other-charge-installment-order for installment: {}", request.getInstallmentId());
        CreateOrderResponse response = otherChargePaymentService.createOtherChargeInstallmentPaymentOrder(
                request.getInstallmentId(),
                request.getAmount(),
                request.getCurrency(),
                request.getTenantId()
        );
        return ResponseEntity.ok(response);
    }
    /**
     * POST /api/payments/refund
     *
     * Initiates a refund for a completed payment.
     *
     * Request body:
     * {
     *   "paymentId": "pay_xxx",
     *   "amount": 8500,
     *   "reason": "Agreement cancelled"
     * }
     */
    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> refund(@Valid @RequestBody RefundApiRequest request) {
        log.info("POST /api/payments/refund for paymentId: {}", request.getPaymentId());
        RefundResponse response = paymentService.refundPayment(
                request.getPaymentId(),
                request.getAmount(),
                request.getReason()
        );
        return ResponseEntity.ok(response);
    }

    // Request DTOs for Other Charges
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOtherChargeOrderApiRequest {
        @NotNull
        private UUID chargeId;
        
        @NotNull
        @Positive
        private Long amount;
        
        @NotBlank
        private String currency;
        
        @NotNull
        private UUID tenantId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOtherChargeInstallmentOrderApiRequest {
        @NotNull
        private UUID installmentId;
        
        @NotNull
        @Positive
        private Long amount;
        
        @NotBlank
        private String currency;
        
        @NotNull
        private UUID tenantId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyOtherChargePaymentRequest {
        @NotBlank
        private String orderId;
        
        @NotBlank
        private String paymentId;
        
        @NotBlank
        private String signature;
        
        @NotNull
        private UUID chargeId;
        
        @NotNull
        private UUID tenantId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyOtherChargeInstallmentPaymentRequest {
        @NotBlank
        private String orderId;
        
        @NotBlank
        private String paymentId;
        
        @NotBlank
        private String signature;
        
        @NotNull
        private UUID installmentId;
        
        @NotNull
        private UUID tenantId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordOtherChargeCashPaymentRequest {
        @NotNull
        private UUID chargeId;
        
        @NotNull
        private UUID tenantId;
        
        @NotNull
        @Positive
        private java.math.BigDecimal amount;
        
        @NotNull
        private UUID ownerId;
        
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordOtherChargeInstallmentCashPaymentRequest {
        @NotNull
        private UUID installmentId;
        
        @NotNull
        @Positive
        private java.math.BigDecimal amount;
        
        @NotNull
        private UUID ownerId;
        
        private String notes;
    }

    /**
     * POST /api/payments/create-settlement-order
     *
     * Called by frontend before opening the payment modal for settlement payment.
     * Returns orderId + providerKey needed to initialize the payment SDK.
     *
     * Request body:
     * {
     *   "settlementId": "uuid",
     *   "amount": 8500,
     *   "currency": "INR"
     * }
     */
    @PostMapping("/create-settlement-order")
    public ResponseEntity<CreateOrderResponse> createSettlementOrder(@Valid @RequestBody CreateSettlementOrderApiRequest request) {
        log.info("POST /api/payments/create-settlement-order for settlement: {}", request.getSettlementId());
        CreateOrderResponse response = paymentService.createSettlementPaymentOrder(
                request.getSettlementId(),
                request.getAmount(),
                request.getCurrency()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/payments/provider
     * Returns the currently active payment provider name.
     */
    @GetMapping("/provider")
    public ResponseEntity<Map<String, String>> getProvider() {
        return ResponseEntity.ok(Map.of("provider", paymentService.getActiveProvider()));
    }

    // ── Inner request DTOs ──────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSettlementOrderApiRequest {
        @NotNull
        private UUID settlementId;
        
        @NotNull
        @Positive
        private Long amount;
        
        @NotBlank
        private String currency;
    }

    @Data
    public static class CreateOrderApiRequest {
        @NotBlank(message = "agreementId is required")
        private String agreementId;

        @NotNull @Positive(message = "amount must be positive")
        private Long amount;

        private String currency = "INR";
    }

    @Data
    public static class CreateInstallmentOrderApiRequest {
        @NotBlank(message = "scheduleId is required")
        private String scheduleId;

        @NotNull @Positive(message = "amount must be positive")
        private Long amount;

        private String currency = "INR";
    }

    @Data
    public static class RefundApiRequest {
        @NotBlank(message = "paymentId is required")
        private String paymentId;

        @NotNull @Positive
        private Long amount;

        private String reason;
    }
}
