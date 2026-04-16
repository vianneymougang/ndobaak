package com.ndobaak.backend.payment.gateway.notchpay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndobaak.backend.common.enums.PaymentChannel;
import com.ndobaak.backend.payment.gateway.PaymentGateway;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRequest;
import com.ndobaak.backend.payment.gateway.PaymentInitiationResult;
import com.ndobaak.backend.payment.gateway.PaymentVerificationResult;
import com.ndobaak.backend.payment.gateway.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@Component
public class NotchPayGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(NotchPayGateway.class);
    private static final String PROVIDER_NAME = "notchpay";

    private final RestClient restClient;
    private final NotchPayProperties properties;
    private final ObjectMapper objectMapper;

    public NotchPayGateway(NotchPayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", properties.getPublicKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supports(PaymentChannel channel) {
        return channel == PaymentChannel.MTN_MOMO || channel == PaymentChannel.ORANGE_MONEY;
    }

    @Override
    public boolean isAvailable() {
        if (properties.getPublicKey() == null || properties.getPublicKey().isBlank()) {
            log.warn("NotchPay gateway disabled: public key not configured");
            return false;
        }
        try {
            restClient.get()
                    .uri("/")
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
                "locked_channel", mapChannelToNotchPay(request.operator())
        );

        Map<String, Object> response = executePost("/payments", body);

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
                "callback", callbackUrl,
                "locked_currency", "XAF",
                "locked_country", "CM"
        );

        Map<String, Object> response = executePost("/payments", body);

        Map<String, Object> transaction = (Map<String, Object>) response.get("transaction");
        String providerRef = transaction != null ? (String) transaction.get("reference") : null;
        String authUrl = (String) response.get("authorization_url");
        String status = transaction != null ? (String) transaction.get("status") : "pending";

        log.info("NotchPay redirect payment initialized: providerRef={}, authUrl={}",
                providerRef, authUrl != null ? "[present]" : "[absent]");

        return new PaymentInitiationResult(providerRef, authUrl, status);
    }

    @Override
    public void cancelPayment(String providerReference) {
        try {
            restClient.delete()
                    .uri("/payments/{reference}", providerReference)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) ->
                            log.warn("NotchPay cancel returned error status: {}", res.getStatusCode()))
                    .toBodilessEntity();
            log.info("NotchPay payment cancelled: providerRef={}", providerReference);
        } catch (Exception e) {
            log.warn("NotchPay cancel failed (non-critical): providerRef={}, error={}", providerReference, e.getMessage());
        }
    }

    @Override
    public void triggerCharge(String providerReference, PaymentGatewayRequest request) {
        String phone = request.phoneNumber().startsWith("+") ? request.phoneNumber() : "+" + request.phoneNumber();
        Map<String, Object> chargeBody = Map.of(
                "channel", mapChannelToNotchPay(request.operator()),
                "data", Map.of("phone", phone)
        );
        executePost("/payments/" + providerReference, chargeBody);
        log.info("NotchPay charge triggered: providerRef={}, phone={}", providerReference, phone);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaymentVerificationResult verifyPayment(String reference) {
        log.info("Verifying NotchPay payment: reference={}", reference);

        Map<String, Object> response = executeGet("/payments/{reference}", reference);

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
            return MessageDigest.isEqual(
                    computed.toLowerCase().getBytes(StandardCharsets.UTF_8),
                    signature.toLowerCase().getBytes(StandardCharsets.UTF_8));
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> executePost(String uri, Map<String, Object> body) {
        try {
            return restClient.post()
                    .uri(uri)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.error("NotchPay 4xx error: status={}, uri={}", res.getStatusCode(), uri);
                        throw new RuntimeException("NotchPay client error: " + res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("NotchPay 5xx error: status={}, uri={}", res.getStatusCode(), uri);
                        throw new RuntimeException("NotchPay server error: " + res.getStatusCode());
                    })
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            log.error("NotchPay request failed: uri={}, status={}, body={}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erreur de communication avec le service de paiement.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeGet(String uri, Object... uriVars) {
        try {
            return restClient.get()
                    .uri(uri, uriVars)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.error("NotchPay 4xx error: status={}, uri={}", res.getStatusCode(), uri);
                        throw new RuntimeException("NotchPay client error: " + res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("NotchPay 5xx error: status={}, uri={}", res.getStatusCode(), uri);
                        throw new RuntimeException("NotchPay server error: " + res.getStatusCode());
                    })
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            log.error("NotchPay request failed: uri={}, status={}, body={}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erreur de communication avec le service de paiement.", e);
        }
    }

    @SuppressWarnings("unchecked")
    public TransferResponse initiateTransfer(TransferRequest request) {
        log.info("Initiating NotchPay transfer: recipient={}, amount={} {}",
                request.recipientPhone(), request.amount(), request.currency());

        Map<String, Object> body = Map.of(
                "amount", request.amount().intValue(),
                "currency", request.currency(),
                "recipient", Map.of(
                        "name", request.recipientName(),
                        "phone", request.recipientPhone()
                ),
                "description", request.description()
        );

        if (properties.getPrivateKey() == null || properties.getPrivateKey().isBlank()) {
            throw new RuntimeException("NotchPay private key non configurée — virement impossible.");
        }

        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(Duration.ofSeconds(30));

            RestClient transferClient = RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .requestFactory(requestFactory)
                    .defaultHeader("Authorization", properties.getPublicKey())
                    .defaultHeader("X-Grant", properties.getPrivateKey())
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> response = transferClient.post()
                    .uri("/transfers")
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.error("NotchPay transfer 4xx: status={}", res.getStatusCode());
                        throw new RuntimeException("NotchPay transfer client error: " + res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("NotchPay transfer 5xx: status={}", res.getStatusCode());
                        throw new RuntimeException("NotchPay transfer server error: " + res.getStatusCode());
                    })
                    .body(Map.class);

            Map<String, Object> transfer = (Map<String, Object>) response.get("transfer");
            String reference = transfer != null ? (String) transfer.get("reference") : null;
            String status = transfer != null ? (String) transfer.get("status") : "pending";

            log.info("NotchPay transfer initiated: reference={}, status={}", reference, status);
            return new TransferResponse(reference, status, true);
        } catch (RestClientResponseException e) {
            log.error("NotchPay transfer failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erreur lors du virement NotchPay: " + e.getResponseBodyAsString(), e);
        }
    }

    private String mapChannelToNotchPay(PaymentChannel channel) {
        return switch (channel) {
            case MTN_MOMO -> "cm.mtn";
            case ORANGE_MONEY -> "cm.orange";
            case CARD -> "card";
            case USSD -> "cm.mtn.ussd";
            case QR_CODE -> "qr";
        };
    }

}
