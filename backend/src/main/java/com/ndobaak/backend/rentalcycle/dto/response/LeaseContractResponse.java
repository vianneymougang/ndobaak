package com.ndobaak.backend.rentalcycle.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndobaak.backend.common.enums.LeaseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeaseContractResponse(
        Long id,
        Long bookingId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        BigDecimal agencyFee,
        String specialClauses,
        String pdfUrl,
        LeaseStatus status,
        LocalDateTime landlordSignedAt,
        LocalDateTime tenantSignedAt,
        LocalDate taxRegistrationDate,
        String taxReference,
        Long generatedById,
        LocalDateTime createdAt
) {
}
