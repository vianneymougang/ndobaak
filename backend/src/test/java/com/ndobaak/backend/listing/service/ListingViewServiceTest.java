package com.ndobaak.backend.listing.service;

import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.dashboard.dto.DailyViewsDTO;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.entity.ListingView;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.ListingViewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingViewServiceTest {

    @Mock
    private ListingViewRepository listingViewRepository;
    @Mock
    private ListingRepository listingRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ListingViewService listingViewService;

    @Test
    void recordViewShouldSaveViewForAuthenticatedUserWhenNoDuplicateToday() {
        Long listingId = 1L;
        Long userId = 42L;
        LocalDate today = LocalDate.now();

        when(listingViewRepository.existsByListingIdAndUserIdAndViewedAt(listingId, userId, today)).thenReturn(false);
        Listing listing = Listing.builder().build();
        listing.setId(listingId);
        when(listingRepository.getReferenceById(listingId)).thenReturn(listing);
        when(userRepository.getReferenceById(userId)).thenReturn(null);
        when(listingViewRepository.save(any(ListingView.class))).thenAnswer(inv -> inv.getArgument(0));

        listingViewService.recordView(listingId, userId, null);

        ArgumentCaptor<ListingView> captor = ArgumentCaptor.forClass(ListingView.class);
        verify(listingViewRepository).save(captor.capture());
        assertThat(captor.getValue().getViewedAt()).isEqualTo(today);
    }

    @Test
    void recordViewShouldNotSaveWhenAuthenticatedUserAlreadyViewedToday() {
        Long listingId = 1L;
        Long userId = 42L;
        LocalDate today = LocalDate.now();

        when(listingViewRepository.existsByListingIdAndUserIdAndViewedAt(listingId, userId, today)).thenReturn(true);

        listingViewService.recordView(listingId, userId, null);

        verify(listingViewRepository, never()).save(any());
    }

    @Test
    void recordViewShouldNotSaveWhenAnonymousIpAlreadyViewedToday() {
        Long listingId = 1L;
        String ip = "192.168.1.50";
        LocalDate today = LocalDate.now();

        when(listingViewRepository.existsByListingIdAndViewerIpAndViewedAt(listingId, ip, today)).thenReturn(true);

        listingViewService.recordView(listingId, null, ip);

        verify(listingViewRepository, never()).save(any());
    }

    @Test
    void getViewsInPeriodShouldDelegateToRepository() {
        Long listingId = 5L;
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 19);

        when(listingViewRepository.countByListingIdAndViewedAtBetween(listingId, from, to)).thenReturn(42L);

        long result = listingViewService.getViewsInPeriod(listingId, from, to);

        assertThat(result).isEqualTo(42L);
    }

    @Test
    void getViewsForLandlordListingsShouldReturnEmptyMapWhenNoListings() {
        Long landlordId = 10L;
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 19);

        when(listingRepository.findIdsByLandlordId(landlordId)).thenReturn(List.of());

        Map<Long, Long> result = listingViewService.getViewsForLandlordListings(landlordId, from, to);

        assertThat(result).isEmpty();
        verify(listingViewRepository, never()).countViewsByListingIdsAndPeriod(any(), any(), any());
    }

    @Test
    void getDailyViewsForLandlordShouldReturnEmptyListWhenNoListings() {
        Long landlordId = 10L;
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 19);

        when(listingRepository.findIdsByLandlordId(landlordId)).thenReturn(List.of());

        List<DailyViewsDTO> result = listingViewService.getDailyViewsForLandlord(landlordId, from, to);

        assertThat(result).isEmpty();
        verify(listingViewRepository, never()).countDailyViewsByListingIdsAndPeriod(any(), any(), any());
    }

    @Test
    void getDailyViewsForLandlordShouldMapRepositoryRowsToDTOs() {
        Long landlordId = 10L;
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 3);
        List<Long> listingIds = List.of(1L, 2L);

        List<Object[]> rows = List.of(
                new Object[]{LocalDate.of(2026, 3, 1), 5L},
                new Object[]{LocalDate.of(2026, 3, 2), 8L}
        );

        when(listingRepository.findIdsByLandlordId(landlordId)).thenReturn(listingIds);
        when(listingViewRepository.countDailyViewsByListingIdsAndPeriod(listingIds, from, to)).thenReturn(rows);

        List<DailyViewsDTO> result = listingViewService.getDailyViewsForLandlord(landlordId, from, to);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(result.get(0).viewCount()).isEqualTo(5L);
        assertThat(result.get(1).date()).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(result.get(1).viewCount()).isEqualTo(8L);
    }
}
