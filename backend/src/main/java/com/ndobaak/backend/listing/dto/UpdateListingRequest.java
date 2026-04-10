package com.ndobaak.backend.listing.dto;

import com.ndobaak.backend.common.enums.RoomType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class UpdateListingRequest {

    @Size(min = 10, max = 200, message = "Le titre doit contenir entre 10 et 200 caractères.")
    private String title;

    @Size(min = 50, max = 5000, message = "La description doit contenir entre 50 et 5000 caractères.")
    private String description;

    private RoomType roomType;

    @DecimalMin(value = "1", message = "Le prix doit être supérieur à 0.")
    @DecimalMax(value = "10000000", message = "Le prix dépasse la limite autorisée.")
    private BigDecimal monthlyPrice;

    @Min(value = 1, message = "Le nombre de chambres doit être au minimum 1.")
    @Max(value = 100, message = "Le nombre de chambres dépasse la limite autorisée.")
    private Integer availableRooms;

    private LocalDate availableDate;

    @Valid
    private UpdateLocationRequest location;

    @Size(max = 20, message = "Maximum 20 équipements autorisés.")
    private Set<String> amenityNames;
}
