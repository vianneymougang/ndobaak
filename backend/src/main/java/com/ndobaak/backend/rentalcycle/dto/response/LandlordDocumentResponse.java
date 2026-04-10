package com.ndobaak.backend.rentalcycle.dto.response;

import com.ndobaak.backend.common.enums.DocumentStatus;
import com.ndobaak.backend.common.enums.LandlordDocumentType;

import java.time.LocalDateTime;

public record LandlordDocumentResponse(
        Long id,
        Long landlordId,
        LandlordDocumentType type,
        String fileName,
        String storageUrl,
        DocumentStatus status,
        Long validatedById,
        LocalDateTime validatedAt,
        String rejectionComment,
        LocalDateTime createdAt
) {
}
