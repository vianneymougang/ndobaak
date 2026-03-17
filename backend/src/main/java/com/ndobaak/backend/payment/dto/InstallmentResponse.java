package com.ndobaak.backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndobaak.backend.common.enums.InstallmentStatus;
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
public class InstallmentResponse {

    private Long id;
    private int installmentNumber;
    private BigDecimal amount;
    private BigDecimal commission;
    private LocalDate dueDate;
    private InstallmentStatus status;
    private LocalDateTime paidAt;
}
