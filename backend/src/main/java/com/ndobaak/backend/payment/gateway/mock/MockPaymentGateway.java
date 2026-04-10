package com.ndobaak.backend.payment.gateway.mock;

import com.ndobaak.backend.common.enums.MobileMoneyOperator;
import com.ndobaak.backend.payment.gateway.PaymentGateway;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRequest;
import com.ndobaak.backend.payment.gateway.PaymentInitiationResult;
import com.ndobaak.backend.payment.gateway.PaymentVerificationResult;
import com.ndobaak.backend.payment.gateway.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "app.payment.primary-gateway", havingValue = "mock")
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);
    private static final String PROVIDER_NAME = "mock";

    private final Map<String, PaymentGatewayRequest> pendingPayments = new ConcurrentHashMap<>();

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supports(MobileMoneyOperator operator) {
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public PaymentInitiationResult initiatePayment(PaymentGatewayRequest request) {
        String mockProviderRef = "mock_" + UUID.randomUUID().toString().substring(0, 8);
        pendingPayments.put(request.reference(), request);

        log.info("[MOCK] Payment initiated: reference={}, amount={} {}, provider_ref={}",
                request.reference(), request.amount(), request.currency(), mockProviderRef);

        return new PaymentInitiationResult(mockProviderRef, null, "pending");
    }

    @Override
    public PaymentInitiationResult initiateRedirectPayment(String reference, BigDecimal amount, String currency,
                                                            String payerName, String payerEmail,
                                                            String description, String callbackUrl) {
        String mockProviderRef = "mock_redirect_" + UUID.randomUUID().toString().substring(0, 8);
        String mockAuthUrl = callbackUrl + "?mock_ref=" + mockProviderRef + "&description=" + description;

        log.info("[MOCK] Redirect payment initiated: reference={}, amount={} {}, auth_url=[mock]",
                reference, amount, currency);

        return new PaymentInitiationResult(mockProviderRef, mockAuthUrl, "pending");
    }

    @Override
    public void triggerCharge(String providerReference, PaymentGatewayRequest request) {
        log.info("[MOCK] triggerCharge no-op: providerRef={}", providerReference);
    }

    @Override
    public void cancelPayment(String providerReference) {
        log.info("[MOCK] cancelPayment no-op: providerRef={}", providerReference);
    }

    @Override
    public PaymentVerificationResult verifyPayment(String reference) {
        PaymentGatewayRequest original = pendingPayments.get(reference);

        log.info("[MOCK] Payment verification: reference={}, auto-completing", reference);

        return new PaymentVerificationResult(
                reference,
                "mock_verified",
                "complete",
                original != null ? original.amount() : BigDecimal.ZERO,
                "XAF",
                LocalDateTime.now()
        );
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        return "mock-signature".equals(signature);
    }

    @Override
    public WebhookEvent parseWebhookPayload(String payload) {
        return new WebhookEvent(
                "payment.complete",
                payload,
                "complete",
                BigDecimal.ZERO,
                "XAF",
                Map.of()
        );
    }

    public WebhookEvent simulateSuccessWebhook(String reference) {
        PaymentGatewayRequest original = pendingPayments.remove(reference);

        log.info("[MOCK] Simulating success webhook for reference={}", reference);

        return new WebhookEvent(
                "payment.complete",
                reference,
                "complete",
                original != null ? original.amount() : BigDecimal.ZERO,
                "XAF",
                Map.of("simulated", true)
        );
    }
}
