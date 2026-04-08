package com.ndobaak.backend.geo;

import com.ndobaak.backend.common.enums.ReviewStatus;
import com.ndobaak.backend.common.enums.RoomType;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.ReviewRepository;
import com.ndobaak.backend.media.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeoServiceTest {

    @Mock
    private ListingRepository listingRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private StorageService storageService;

    @InjectMocks
    private GeoService geoService;

    @Test
    void findNearbyShouldReturnMappedResults() {
        Object[] row = buildRow(1L, "Studio Eyang", "photos/thumb.webp",
                new BigDecimal("35000"), "STUDIO", "Eyang", 0.15, 3.8855, 11.3922, true);
        when(listingRepository.findNearby(eq(3.8845), eq(11.3912), eq(2.0), eq(50)))
                .thenReturn(List.<Object[]>of(row));
        when(reviewRepository.findAverageRatingsByListingIds(List.of(1L), ReviewStatus.APPROVED))
                .thenReturn(Collections.emptyList());
        when(storageService.resolvePublicUrl("photos/thumb.webp"))
                .thenReturn("https://cdn.example.com/photos/thumb.webp");

        List<NearbyListingResponse> results = geoService.findNearby(3.8845, 11.3912, 2.0);

        assertThat(results).hasSize(1);
        NearbyListingResponse r = results.get(0);
        assertThat(r.id()).isEqualTo(1L);
        assertThat(r.title()).isEqualTo("Studio Eyang");
        assertThat(r.thumbnailUrl()).isEqualTo("https://cdn.example.com/photos/thumb.webp");
        assertThat(r.monthlyPrice()).isEqualByComparingTo(new BigDecimal("35000"));
        assertThat(r.roomType()).isEqualTo(RoomType.STUDIO);
        assertThat(r.verified()).isTrue();
    }

    @Test
    void findNearbyShouldEnrichWithRatings() {
        Object[] row = buildRow(1L, "Rated Place", null,
                new BigDecimal("40000"), "APARTMENT", "Melen", 1.2, 3.8780, 11.3960, true);
        when(listingRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.<Object[]>of(row));
        List<Object[]> ratings = List.<Object[]>of(new Object[]{1L, 4.5, 12L});
        when(reviewRepository.findAverageRatingsByListingIds(List.of(1L), ReviewStatus.APPROVED))
                .thenReturn(ratings);

        List<NearbyListingResponse> results = geoService.findNearby(3.8845, 11.3912, 5.0);

        assertThat(results.get(0).avgRating()).isEqualTo(4.5);
        assertThat(results.get(0).reviewCount()).isEqualTo(12);
    }

    @Test
    void findNearbyShouldHandleNullThumbnail() {
        Object[] row = buildRow(1L, "No Photo", null,
                new BigDecimal("25000"), "SINGLE_ROOM", "Eyang", 0.5, 3.8855, 11.3922, true);
        when(listingRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.<Object[]>of(row));
        when(reviewRepository.findAverageRatingsByListingIds(anyList(), any(ReviewStatus.class)))
                .thenReturn(Collections.emptyList());

        List<NearbyListingResponse> results = geoService.findNearby(3.8845, 11.3912, 2.0);

        assertThat(results.get(0).thumbnailUrl()).isNull();
        verify(storageService, never()).resolvePublicUrl(any());
    }

    @Test
    void findNearbyWithEmptyResultsShouldReturnEmptyList() {
        when(listingRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(reviewRepository.findAverageRatingsByListingIds(anyList(), any(ReviewStatus.class)))
                .thenReturn(Collections.emptyList());

        List<NearbyListingResponse> results = geoService.findNearby(3.8845, 11.3912, 2.0);

        assertThat(results).isEmpty();
    }

    @Test
    void findNearbyShouldRejectInvalidLatitude() {
        assertThatThrownBy(() -> geoService.findNearby(91.0, 11.3912, 2.0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("latitude");

        assertThatThrownBy(() -> geoService.findNearby(-91.0, 11.3912, 2.0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("latitude");
    }

    @Test
    void findNearbyShouldRejectInvalidLongitude() {
        assertThatThrownBy(() -> geoService.findNearby(3.8845, 181.0, 2.0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("longitude");

        assertThatThrownBy(() -> geoService.findNearby(3.8845, -181.0, 2.0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("longitude");
    }

    @Test
    void findNearbyShouldRejectRadiusTooSmall() {
        assertThatThrownBy(() -> geoService.findNearby(3.8845, 11.3912, 0.05))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rayon");
    }

    @Test
    void findNearbyShouldRejectRadiusTooLarge() {
        assertThatThrownBy(() -> geoService.findNearby(3.8845, 11.3912, 15.0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rayon");
    }

    private Object[] buildRow(Long id, String title, String thumbnailUrl,
                               BigDecimal price, String roomType, String neighborhood,
                               Double distanceToCampus, Double lat, Double lng, Boolean verified) {
        return new Object[]{id, title, thumbnailUrl, price, roomType,
                neighborhood, distanceToCampus, lat, lng, verified};
    }
}
