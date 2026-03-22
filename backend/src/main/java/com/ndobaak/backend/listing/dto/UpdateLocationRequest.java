package com.ndobaak.backend.listing.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLocationRequest {

    @Size(max = 500, message = "L'adresse dépasse la longueur autorisée.")
    private String address;

    @DecimalMin(value = "-90", message = "Latitude invalide.")
    @DecimalMax(value = "90", message = "Latitude invalide.")
    private Double latitude;

    @DecimalMin(value = "-180", message = "Longitude invalide.")
    @DecimalMax(value = "180", message = "Longitude invalide.")
    private Double longitude;

    @Size(max = 200, message = "Le quartier dépasse la longueur autorisée.")
    private String neighborhood;

    @Size(max = 100, message = "La ville dépasse la longueur autorisée.")
    private String city;
}
