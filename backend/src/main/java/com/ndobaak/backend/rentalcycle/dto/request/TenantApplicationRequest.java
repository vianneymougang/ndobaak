package com.ndobaak.backend.rentalcycle.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TenantApplicationRequest(
        @NotEmpty(message = "Au moins un garant est requis")
        @Valid
        List<GuarantorRequest> guarantors
) {
}
