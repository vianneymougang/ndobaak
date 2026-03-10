package com.ndobaak.backend.listing.mapper;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.listing.dto.ListingResponse;
import com.ndobaak.backend.listing.entity.Amenity;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.entity.Location;
import com.ndobaak.backend.listing.entity.Photo;
import com.ndobaak.backend.media.service.StorageService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListingMapper {

    private final StorageService storageService;

    public ListingMapper(StorageService storageService) {
        this.storageService = storageService;
    }

    public ListingResponse toResponse(Listing listing, boolean maskContact) {
        return ListingResponse.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .roomType(listing.getRoomType())
                .monthlyPrice(listing.getMonthlyPrice())
                .availableRooms(listing.getAvailableRooms())
                .availableDate(listing.getAvailableDate())
                .status(listing.getStatus())
                .rejectionReason(listing.getRejectionReason())
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .location(toLocationResponse(listing.getLocation()))
                .photos(listing.getPhotos().stream().map(this::toPhotoResponse).toList())
                .amenities(listing.getAmenities().stream().map(this::toAmenityResponse).toList())
                .landlord(toLandlordSummary(listing.getLandlord(), maskContact))
                .build();
    }

    private ListingResponse.LocationResponse toLocationResponse(Location location) {
        if (location == null) return null;
        return ListingResponse.LocationResponse.builder()
                .id(location.getId())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .distanceToCampus(location.getDistanceToCampus())
                .neighborhood(location.getNeighborhood())
                .city(location.getCity())
                .build();
    }

    private ListingResponse.PhotoResponse toPhotoResponse(Photo photo) {
        return ListingResponse.PhotoResponse.builder()
                .id(photo.getId())
                .url(storageService.resolvePublicUrl(photo.getUrl()))
                .thumbnailUrl(storageService.resolvePublicUrl(photo.getThumbnailUrl()))
                .mediumUrl(storageService.resolvePublicUrl(photo.getMediumUrl()))
                .displayOrder(photo.getDisplayOrder())
                .build();
    }

    private ListingResponse.AmenityResponse toAmenityResponse(Amenity amenity) {
        return ListingResponse.AmenityResponse.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .icon(amenity.getIcon())
                .build();
    }

    private ListingResponse.LandlordSummary toLandlordSummary(User landlord, boolean maskContact) {
        if (landlord == null) return null;
        return ListingResponse.LandlordSummary.builder()
                .id(landlord.getId())
                .firstName(landlord.getFirstName())
                .lastName(landlord.getLastName())
                .profilePhotoUrl(storageService.resolvePublicUrl(landlord.getProfilePhotoUrl()))
                .phone(maskContact ? maskPhone(landlord.getPhone()) : landlord.getPhone())
                .email(maskContact ? maskEmail(landlord.getEmail()) : landlord.getEmail())
                .build();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 2);
    }

    private String maskEmail(String email) {
        if (email == null) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***" + email.substring(atIndex);
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
