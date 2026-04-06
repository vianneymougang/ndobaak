package com.ndobaak.backend.rentalcycle.dto.response;

import com.ndobaak.backend.common.enums.TenantDocumentType;

import java.time.LocalDateTime;

public record TenantDocumentResponse(
        Long id,
        TenantDocumentType type,
        String fileName,
        String storageUrl,
        LocalDateTime uploadedAt
) {
}
