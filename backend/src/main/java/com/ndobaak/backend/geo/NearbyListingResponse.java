package com.ndobaak.backend.geo;

import com.ndobaak.backend.common.enums.RoomType;

import java.math.BigDecimal;

public record NearbyListingResponse(
        Long id,
        String title,
        String thumbnailUrl,
        BigDecimal monthlyPrice,
        RoomType roomType,
        String neighborhood,
        Double distanceToCampus,
        Double latitude,
        Double longitude,
        Double avgRating,
        Integer reviewCount,
        boolean verified
) {}
