package com.ndobaak.backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndobaak.backend.common.enums.ScheduleFrequency;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentScheduleResponse {

    private Long id;
    private Long bookingId;
    private BigDecimal totalAmount;
    private ScheduleFrequency frequency;
    private int numberOfInstallments;
    private int paidInstallments;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private List<InstallmentResponse> installments;
    private LocalDateTime createdAt;
}
