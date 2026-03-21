package com.ndobaak.backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndobaak.backend.common.enums.MobileMoneyOperator;
import com.ndobaak.backend.common.enums.PaymentMethod;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.PaymentType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    private Long id;
    private Long bookingId;
    private BigDecimal amount;
    private BigDecimal commission;
    private BigDecimal totalCharged;
    private PaymentStatus status;
    private PaymentType paymentType;
    private PaymentMethod paymentMethod;
    private MobileMoneyOperator operator;
    private String transactionId;
    private String authorizationUrl;
    private String providerName;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
