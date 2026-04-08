package com.ndobaak.backend.listing.dto;

import com.ndobaak.backend.common.enums.RoomType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class ListingSearchResponse {

    private Long id;
    private String title;
    private String thumbnailUrl;
    private BigDecimal monthlyPrice;
    private RoomType roomType;
    private String neighborhood;
    private Double distanceToCampus;
    private Double avgRating;
    private Integer reviewCount;
    private List<String> amenities;
    private boolean verified;
    private Double latitude;
    private Double longitude;
    private Integer availableRooms;
}
