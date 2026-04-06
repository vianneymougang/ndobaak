package com.ndobaak.backend.rentalcycle.dto.request;

import com.ndobaak.backend.common.enums.DocumentStatus;
import jakarta.validation.constraints.NotNull;

public record ValidationDecisionRequest(
        @NotNull(message = "Le statut de validation est requis")
        DocumentStatus status,
        
        String comment
) {
}
