package com.ndobaak.backend.rentalcycle.dto.response;

public record GuarantorResponse(
        Long id,
        String lastName,
        String firstName,
        String idCardNumber,
        String phone,
        String address,
        String relationshipToTenant
) {
}
