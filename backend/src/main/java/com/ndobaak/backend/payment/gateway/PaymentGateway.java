package com.ndobaak.backend.payment.gateway;

import com.ndobaak.backend.common.enums.MobileMoneyOperator;

public interface PaymentGateway {

    String getProviderName();

    boolean supports(MobileMoneyOperator operator);

    boolean isAvailable();

    PaymentInitiationResult initiatePayment(PaymentGatewayRequest request);

    PaymentVerificationResult verifyPayment(String reference);

    boolean verifyWebhookSignature(String payload, String signature);

    WebhookEvent parseWebhookPayload(String payload);
}
