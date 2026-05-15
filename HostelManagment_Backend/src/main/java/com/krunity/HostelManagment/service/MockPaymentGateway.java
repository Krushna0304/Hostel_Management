package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.CreateOrderRequest;
import com.krunity.HostelManagment.dto.CreateOrderResponse;
import com.krunity.HostelManagment.dto.RefundRequest;
import com.krunity.HostelManagment.dto.RefundResponse;
import com.krunity.HostelManagment.dto.VerifyPaymentRequest;
import com.krunity.HostelManagment.dto.VerifyPaymentResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Mock implementation for local development and testing.
 * Always returns success — no real API calls made.
 * Activate with: PAYMENT_PROVIDER=mock
 */
@Slf4j
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        String mockOrderId = "mock_order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("🧪 MockPaymentGateway: createOrder() → {}", mockOrderId);
        return CreateOrderResponse.builder()
                .orderId(mockOrderId)
                .providerKey("mock_key_public")
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .receiptId(request.getReceiptId())
                .provider(getProviderName())
                .build();
    }

    @Override
    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request) {
        log.info("🧪 MockPaymentGateway: verifyPayment() → always verified");
        return VerifyPaymentResponse.builder()
                .verified(true)
                .paymentId(request.getPaymentId() != null ? request.getPaymentId() : "mock_pay_" + UUID.randomUUID())
                .orderId(request.getOrderId())
                .message("Mock payment verified successfully")
                .build();
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        String mockRefundId = "mock_refund_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.info("🧪 MockPaymentGateway: refund() → {}", mockRefundId);
        return RefundResponse.builder()
                .refundId(mockRefundId)
                .paymentId(request.getPaymentId())
                .amount(request.getAmount())
                .status("processed")
                .build();
    }

    @Override
    public String getProviderName() {
        return "mock";
    }
}
