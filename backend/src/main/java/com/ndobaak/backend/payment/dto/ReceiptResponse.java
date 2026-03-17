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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReceiptResponse {

    private String transactionId;
    private LocalDateTime paymentDate;
    private String payerName;
    private String payerPhone;
    private String listingTitle;
    private String listingAddress;
    private LocalDate bookingStartDate;
    private LocalDate bookingEndDate;
    private BigDecimal rentAmount;
    private BigDecimal commission;
    private BigDecimal totalCharged;
    private PaymentMethod paymentMethod;
    private MobileMoneyOperator operator;
    private PaymentStatus status;
    private PaymentType paymentType;
}
