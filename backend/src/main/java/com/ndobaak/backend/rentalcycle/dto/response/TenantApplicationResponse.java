package com.ndobaak.backend.rentalcycle.dto.response;

import com.ndobaak.backend.common.enums.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TenantApplicationResponse(
        Long id,
        Long bookingId,
        ApplicationStatus status,
        LocalDateTime createdAt,
        LocalDateTime decidedAt,
        String decisionComment,
        List<TenantDocumentResponse> documents,
        List<GuarantorResponse> guarantors
) {
}
