package com.ndobaak.backend.payment.gateway.notchpay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndobaak.backend.common.enums.MobileMoneyOperator;
import com.ndobaak.backend.payment.gateway.PaymentGateway;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRequest;
import com.ndobaak.backend.payment.gateway.PaymentInitiationResult;
import com.ndobaak.backend.payment.gateway.PaymentVerificationResult;
import com.ndobaak.backend.payment.gateway.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.payment.primary-gateway", havingValue = "notchpay", matchIfMissing = true)
public class NotchPayGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(NotchPayGateway.class);
    private static final String PROVIDER_NAME = "notchpay";

    private final RestClient restClient;
    private final NotchPayProperties properties;
    private final ObjectMapper objectMapper;

    public NotchPayGateway(NotchPayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", properties.getPublicKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supports(MobileMoneyOperator operator) {
        return operator == MobileMoneyOperator.MTN_MOMO || operator == MobileMoneyOperator.ORANGE_MONEY;
    }

    @Override
    public boolean isAvailable() {
        try {
            restClient.get()
                    .uri("/payments")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("NotchPay gateway health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaymentInitiationResult initiatePayment(PaymentGatewayRequest request) {
        log.info("Initiating NotchPay payment: reference={}, amount={} {}",
                request.reference(), request.amount(), request.currency());

        Map<String, Object> body = Map.of(
                "amount", request.amount().intValue(),
                "currency", request.currency(),
                "customer", Map.of(
                        "name", request.payerName(),
                        "email", request.payerEmail(),
                        "phone", request.phoneNumber()
                ),
                "description", request.description(),
                "reference", request.reference(),
                "callback", request.callbackUrl(),
                "locked_currency", "XAF",
                "locked_country", "CM",
                "locked_channel", mapOperatorToChannel(request.operator())
        );

        Map<String, Object> response = restClient.post()
                .uri("/payments")
                .body(body)
                .retrieve()
                .body(Map.class);

        Map<String, Object> transaction = (Map<String, Object>) response.get("transaction");
        String providerRef = transaction != null ? (String) transaction.get("reference") : null;
        String status = transaction != null ? (String) transaction.get("status") : "pending";

        log.info("NotchPay payment initialized: providerRef={}, status={}", providerRef, status);

        return new PaymentInitiationResult(providerRef, null, status);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaymentInitiationResult initiateRedirectPayment(String reference, BigDecimal amount, String currency,
                                                            String payerName, String payerEmail,
                                                            String description, String callbackUrl) {
        log.info("Initiating NotchPay redirect payment: reference={}, amount={} {}", reference, amount, currency);

        Map<String, Object> body = Map.of(
                "amount", amount.intValue(),
                "currency", currency,
                "customer", Map.of(
                        "name", payerName,
                        "email", payerEmail
                ),
                "description", description,
                "reference", reference,
                "callback", callbackUrl
        );

        Map<String, Object> response = restClient.post()
                .uri("/payments")
                .body(body)
                .retrieve()
                .body(Map.class);

        Map<String, Object> transaction = (Map<String, Object>) response.get("transaction");
        String providerRef = transaction != null ? (String) transaction.get("reference") : null;
        String authUrl = transaction != null ? (String) transaction.get("authorization_url") : null;
        String status = transaction != null ? (String) transaction.get("status") : "pending";

        log.info("NotchPay redirect payment initialized: providerRef={}, authUrl={}",
                providerRef, authUrl != null ? "[present]" : "[absent]");

        return new PaymentInitiationResult(providerRef, authUrl, status);
    }

    @Override
    public void triggerCharge(String providerReference, PaymentGatewayRequest request) {
        Map<String, Object> chargeBody = Map.of(
                "channel", mapOperatorToChannel(request.operator()),
                "data", Map.of("phone", request.phoneNumber())
        );
        restClient.post()
                .uri("/payments/{reference}", providerReference)
                .body(chargeBody)
                .retrieve()
                .toBodilessEntity();
        log.info("NotchPay USSD push triggered: providerRef={}, phone={}", providerReference, request.phoneNumber());
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaymentVerificationResult verifyPayment(String reference) {
        log.info("Verifying NotchPay payment: reference={}", reference);

        Map<String, Object> response = restClient.get()
                .uri("/payments/{reference}", reference)
                .retrieve()
                .body(Map.class);

        Map<String, Object> transaction = (Map<String, Object>) response.get("transaction");
        String status = (String) transaction.get("status");
        String providerRef = (String) transaction.get("reference");
        Number amountNum = (Number) transaction.get("amount");
        BigDecimal amount = amountNum != null ? new BigDecimal(amountNum.toString()) : BigDecimal.ZERO;
        String currency = (String) transaction.get("currency");

        LocalDateTime completedAt = "complete".equals(status) ? LocalDateTime.now() : null;

        return new PaymentVerificationResult(reference, providerRef, status, amount, currency, completedAt);
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (properties.getWebhookHash() == null || signature == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    properties.getWebhookHash().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hmacBytes);
            return computed.equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }

    @Override
    public WebhookEvent parseWebhookPayload(String payload) {
        try {
            Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {});
            log.debug("NotchPay webhook payload: {}", payload);
            String type = (String) event.get("type");
            if (type == null) {
                type = (String) event.get("event");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            String reference = data != null ? (String) data.get("reference") : null;
            String status = data != null ? (String) data.get("status") : null;

            Number amountNum = data != null ? (Number) data.get("amount") : null;
            BigDecimal amount = amountNum != null ? new BigDecimal(amountNum.toString()) : BigDecimal.ZERO;
            String currency = data != null ? (String) data.get("currency") : null;

            return new WebhookEvent(type, reference, status, amount, currency, event);
        } catch (Exception e) {
            log.error("Failed to parse webhook payload", e);
            throw new IllegalArgumentException("Invalid webhook payload", e);
        }
    }

    private String mapOperatorToChannel(MobileMoneyOperator operator) {
        return switch (operator) {
            case MTN_MOMO -> "cm.mtn";
            case ORANGE_MONEY -> "cm.orange";
        };
    }
}
