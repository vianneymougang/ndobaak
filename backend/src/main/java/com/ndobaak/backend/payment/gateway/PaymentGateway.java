package com.ndobaak.backend.payment.gateway;

import com.ndobaak.backend.common.enums.PaymentChannel;

import java.math.BigDecimal;

public interface PaymentGateway {

    String getProviderName();

    boolean supports(PaymentChannel channel);

    boolean isAvailable();

    PaymentInitiationResult initiatePayment(PaymentGatewayRequest request);

    PaymentInitiationResult initiateRedirectPayment(String reference, BigDecimal amount, String currency,
                                                     String payerName, String payerEmail,
                                                     String description, String callbackUrl);

    void triggerCharge(String providerReference, PaymentGatewayRequest request);

    void cancelPayment(String providerReference);

    PaymentVerificationResult verifyPayment(String reference);

    boolean verifyWebhookSignature(String payload, String signature);

    WebhookEvent parseWebhookPayload(String payload);
}
