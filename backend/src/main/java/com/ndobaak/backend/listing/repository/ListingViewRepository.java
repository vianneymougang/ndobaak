package com.ndobaak.backend.listing.repository;

import com.ndobaak.backend.listing.entity.ListingView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ListingViewRepository extends JpaRepository<ListingView, Long> {

    long countByListingId(Long listingId);

    long countByListingIdAndViewedAtBetween(Long listingId, LocalDate from, LocalDate to);

    boolean existsByListingIdAndUserIdAndViewedAt(Long listingId, Long userId, LocalDate viewedAt);

    boolean existsByListingIdAndViewerIpAndViewedAt(Long listingId, String viewerIp, LocalDate viewedAt);

    @Query("""
        SELECT lv.listing.id, COUNT(lv) as viewCount
        FROM ListingView lv
        WHERE lv.listing.id IN :listingIds
          AND lv.viewedAt BETWEEN :from AND :to
        GROUP BY lv.listing.id
        """)
    List<Object[]> countViewsByListingIdsAndPeriod(List<Long> listingIds, LocalDate from, LocalDate to);

    @Query("""
        SELECT lv.viewedAt, COUNT(lv) as viewCount
        FROM ListingView lv
        WHERE lv.listing.id IN :listingIds
          AND lv.viewedAt BETWEEN :from AND :to
        GROUP BY lv.viewedAt
        ORDER BY lv.viewedAt ASC
        """)
    List<Object[]> countDailyViewsByListingIdsAndPeriod(List<Long> listingIds, LocalDate from, LocalDate to);
}
