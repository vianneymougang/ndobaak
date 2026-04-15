package com.ndobaak.backend.rentalcycle.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndobaak.backend.common.enums.InspectionStatus;
import com.ndobaak.backend.common.enums.InspectionType;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PropertyInspectionResponse(
        Long id,
        Long bookingId,
        InspectionType type,
        LocalDateTime conductedAt,
        Long agentId,
        InspectionStatus status,
        boolean landlordSigned,
        boolean tenantSigned,
        LocalDateTime landlordSignedAt,
        LocalDateTime tenantSignedAt,
        String observations,
        List<InspectionItemResponse> items,
        List<InspectionPhotoResponse> photos,
        LocalDateTime createdAt
) {
}
