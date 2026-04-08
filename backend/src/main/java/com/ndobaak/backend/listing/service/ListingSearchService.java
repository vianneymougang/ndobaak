package com.ndobaak.backend.listing.service;

import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.geo.GeoUtils;
import com.ndobaak.backend.common.enums.ReviewStatus;
import com.ndobaak.backend.listing.dto.ListingSearchRequest;
import com.ndobaak.backend.listing.dto.ListingSearchResponse;
import com.ndobaak.backend.listing.dto.SearchPageResponse;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.entity.Photo;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.ReviewRepository;
import com.ndobaak.backend.media.service.StorageService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ListingSearchService {

    private static final int MAX_OVERSIZED_WINDOW = 500;

    private final ListingRepository listingRepository;
    private final ReviewRepository reviewRepository;
    private final StorageService storageService;

    public ListingSearchService(ListingRepository listingRepository,
                                ReviewRepository reviewRepository,
                                StorageService storageService) {
        this.listingRepository = listingRepository;
        this.reviewRepository = reviewRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public SearchPageResponse<ListingSearchResponse> search(ListingSearchRequest request) {
        Specification<Listing> spec = buildSpecification(request);
        boolean needsPostFilter = hasPostFilters(request);
        boolean needsComplexSort = needsComplexSorting(request.getSortBy());

        if (!needsPostFilter && !needsComplexSort) {
            return searchWithDbPagination(spec, request);
        }
        return searchWithOversizedWindow(spec, request);
    }

    private SearchPageResponse<ListingSearchResponse> searchWithDbPagination(
            Specification<Listing> spec, ListingSearchRequest request) {
        Sort sort = resolveDbSort(request.getSortBy());
        PageRequest pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        Page<Listing> page = listingRepository.findAll(spec, pageable);

        List<Long> listingIds = page.getContent().stream().map(Listing::getId).toList();
        Map<Long, double[]> ratingsMap = loadRatings(listingIds);

        List<ListingSearchResponse> results = page.getContent().stream()
                .map(listing -> toSearchResponse(listing, ratingsMap, request))
                .toList();

        return SearchPageResponse.<ListingSearchResponse>builder()
                .content(results)
                .totalElements((int) page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(request.getPage())
                .size(request.getSize())
                .build();
    }

    private SearchPageResponse<ListingSearchResponse> searchWithOversizedWindow(
            Specification<Listing> spec, ListingSearchRequest request) {
        int windowSize = Math.min(request.getSize() * 3, MAX_OVERSIZED_WINDOW);
        PageRequest pageable = PageRequest.of(0, windowSize);
        Page<Listing> page = listingRepository.findAll(spec, pageable);

        List<Long> listingIds = page.getContent().stream().map(Listing::getId).toList();
        Map<Long, double[]> ratingsMap = loadRatings(listingIds);

        List<ListingSearchResponse> results = page.getContent().stream()
                .map(listing -> toSearchResponse(listing, ratingsMap, request))
                .toList();

        results = applyPostFilters(results, request);
        results = applySorting(results, request.getSortBy());

        int totalElements = results.size();
        int pageNum = request.getPage();
        int size = request.getSize();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(pageNum * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<ListingSearchResponse> pageContent = results.subList(fromIndex, toIndex);

        return SearchPageResponse.<ListingSearchResponse>builder()
                .content(pageContent)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .page(pageNum)
                .size(size)
                .build();
    }

    private boolean hasPostFilters(ListingSearchRequest request) {
        return request.getMaxDistance() != null
                || request.getMinRating() != null
                || (request.getAmenities() != null && !request.getAmenities().isEmpty());
    }

    private boolean needsComplexSorting(String sortBy) {
        if (sortBy == null) return false;
        return switch (sortBy.toUpperCase()) {
            case "DISTANCE_ASC", "RATING_DESC" -> true;
            default -> false;
        };
    }

    private Sort resolveDbSort(String sortBy) {
        if (sortBy == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sortBy.toUpperCase()) {
            case "PRICE_ASC" -> Sort.by(Sort.Direction.ASC, "monthlyPrice");
            case "PRICE_DESC" -> Sort.by(Sort.Direction.DESC, "monthlyPrice");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private Specification<Listing> buildSpecification(ListingSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), ListingStatus.PUBLISHED));
            predicates.add(cb.greaterThan(root.get("availableRooms"), 0));

            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            if (request.getRoomType() != null) {
                predicates.add(cb.equal(root.get("roomType"), request.getRoomType()));
            }

            if (request.getPriceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("monthlyPrice"), request.getPriceMin()));
            }

            if (request.getPriceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("monthlyPrice"), request.getPriceMax()));
            }

            if (request.getAvailableBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("availableDate"), request.getAvailableBefore()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<ListingSearchResponse> applyPostFilters(List<ListingSearchResponse> results,
                                                          ListingSearchRequest request) {
        List<ListingSearchResponse> filtered = new ArrayList<>(results);

        if (request.getMaxDistance() != null) {
            boolean hasUserCoords = request.getUserLat() != null && request.getUserLng() != null;
            filtered = filtered.stream()
                    .filter(r -> {
                        Double distance = hasUserCoords ? r.getDistanceFromUser() : r.getDistanceToCampus();
                        return distance != null && distance <= request.getMaxDistance();
                    })
                    .toList();
        }

        if (request.getMinRating() != null) {
            filtered = filtered.stream()
                    .filter(r -> r.getAvgRating() != null && r.getAvgRating() >= request.getMinRating())
                    .toList();
        }

        if (request.getAmenities() != null && !request.getAmenities().isEmpty()) {
            filtered = filtered.stream()
                    .filter(r -> r.getAmenities() != null && r.getAmenities().containsAll(request.getAmenities()))
                    .toList();
        }

        return filtered;
    }

    private List<ListingSearchResponse> applySorting(List<ListingSearchResponse> results, String sortBy) {
        if (sortBy == null) return results;

        List<ListingSearchResponse> sorted = new ArrayList<>(results);

        switch (sortBy.toUpperCase()) {
            case "PRICE_ASC" -> sorted.sort(Comparator.comparing(ListingSearchResponse::getMonthlyPrice));
            case "PRICE_DESC" -> sorted.sort(Comparator.comparing(ListingSearchResponse::getMonthlyPrice).reversed());
            case "DISTANCE_ASC" -> sorted.sort(Comparator.comparing(
                    r -> r.getDistanceFromUser() != null ? r.getDistanceFromUser() : r.getDistanceToCampus(),
                    Comparator.nullsLast(Comparator.naturalOrder())));
            case "RATING_DESC" -> sorted.sort(Comparator.comparing(
                    ListingSearchResponse::getAvgRating, Comparator.nullsLast(Comparator.reverseOrder())));
            default -> {}
        }

        return sorted;
    }

    private Map<Long, double[]> loadRatings(List<Long> listingIds) {
        Map<Long, double[]> ratingsMap = new HashMap<>();
        if (listingIds.isEmpty()) return ratingsMap;

        List<Object[]> ratings = reviewRepository.findAverageRatingsByListingIds(listingIds, ReviewStatus.APPROVED);
        for (Object[] row : ratings) {
            Long listingId = (Long) row[0];
            Double avgRating = (Double) row[1];
            Long count = (Long) row[2];
            ratingsMap.put(listingId, new double[]{
                    Math.round(avgRating * 10.0) / 10.0,
                    count
            });
        }
        return ratingsMap;
    }

    private ListingSearchResponse toSearchResponse(Listing listing, Map<Long, double[]> ratingsMap,
                                                      ListingSearchRequest request) {
        String thumbnailKey = listing.getPhotos().stream()
                .filter(p -> p.getThumbnailUrl() != null)
                .min(Comparator.comparing(Photo::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Photo::getThumbnailUrl)
                .orElse(null);

        Double distanceToCampus = null;
        String neighborhood = null;
        Double latitude = null;
        Double longitude = null;

        if (listing.getLocation() != null) {
            distanceToCampus = listing.getLocation().getDistanceToCampus();
            neighborhood = listing.getLocation().getNeighborhood();
            latitude = listing.getLocation().getLatitude();
            longitude = listing.getLocation().getLongitude();

            if (distanceToCampus == null && latitude != null && longitude != null) {
                distanceToCampus = GeoUtils.roundDistance(
                        GeoUtils.haversineDistance(latitude, longitude, GeoUtils.CAMPUS_LAT, GeoUtils.CAMPUS_LNG));
            }
        }

        double[] ratingData = ratingsMap.getOrDefault(listing.getId(), null);
        Double avgRating = ratingData != null ? ratingData[0] : null;
        Integer reviewCount = ratingData != null ? (int) ratingData[1] : 0;

        List<String> amenityNames = listing.getAmenities().stream()
                .map(a -> a.getName())
                .toList();

        return ListingSearchResponse.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .thumbnailUrl(storageService.resolvePublicUrl(thumbnailKey))
                .monthlyPrice(listing.getMonthlyPrice())
                .roomType(listing.getRoomType())
                .neighborhood(neighborhood)
                .distanceToCampus(distanceToCampus)
                .avgRating(avgRating)
                .reviewCount(reviewCount)
                .amenities(amenityNames)
                .verified(ListingStatus.PUBLISHED.equals(listing.getStatus()))
                .latitude(latitude)
                .longitude(longitude)
                .availableRooms(listing.getAvailableRooms())
                .build();
    }
}
