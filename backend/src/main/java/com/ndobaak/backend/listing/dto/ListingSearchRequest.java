package com.ndobaak.backend.listing.dto;

import com.ndobaak.backend.common.enums.RoomType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class ListingSearchRequest {

    private String keyword;
    private RoomType roomType;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private Double maxDistance;
    private Set<String> amenities;
    private Double minRating;
    private LocalDate availableBefore;
    private String sortBy;
    private Double userLat;
    private Double userLng;
    private int page = 0;
    private int size = 10;
}
