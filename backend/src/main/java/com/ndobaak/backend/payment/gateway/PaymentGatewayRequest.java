package com.ndobaak.backend.payment.gateway;

import com.ndobaak.backend.common.enums.MobileMoneyOperator;

import java.math.BigDecimal;

public record PaymentGatewayRequest(
        String reference,
        BigDecimal amount,
        String currency,
        String payerName,
        String payerEmail,
        String phoneNumber,
        MobileMoneyOperator operator,
        String description,
        String callbackUrl
) {
}
