package com.ndobaak.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndobaak.backend.common.enums.PaymentChannel;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.PaymentType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminTransactionResponse {
    private Long id;
    private BigDecimal amount;
    private PaymentType paymentType;
    private PaymentStatus status;
    private String payerName;
    private String phoneNumber;
    private PaymentChannel operator;
    private String providerName;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private Long bookingId;
    private boolean anomaly;
    private String anomalyReason;
}
