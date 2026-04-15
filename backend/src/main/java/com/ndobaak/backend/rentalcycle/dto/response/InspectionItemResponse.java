package com.ndobaak.backend.rentalcycle.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndobaak.backend.common.enums.ElementCondition;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InspectionItemResponse(
        Long id,
        String location,
        String element,
        ElementCondition condition,
        String observations,
        LocalDateTime createdAt
) {
}
