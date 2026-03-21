package com.ndobaak.backend.booking.mapper;

import com.ndobaak.backend.booking.dto.BookingResponse;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.entity.Photo;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .listing(toListingSummary(booking.getListing()))
                .student(toStudentSummary(booking))
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .monthlyRent(booking.getMonthlyRent())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .cancellationReason(booking.getCancellationReason())
                .landlordResponseDate(booking.getLandlordResponseDate())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private BookingResponse.ListingSummary toListingSummary(Listing listing) {
        if (listing == null) return null;
        String thumbnailUrl = listing.getPhotos().stream()
                .min(Comparator.comparingInt(Photo::getDisplayOrder))
                .map(Photo::getThumbnailUrl)
                .orElse(null);
        String address = listing.getLocation() != null ? listing.getLocation().getAddress() : null;
        return BookingResponse.ListingSummary.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .address(address)
                .thumbnailUrl(thumbnailUrl)
                .monthlyPrice(listing.getMonthlyPrice())
                .build();
    }

    private BookingResponse.StudentSummary toStudentSummary(Booking booking) {
        if (booking.getStudent() == null) return null;
        return BookingResponse.StudentSummary.builder()
                .id(booking.getStudent().getId())
                .firstName(booking.getStudent().getFirstName())
                .lastName(booking.getStudent().getLastName())
                .profilePhotoUrl(booking.getStudent().getProfilePhotoUrl())
                .build();
    }
}
