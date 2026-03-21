package com.ndobaak.backend.listing.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.dashboard.dto.DailyViewsDTO;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.entity.ListingView;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.ListingViewRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ListingViewService {

    private final ListingViewRepository listingViewRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ListingViewService(ListingViewRepository listingViewRepository,
                              ListingRepository listingRepository,
                              UserRepository userRepository) {
        this.listingViewRepository = listingViewRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    @Async
    @Transactional
    public void recordView(Long listingId, Long userId, String viewerIp) {
        LocalDate today = LocalDate.now();
        if (userId != null) {
            if (listingViewRepository.existsByListingIdAndUserIdAndViewedAt(listingId, userId, today)) {
                return;
            }
        } else if (viewerIp != null) {
            if (listingViewRepository.existsByListingIdAndViewerIpAndViewedAt(listingId, viewerIp, today)) {
                return;
            }
        }
        Listing listing = listingRepository.getReferenceById(listingId);
        User user = userId != null ? userRepository.getReferenceById(userId) : null;
        listingViewRepository.save(ListingView.builder()
                .listing(listing)
                .user(user)
                .viewerIp(viewerIp)
                .viewedAt(today)
                .build());
    }

    @Transactional(readOnly = true)
    public long getTotalViews(Long listingId) {
        return listingViewRepository.countByListingId(listingId);
    }

    @Transactional(readOnly = true)
    public long getViewsInPeriod(Long listingId, LocalDate from, LocalDate to) {
        return listingViewRepository.countByListingIdAndViewedAtBetween(listingId, from, to);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getViewsForLandlordListings(Long landlordId, LocalDate from, LocalDate to) {
        List<Long> listingIds = listingRepository.findIdsByLandlordId(landlordId);
        if (listingIds.isEmpty()) {
            return Map.of();
        }
        return listingViewRepository.countViewsByListingIdsAndPeriod(listingIds, from, to)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));
    }

    @Transactional(readOnly = true)
    public List<DailyViewsDTO> getDailyViewsForLandlord(Long landlordId, LocalDate from, LocalDate to) {
        List<Long> listingIds = listingRepository.findIdsByLandlordId(landlordId);
        if (listingIds.isEmpty()) {
            return List.of();
        }
        return listingViewRepository.countDailyViewsByListingIdsAndPeriod(listingIds, from, to)
                .stream()
                .map(row -> new DailyViewsDTO((LocalDate) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
