package com.ndobaak.backend.rentalcycle.dto.request;

import com.ndobaak.backend.common.enums.ElementCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InspectionItemRequest(
        @NotBlank @Size(max = 100) String location,
        @NotBlank @Size(max = 100) String element,
        @NotNull ElementCondition condition,
        String observations
) {
}
