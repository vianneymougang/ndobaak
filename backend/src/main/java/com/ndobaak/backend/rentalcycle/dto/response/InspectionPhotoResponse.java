package com.ndobaak.backend.rentalcycle.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InspectionPhotoResponse(
        Long id,
        Long inspectionId,
        Long itemId,
        String storageUrl,
        String caption,
        LocalDateTime createdAt
) {
}
