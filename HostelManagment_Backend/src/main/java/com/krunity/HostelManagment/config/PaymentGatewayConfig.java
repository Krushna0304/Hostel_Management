package com.krunity.HostelManagment.config;

import com.krunity.HostelManagment.service.MockPaymentGateway;
import com.krunity.HostelManagment.service.PaymentGateway;
import com.krunity.HostelManagment.service.RazorpayPaymentGateway;
import com.krunity.HostelManagment.service.StripePaymentGateway;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PaymentGatewayConfig — Factory that resolves the correct PaymentGateway bean.
 *
 * Switching providers = change PAYMENT_PROVIDER env var.
 * No business logic changes required anywhere else.
 *
 * Supported values: razorpay | stripe | mock
 */
@Slf4j
@Configuration
public class PaymentGatewayConfig {

    @Value("${payment.provider:razorpay}")
    private String provider;

    @Value("${payment.razorpay.key-id:rzp_test_placeholder}")
    private String razorpayKeyId;

    @Value("${payment.razorpay.key-secret:placeholder_secret}")
    private String razorpayKeySecret;

    @Value("${payment.razorpay.currency:INR}")
    private String razorpayCurrency;

    @Value("${payment.stripe.api-key:sk_test_placeholder}")
    private String stripeApiKey;

    @Bean
    public PaymentGateway paymentGateway() throws RazorpayException {
        log.info("🔧 Resolving PaymentGateway for provider: '{}'", provider);

        return switch (provider.toLowerCase().trim()) {
            case "razorpay" -> {
                log.info("✅ Using RazorpayPaymentGateway");
                yield new RazorpayPaymentGateway(razorpayKeyId, razorpayKeySecret, razorpayCurrency);
            }
            case "stripe" -> {
                log.info("✅ Using StripePaymentGateway (stub)");
                yield new StripePaymentGateway(stripeApiKey);
            }
            case "mock" -> {
                log.info("✅ Using MockPaymentGateway (test mode)");
                yield new MockPaymentGateway();
            }
            default -> throw new IllegalArgumentException(
                "Unknown payment provider: '" + provider + "'. Supported: razorpay, stripe, mock"
            );
        };
    }
}
