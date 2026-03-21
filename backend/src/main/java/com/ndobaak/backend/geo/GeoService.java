package com.ndobaak.backend.geo;

import com.ndobaak.backend.common.enums.RoomType;
import com.ndobaak.backend.common.enums.ReviewStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.ReviewRepository;
import com.ndobaak.backend.media.service.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeoService {

    private static final int MAX_RESULTS = 50;
    private static final double MIN_RADIUS_KM = 0.1;
    private static final double MAX_RADIUS_KM = 10.0;

    private final ListingRepository listingRepository;
    private final ReviewRepository reviewRepository;
    private final StorageService storageService;

    public GeoService(ListingRepository listingRepository,
                      ReviewRepository reviewRepository,
                      StorageService storageService) {
        this.listingRepository = listingRepository;
        this.reviewRepository = reviewRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<NearbyListingResponse> findNearby(double lat, double lng, double radiusKm) {
        if (lat < -90 || lat > 90) {
            throw new BusinessException("La latitude doit être comprise entre -90 et 90.");
        }
        if (lng < -180 || lng > 180) {
            throw new BusinessException("La longitude doit être comprise entre -180 et 180.");
        }
        if (radiusKm < MIN_RADIUS_KM || radiusKm > MAX_RADIUS_KM) {
            throw new BusinessException("Le rayon doit être compris entre 0.1 et 10 km.");
        }

        List<Object[]> rows = listingRepository.findNearby(lat, lng, radiusKm, MAX_RESULTS);

        List<Long> listingIds = rows.stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();

        Map<Long, double[]> ratingsMap = reviewRepository.findAverageRatingsByListingIds(listingIds, ReviewStatus.APPROVED)
                .stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> new double[]{((Number) r[1]).doubleValue(), ((Number) r[2]).doubleValue()}
                ));

        return rows.stream()
                .map(row -> mapToResponse(row, lat, lng, ratingsMap))
                .toList();
    }

    private NearbyListingResponse mapToResponse(Object[] row, double userLat, double userLng,
                                                 Map<Long, double[]> ratingsMap) {
        Long id = ((Number) row[0]).longValue();
        String title = (String) row[1];
        String thumbnailUrl = row[2] != null ? storageService.resolvePublicUrl((String) row[2]) : null;
        BigDecimal monthlyPrice = row[3] != null ? new BigDecimal(row[3].toString()) : null;
        RoomType roomType = row[4] != null ? RoomType.valueOf(row[4].toString()) : null;
        String neighborhood = (String) row[5];
        Double distanceToCampus = row[6] != null ? ((Number) row[6]).doubleValue() : null;
        Double latitude = row[7] != null ? ((Number) row[7]).doubleValue() : null;
        Double longitude = row[8] != null ? ((Number) row[8]).doubleValue() : null;
        boolean verified = row[9] != null && (Boolean) row[9];

        Double distanceFromUser = null;
        if (latitude != null && longitude != null) {
            distanceFromUser = GeoUtils.roundDistance(
                    GeoUtils.haversineDistance(userLat, userLng, latitude, longitude));
        }

        double[] ratingData = ratingsMap.get(id);
        Double avgRating = ratingData != null ? Math.round(ratingData[0] * 10.0) / 10.0 : null;
        Integer reviewCount = ratingData != null ? (int) ratingData[1] : 0;

        return new NearbyListingResponse(
                id, title, thumbnailUrl, monthlyPrice, roomType, neighborhood,
                distanceToCampus, distanceFromUser, latitude, longitude,
                avgRating, reviewCount, verified
        );
    }
}
