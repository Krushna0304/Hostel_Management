package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.CreateOrderRequest;
import com.krunity.HostelManagment.dto.CreateOrderResponse;
import com.krunity.HostelManagment.dto.RefundRequest;
import com.krunity.HostelManagment.dto.RefundResponse;
import com.krunity.HostelManagment.dto.VerifyPaymentRequest;
import com.krunity.HostelManagment.dto.VerifyPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * Stripe stub implementation of PaymentGateway.
 *
 * Demonstrates extensibility — switching from Razorpay to Stripe
 * requires ONLY setting PAYMENT_PROVIDER=stripe in config.
 * Zero changes to PaymentService or AgreementService.
 *
 * To fully implement: add stripe-java dependency and implement each method.
 */
@Slf4j
public class StripePaymentGateway implements PaymentGateway {

    private final String apiKey;

    public StripePaymentGateway(@Value("${payment.stripe.api-key}") String apiKey) {
        this.apiKey = apiKey;
        log.info("✅ StripePaymentGateway initialized (stub)");
    }

    @Override
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        // TODO: Implement using Stripe PaymentIntent API
        // Stripe.apiKey = apiKey;
        // PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
        //     .setAmount(request.getAmount())
        //     .setCurrency(request.getCurrency())
        //     .build();
        // PaymentIntent intent = PaymentIntent.create(params);
        log.warn("StripePaymentGateway.createOrder() is a stub — not yet implemented");
        return CreateOrderResponse.builder()
                .orderId("stripe_order_stub_" + request.getReceiptId())
                .providerKey(apiKey)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .receiptId(request.getReceiptId())
                .provider(getProviderName())
                .build();
    }

    @Override
    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request) {
        // TODO: Verify Stripe webhook signature using Stripe.Webhook.constructEvent()
        log.warn("StripePaymentGateway.verifyPayment() is a stub — not yet implemented");
        return VerifyPaymentResponse.builder()
                .verified(true)
                .paymentId(request.getPaymentId())
                .orderId(request.getOrderId())
                .message("Stripe stub: payment assumed verified")
                .build();
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        // TODO: Implement using Stripe Refund API
        log.warn("StripePaymentGateway.refund() is a stub — not yet implemented");
        return RefundResponse.builder()
                .refundId("stripe_refund_stub")
                .paymentId(request.getPaymentId())
                .amount(request.getAmount())
                .status("succeeded")
                .build();
    }

    @Override
    public String getProviderName() {
        return "stripe";
    }
}
