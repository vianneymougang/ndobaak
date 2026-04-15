package com.ndobaak.backend.rentalcycle.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KeyHandoverResponse(
        Long id,
        Long bookingId,
        Long moveInInspectionId,
        LocalDateTime handoverDate,
        boolean landlordConfirmed,
        boolean tenantConfirmed,
        LocalDateTime landlordConfirmedAt,
        LocalDateTime tenantConfirmedAt,
        boolean transferInitiated,
        LocalDateTime transferDate,
        String observations,
        LocalDateTime createdAt
) {
}
