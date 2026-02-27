package com.ndobaak.backend.listing.dto;

import com.ndobaak.backend.common.enums.RoomType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class CreateListingRequest {

    private String title;
    private String description;
    private RoomType roomType;
    private BigDecimal monthlyPrice;
    private Integer availableRooms;
    private LocalDate availableDate;
    private LocationRequest location;
    private Set<String> amenityNames;
}
