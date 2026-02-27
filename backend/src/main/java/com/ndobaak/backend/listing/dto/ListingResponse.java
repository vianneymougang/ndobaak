package com.ndobaak.backend.listing.dto;

import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.common.enums.RoomType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ListingResponse {

    private Long id;
    private String title;
    private String description;
    private RoomType roomType;
    private BigDecimal monthlyPrice;
    private Integer availableRooms;
    private LocalDate availableDate;
    private ListingStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LocationResponse location;
    private List<PhotoResponse> photos;
    private List<AmenityResponse> amenities;

    private LandlordSummary landlord;

    @Getter
    @Builder
    public static class LocationResponse {
        private Long id;
        private String address;
        private Double latitude;
        private Double longitude;
        private Double distanceToCampus;
        private String neighborhood;
        private String city;
    }

    @Getter
    @Builder
    public static class PhotoResponse {
        private Long id;
        private String url;
        private String thumbnailUrl;
        private String mediumUrl;
        private Integer displayOrder;
    }

    @Getter
    @Builder
    public static class AmenityResponse {
        private Long id;
        private String name;
        private String icon;
    }

    @Getter
    @Builder
    public static class LandlordSummary {
        private Long id;
        private String firstName;
        private String lastName;
        private String profilePhotoUrl;
        private String phone;
        private String email;
    }
}
