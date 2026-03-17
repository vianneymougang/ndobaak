package com.ndobaak.backend.payment.gateway;

import java.math.BigDecimal;
import java.util.Map;

public record WebhookEvent(
        String type,
        String reference,
        String status,
        BigDecimal amount,
        String currency,
        Map<String, Object> rawData
) {
}
