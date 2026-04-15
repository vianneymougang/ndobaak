package com.ndobaak.backend.rentalcycle.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GenerateLeaseRequest(
        @NotNull(message = "La date de début est requise")
        LocalDate startDate,

        @NotNull(message = "La date de fin est requise")
        LocalDate endDate,

        @NotNull(message = "Le montant de la caution est requis")
        BigDecimal securityDeposit,

        String specialClauses
) {
}
