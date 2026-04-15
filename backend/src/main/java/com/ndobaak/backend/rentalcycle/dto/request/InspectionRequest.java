package com.ndobaak.backend.rentalcycle.dto.request;

import com.ndobaak.backend.common.enums.InspectionType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record InspectionRequest(
        @NotNull InspectionType type,
        @NotNull LocalDateTime conductedAt,
        String observations
) {
}
