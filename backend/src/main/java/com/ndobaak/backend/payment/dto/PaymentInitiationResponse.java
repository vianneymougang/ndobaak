package com.ndobaak.backend.payment.dto;

public record PaymentInitiationResponse(
        String authorizationUrl,
        String reference
) {
}
