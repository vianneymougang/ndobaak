package com.ndobaak.backend.listing.service;

import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.common.enums.RoomType;
import com.ndobaak.backend.listing.dto.ListingSearchRequest;
import com.ndobaak.backend.listing.dto.ListingSearchResponse;
import com.ndobaak.backend.listing.dto.SearchPageResponse;
import com.ndobaak.backend.listing.entity.Amenity;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.entity.Location;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingSearchServiceTest {

    @Mock
    private ListingRepository listingRepository;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ListingSearchService searchService;

    @Test
    void searchShouldReturnPaginatedResults() {
        Listing listing = buildPublishedListing(1L, "Studio Eyang", new BigDecimal("35000"), RoomType.STUDIO);
        when(listingRepository.findAll(any(Specification.class))).thenReturn(List.of(listing));
        when(reviewRepository.findAverageRatingsByListingIds(List.of(1L)))
                .thenReturn(Collections.emptyList());

        ListingSearchRequest request = new ListingSearchRequest();
        request.setPage(0);
        request.setSize(10);

        SearchPageResponse<ListingSearchResponse> result = searchService.search(request);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(0);
    }

    @Test
    void searchShouldFilterByMaxDistance() {
        Listing nearListing = buildPublishedListing(1L, "Near", new BigDecimal("35000"), RoomType.STUDIO);
        nearListing.setLocation(Location.builder()
                .address("Eyang").latitude(3.849).longitude(11.503).distanceToCampus(0.15).build());

        Listing farListing = buildPublishedListing(2L, "Far", new BigDecimal("30000"), RoomType.APARTMENT);
        farListing.setLocation(Location.builder()
                .address("Douala").latitude(4.05).longitude(9.7).distanceToCampus(200.0).build());

        when(listingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(nearListing, farListing));
        when(reviewRepository.findAverageRatingsByListingIds(anyList()))
                .thenReturn(Collections.emptyList());

        ListingSearchRequest request = new ListingSearchRequest();
        request.setMaxDistance(5.0);
        request.setPage(0);
        request.setSize(10);

        SearchPageResponse<ListingSearchResponse> result = searchService.search(request);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Near");
    }

    @Test
    void searchShouldSortByPriceAsc() {
        Listing cheapListing = buildPublishedListing(1L, "Cheap", new BigDecimal("25000"), RoomType.SINGLE_ROOM);
        Listing expensiveListing = buildPublishedListing(2L, "Expensive", new BigDecimal("75000"), RoomType.APARTMENT);

        when(listingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(expensiveListing, cheapListing));
        when(reviewRepository.findAverageRatingsByListingIds(anyList()))
                .thenReturn(Collections.emptyList());

        ListingSearchRequest request = new ListingSearchRequest();
        request.setSortBy("PRICE_ASC");
        request.setPage(0);
        request.setSize(10);

        SearchPageResponse<ListingSearchResponse> result = searchService.search(request);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getMonthlyPrice()).isLessThan(result.getContent().get(1).getMonthlyPrice());
    }

    @Test
    void searchShouldSortByPriceDesc() {
        Listing cheapListing = buildPublishedListing(1L, "Cheap", new BigDecimal("25000"), RoomType.SINGLE_ROOM);
        Listing expensiveListing = buildPublishedListing(2L, "Expensive", new BigDecimal("75000"), RoomType.APARTMENT);

        when(listingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(cheapListing, expensiveListing));
        when(reviewRepository.findAverageRatingsByListingIds(anyList()))
                .thenReturn(Collections.emptyList());

        ListingSearchRequest request = new ListingSearchRequest();
        request.setSortBy("PRICE_DESC");
        request.setPage(0);
        request.setSize(10);

        SearchPageResponse<ListingSearchResponse> result = searchService.search(request);

        assertThat(result.getContent().get(0).getMonthlyPrice())
                .isGreaterThan(result.getContent().get(1).getMonthlyPrice());
    }

    @Test
    void searchShouldFilterByAmenities() {
        Amenity wifi = new Amenity();
        wifi.setId(1L);
        wifi.setName("WiFi");

        Amenity water = new Amenity();
        water.setId(2L);
        water.setName("Running Water");

        Listing withBoth = buildPublishedListing(1L, "Full", new BigDecimal("50000"), RoomType.STUDIO);
        withBoth.setAmenities(new HashSet<>(Set.of(wifi, water)));

        Listing withOnlyWifi = buildPublishedListing(2L, "Partial", new BigDecimal("40000"), RoomType.SINGLE_ROOM);
        withOnlyWifi.setAmenities(new HashSet<>(Set.of(wifi)));

        when(listingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(withBoth, withOnlyWifi));
        when(reviewRepository.findAverageRatingsByListingIds(anyList()))
                .thenReturn(Collections.emptyList());

        ListingSearchRequest request = new ListingSearchRequest();
        request.setAmenities(Set.of("WiFi", "Running Water"));
        request.setPage(0);
        request.setSize(10);

        SearchPageResponse<ListingSearchResponse> result = searchService.search(request);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Full");
    }

    @Test
    void searchPaginationShouldWork() {
        List<Listing> listings = List.of(
                buildPublishedListing(1L, "L1", new BigDecimal("30000"), RoomType.STUDIO),
                buildPublishedListing(2L, "L2", new BigDecimal("35000"), RoomType.STUDIO),
                buildPublishedListing(3L, "L3", new BigDecimal("40000"), RoomType.STUDIO)
        );

        when(listingRepository.findAll(any(Specification.class))).thenReturn(listings);
        when(reviewRepository.findAverageRatingsByListingIds(anyList()))
                .thenReturn(Collections.emptyList());

        ListingSearchRequest request = new ListingSearchRequest();
        request.setPage(0);
        request.setSize(2);

        SearchPageResponse<ListingSearchResponse> page0 = searchService.search(request);

        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getTotalElements()).isEqualTo(3);
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page0.getPage()).isEqualTo(0);

        request.setPage(1);
        SearchPageResponse<ListingSearchResponse> page1 = searchService.search(request);

        assertThat(page1.getContent()).hasSize(1);
    }

    @Test
    void searchWithRatingsShouldIncludeAverages() {
        Listing listing = buildPublishedListing(1L, "Rated", new BigDecimal("50000"), RoomType.STUDIO);

        when(listingRepository.findAll(any(Specification.class))).thenReturn(List.of(listing));

        List<Object[]> ratingResults = new ArrayList<>();
        ratingResults.add(new Object[]{1L, 4.5, 10L});
        when(reviewRepository.findAverageRatingsByListingIds(List.of(1L)))
                .thenReturn(ratingResults);

        ListingSearchRequest request = new ListingSearchRequest();
        request.setPage(0);
        request.setSize(10);

        SearchPageResponse<ListingSearchResponse> result = searchService.search(request);

        assertThat(result.getContent().get(0).getAvgRating()).isEqualTo(4.5);
        assertThat(result.getContent().get(0).getReviewCount()).isEqualTo(10);
    }

    @Test
    void searchShouldFilterByMinRating() {
        Listing listing = buildPublishedListing(1L, "Good", new BigDecimal("50000"), RoomType.STUDIO);

        when(listingRepository.findAll(any(Specification.class))).thenReturn(List.of(listing));

        List<Object[]> ratingResults = new ArrayList<>();
        ratingResults.add(new Object[]{1L, 3.0, 5L});
        when(reviewRepository.findAverageRatingsByListingIds(List.of(1L)))
                .thenReturn(ratingResults);

        ListingSearchRequest request = new ListingSearchRequest();
        request.setMinRating(4.0);
        request.setPage(0);
        request.setSize(10);

        SearchPageResponse<ListingSearchResponse> result = searchService.search(request);

        assertThat(result.getContent()).isEmpty();
    }

    private Listing buildPublishedListing(Long id, String title, BigDecimal price, RoomType roomType) {
        Listing listing = Listing.builder()
                .title(title)
                .monthlyPrice(price)
                .roomType(roomType)
                .availableRooms(1)
                .status(ListingStatus.PUBLISHED)
                .amenities(new HashSet<>())
                .photos(new ArrayList<>())
                .build();
        listing.setId(id);
        return listing;
    }
}
