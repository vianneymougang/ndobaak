package com.ndobaak.backend.payment.gateway;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentVerificationResult(
        String reference,
        String providerReference,
        String status,
        BigDecimal amount,
        String currency,
        LocalDateTime completedAt
) {
}
