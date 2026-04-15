package com.ndobaak.backend.payment.gateway.notchpay;

import java.math.BigDecimal;

public record TransferRequest(
        BigDecimal amount,
        String currency,
        String recipientName,
        String recipientPhone,
        String description
) {
}
