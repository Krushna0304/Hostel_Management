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
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Razorpay implementation of PaymentGateway.
 *
 * Registered as a Spring bean only when payment.provider=razorpay.
 * Business logic never imports this class directly — it uses PaymentGateway.
 */
@Slf4j
public class RazorpayPaymentGateway implements PaymentGateway {

    private final RazorpayClient razorpayClient;
    private final String keyId;
    private final String keySecret;
    private final String defaultCurrency;

    public RazorpayPaymentGateway(
            @Value("${payment.razorpay.key-id}") String keyId,
            @Value("${payment.razorpay.key-secret}") String keySecret,
            @Value("${payment.razorpay.currency:INR}") String defaultCurrency
    ) throws RazorpayException {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.defaultCurrency = defaultCurrency;
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
        log.info("✅ RazorpayPaymentGateway initialized (key: {}...)", keyId.substring(0, Math.min(keyId.length(), 12)));
    }

    @Override
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount());  // in paise
            orderRequest.put("currency", request.getCurrency() != null ? request.getCurrency() : defaultCurrency);
            orderRequest.put("receipt", request.getReceiptId());
            orderRequest.put("payment_capture", 1); // auto-capture

            if (request.getDescription() != null) {
                JSONObject notes = new JSONObject();
                notes.put("description", request.getDescription());
                orderRequest.put("notes", notes);
            }

            Order order = razorpayClient.orders.create(orderRequest);
            log.info("Razorpay order created: {} for receipt: {}", order.get("id"), request.getReceiptId());

            return CreateOrderResponse.builder()
                    .orderId(order.get("id"))
                    .providerKey(keyId)
                    .amount(request.getAmount())
                    .currency(order.get("currency"))
                    .receiptId(request.getReceiptId())
                    .provider(getProviderName())
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to create payment order: " + e.getMessage(), e);
        }
    }

    @Override
    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request) {
        try {
            // Razorpay signature = HMAC-SHA256(orderId + "|" + paymentId, keySecret)
            String payload = request.getOrderId() + "|" + request.getPaymentId();
            String generatedSignature = generateHmacSha256(payload, keySecret);

            boolean isValid = generatedSignature.equals(request.getSignature());

            log.info("Razorpay payment verification for orderId={} paymentId={}: {}",
                    request.getOrderId(), request.getPaymentId(), isValid ? "VERIFIED" : "FAILED");

            return VerifyPaymentResponse.builder()
                    .verified(isValid)
                    .paymentId(request.getPaymentId())
                    .orderId(request.getOrderId())
                    .message(isValid ? "Payment verified successfully" : "Invalid payment signature")
                    .build();

        } catch (Exception e) {
            log.error("Razorpay payment verification error: {}", e.getMessage());
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
            log.info("Razorpay refund created: {} for payment: {}", refund.get("id"), request.getPaymentId());

            return RefundResponse.builder()
                    .refundId(refund.get("id"))
                    .paymentId(request.getPaymentId())
                    .amount(((Number) refund.get("amount")).longValue())
                    .status(refund.get("status"))
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay refund failed: {}", e.getMessage());
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "razorpay";
    }

    // HMAC-SHA256 signature generation
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
