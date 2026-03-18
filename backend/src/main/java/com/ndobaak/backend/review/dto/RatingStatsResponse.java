package com.ndobaak.backend.review.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class RatingStatsResponse {

    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> distribution;
}
