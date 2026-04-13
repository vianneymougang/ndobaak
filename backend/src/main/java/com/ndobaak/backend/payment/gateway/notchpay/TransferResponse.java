package com.ndobaak.backend.payment.gateway.notchpay;

public record TransferResponse(
        String reference,
        String status,
        boolean success
) {
}
