package com.ndobaak.backend.listing.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationRequest {

    @NotBlank(message = "L'adresse est obligatoire.")
    @Size(max = 500, message = "L'adresse dépasse la longueur autorisée.")
    private String address;

    @NotNull(message = "La latitude est obligatoire.")
    @DecimalMin(value = "-90", message = "Latitude invalide.")
    @DecimalMax(value = "90", message = "Latitude invalide.")
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire.")
    @DecimalMin(value = "-180", message = "Longitude invalide.")
    @DecimalMax(value = "180", message = "Longitude invalide.")
    private Double longitude;

    @Size(max = 200, message = "Le quartier dépasse la longueur autorisée.")
    private String neighborhood;

    @Size(max = 100, message = "La ville dépasse la longueur autorisée.")
    private String city;
}
