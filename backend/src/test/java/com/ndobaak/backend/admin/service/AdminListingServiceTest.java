package com.ndobaak.backend.admin.service;

import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.dto.ListingResponse;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.mapper.ListingMapper;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminListingServiceTest {

    @Mock
    private ListingRepository listingRepository;
    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private ListingMapper listingMapper;

    @InjectMocks
    private AdminListingService adminListingService;

    @Test
    void getPendingListingsShouldReturnOnlyPendingReview() {
        Listing pending = Listing.builder()
                .title("Pending One").status(ListingStatus.PENDING_REVIEW).build();
        pending.setId(1L);

        when(listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.PENDING_REVIEW))
                .thenReturn(List.of(pending));

        ListingResponse response = ListingResponse.builder().id(1L)
                .photos(java.util.Collections.emptyList()).amenities(java.util.Collections.emptyList()).build();
        when(listingMapper.toResponse(eq(pending), eq(false))).thenReturn(response);

        List<ListingResponse> results = adminListingService.getPendingListings();

        assertThat(results).hasSize(1);
        verify(listingRepository).findByStatusOrderByCreatedAtDesc(ListingStatus.PENDING_REVIEW);
    }

    @Test
    void validateListingShouldChangeStatusToPublished() {
        Listing listing = Listing.builder()
                .title("Test Listing").status(ListingStatus.PENDING_REVIEW).build();
        listing.setId(1L);

        when(listingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(listing));
        when(photoRepository.countByListingId(1L)).thenReturn(5);
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
        when(listingMapper.toResponse(any(), eq(false))).thenReturn(
                ListingResponse.builder().photos(java.util.Collections.emptyList())
                        .amenities(java.util.Collections.emptyList()).build());

        adminListingService.validateListing(1L);

        verify(listingRepository).save(argThat(l ->
                l.getStatus() == ListingStatus.PUBLISHED
        ));
    }

    @Test
    void validateListingNotPendingShouldThrow() {
        Listing listing = Listing.builder()
                .title("Draft").status(ListingStatus.DRAFT).build();
        listing.setId(1L);

        when(listingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> adminListingService.validateListing(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDING_REVIEW");
    }

    @Test
    void validateListingWithTooFewPhotosShouldThrow() {
        Listing listing = Listing.builder()
                .title("Test").status(ListingStatus.PENDING_REVIEW).build();
        listing.setId(1L);

        when(listingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(listing));
        when(photoRepository.countByListingId(1L)).thenReturn(2);

        assertThatThrownBy(() -> adminListingService.validateListing(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("between 3 and 10 photos");
    }

    @Test
    void validateListingNotFoundShouldThrow() {
        when(listingRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminListingService.validateListing(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectListingShouldSetRejectedStatusAndReason() {
        Listing listing = Listing.builder()
                .title("Test").status(ListingStatus.PENDING_REVIEW).build();
        listing.setId(1L);

        when(listingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
        when(listingMapper.toResponse(any(), eq(false))).thenReturn(
                ListingResponse.builder().photos(java.util.Collections.emptyList())
                        .amenities(java.util.Collections.emptyList()).build());

        adminListingService.rejectListing(1L, "Photos de mauvaise qualite");

        verify(listingRepository).save(argThat(l ->
                l.getStatus() == ListingStatus.REJECTED &&
                "Photos de mauvaise qualite".equals(l.getRejectionReason())
        ));
    }

    @Test
    void rejectListingWithoutReasonShouldThrow() {
        assertThatThrownBy(() -> adminListingService.rejectListing(1L, ""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Rejection reason is required");
    }

    @Test
    void rejectListingWithNullReasonShouldThrow() {
        assertThatThrownBy(() -> adminListingService.rejectListing(1L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Rejection reason is required");
    }

    @Test
    void rejectListingNotPendingShouldThrow() {
        Listing listing = Listing.builder()
                .title("Draft").status(ListingStatus.DRAFT).build();
        listing.setId(1L);

        when(listingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> adminListingService.rejectListing(1L, "Some reason"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDING_REVIEW");
    }
}
