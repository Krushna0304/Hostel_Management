package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.CreateOrderRequest;
import com.krunity.HostelManagment.dto.CreateOrderResponse;
import com.krunity.HostelManagment.dto.RefundRequest;
import com.krunity.HostelManagment.dto.RefundResponse;
import com.krunity.HostelManagment.dto.VerifyPaymentRequest;
import com.krunity.HostelManagment.dto.VerifyPaymentResponse;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Dynamic Razorpay gateway that uses owner-specific credentials.
 * Created on-demand for each payment transaction.
 */
@Slf4j
public class OwnerRazorpayGateway implements PaymentGateway {

    private final RazorpayClient razorpayClient;
    private final String keyId;
    private final String keySecret;
    private final UUID ownerId;

    public OwnerRazorpayGateway(UUID ownerId, String keyId, String keySecret) throws RazorpayException {
        this.ownerId = ownerId;
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
        log.info("✅ OwnerRazorpayGateway initialized for owner: {} (key: {}...)", 
                ownerId, keyId.substring(0, Math.min(keyId.length(), 12)));
    }

    @Override
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount());
            orderRequest.put("currency", request.getCurrency() != null ? request.getCurrency() : "INR");
            orderRequest.put("receipt", request.getReceiptId());
            orderRequest.put("payment_capture", 1);

            if (request.getDescription() != null) {
                JSONObject notes = new JSONObject();
                notes.put("description", request.getDescription());
                notes.put("owner_id", ownerId.toString());
                orderRequest.put("notes", notes);
            }

            Order order = razorpayClient.orders.create(orderRequest);
            log.info("Razorpay order created for owner {}: {} for receipt: {}", 
                    ownerId, order.get("id"), request.getReceiptId());

            return CreateOrderResponse.builder()
                    .orderId(order.get("id"))
                    .providerKey(keyId)
                    .amount(request.getAmount())
                    .currency(order.get("currency"))
                    .receiptId(request.getReceiptId())
                    .provider("razorpay")
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for owner {}: {}", ownerId, e.getMessage());
            throw new RuntimeException("Failed to create payment order: " + e.getMessage(), e);
        }
    }

    @Override
    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request) {
        try {
            String payload = request.getOrderId() + "|" + request.getPaymentId();
            String generatedSignature = generateHmacSha256(payload, keySecret);

            boolean isValid = generatedSignature.equals(request.getSignature());

            log.info("Razorpay payment verification for owner {} orderId={} paymentId={}: {}",
                    ownerId, request.getOrderId(), request.getPaymentId(), isValid ? "VERIFIED" : "FAILED");

            return VerifyPaymentResponse.builder()
                    .verified(isValid)
                    .paymentId(request.getPaymentId())
                    .orderId(request.getOrderId())
                    .message(isValid ? "Payment verified successfully" : "Invalid payment signature")
                    .build();

        } catch (Exception e) {
            log.error("Razorpay payment verification error for owner {}: {}", ownerId, e.getMessage());
            return VerifyPaymentResponse.builder()
                    .verified(false)
                    .paymentId(request.getPaymentId())
                    .orderId(request.getOrderId())
                    .message("Verification error: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        try {
            JSONObject refundRequest = new JSONObject();
            if (request.getAmount() > 0) {
                refundRequest.put("amount", request.getAmount());
            }
            if (request.getReason() != null) {
                refundRequest.put("notes", new JSONObject().put("reason", request.getReason()));
            }

            Refund refund = razorpayClient.payments.refund(request.getPaymentId(), refundRequest);
            log.info("Razorpay refund created for owner {}: {} for payment: {}", 
                    ownerId, refund.get("id"), request.getPaymentId());

            return RefundResponse.builder()
                    .refundId(refund.get("id"))
                    .paymentId(request.getPaymentId())
                    .amount(((Number) refund.get("amount")).longValue())
                    .status(refund.get("status"))
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay refund failed for owner {}: {}", ownerId, e.getMessage());
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "razorpay";
    }

    private String generateHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
