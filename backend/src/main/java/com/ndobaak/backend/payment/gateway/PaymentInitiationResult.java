package com.ndobaak.backend.payment.gateway;

public record PaymentInitiationResult(
        String providerReference,
        String authorizationUrl,
        String status
) {
}
