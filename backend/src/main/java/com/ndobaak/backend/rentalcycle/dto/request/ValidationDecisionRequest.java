package com.ndobaak.backend.rentalcycle.dto.request;

import com.ndobaak.backend.common.enums.DocumentStatus;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ValidationDecisionRequest(
        @NotNull(message = "Le statut de validation est requis")
        DocumentStatus status,

        String comment,

        LocalDate startDate,
        LocalDate endDate,
        BigDecimal securityDeposit,
        String specialClauses
) {
}
