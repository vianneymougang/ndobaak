package com.ndobaak.backend.rentalcycle.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuarantorRequest(
        @NotBlank(message = "Le nom est requis")
        @Size(max = 100)
        String lastName,

        @NotBlank(message = "Le prénom est requis")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Le numéro CNI est requis")
        @Size(max = 50)
        String idCardNumber,

        @NotBlank(message = "Le téléphone est requis")
        @Pattern(regexp = "^\\+237[26][0-9]{8}$", message = "Format téléphone invalide")
        String phone,

        @NotBlank(message = "L'adresse est requise")
        @Size(max = 500)
        String address,

        @NotBlank(message = "Le lien avec le locataire est requis")
        @Size(max = 100)
        String relationshipToTenant
) {
}
