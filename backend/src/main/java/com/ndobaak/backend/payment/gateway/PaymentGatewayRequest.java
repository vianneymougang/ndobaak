package com.ndobaak.backend.payment.gateway;

import com.ndobaak.backend.common.enums.PaymentChannel;

import java.math.BigDecimal;

public record PaymentGatewayRequest(
        String reference,
        BigDecimal amount,
        String currency,
        String payerName,
        String payerEmail,
        String phoneNumber,
        PaymentChannel operator,
        String description,
        String callbackUrl
) {
}
