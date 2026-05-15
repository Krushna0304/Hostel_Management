package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.CreateOrderRequest;
import com.krunity.HostelManagment.dto.CreateOrderResponse;
import com.krunity.HostelManagment.dto.RefundRequest;
import com.krunity.HostelManagment.dto.RefundResponse;
import com.krunity.HostelManagment.dto.VerifyPaymentRequest;
import com.krunity.HostelManagment.dto.VerifyPaymentResponse;

/**
 * PaymentGateway — Core abstraction interface.
 *
 * All payment providers (Razorpay, Stripe, PayPal, etc.) must implement this.
 * Business logic depends ONLY on this interface — never on a concrete provider.
 * Switching providers = change config, zero business logic changes.
 */
public interface PaymentGateway {

    /**
     * Create a payment order with the provider.
     * Returns an order ID and provider-specific metadata needed by the frontend.
     */
    CreateOrderResponse createOrder(CreateOrderRequest request);

    /**
     * Verify payment signature/status after frontend completes payment.
     * Returns whether the payment is authentic and successful.
     */
    VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request);

    /**
     * Initiate a refund for a completed payment.
     */
    RefundResponse refund(RefundRequest request);

    /**
     * Returns the provider name for logging/auditing.
     */
    String getProviderName();
}
