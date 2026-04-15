package com.ndobaak.backend.rentalcycle.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record KeyHandoverRequest(
        @NotNull LocalDateTime handoverDate,
        String observations
) {
}
