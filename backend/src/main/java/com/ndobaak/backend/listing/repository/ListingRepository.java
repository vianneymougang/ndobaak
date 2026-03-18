package com.ndobaak.backend.listing.repository;

import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.listing.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    List<Listing> findByLandlordIdOrderByCreatedAtDesc(Long landlordId);

    List<Listing> findByStatusOrderByCreatedAtDesc(ListingStatus status);

    @Query("SELECT l FROM Listing l LEFT JOIN FETCH l.location LEFT JOIN FETCH l.amenities LEFT JOIN FETCH l.landlord WHERE l.id = :id")
    Optional<Listing> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT l FROM Listing l LEFT JOIN FETCH l.location LEFT JOIN FETCH l.amenities " +
           "WHERE l.status = 'PUBLISHED' AND l.availableRooms > 0")
    List<Listing> findPublishedWithAvailableRooms();

    @Query(value = "SELECT l.id, l.title, p.thumbnail_url, l.monthly_price, " +
            "l.room_type, loc.neighborhood, loc.distance_to_campus, " +
            "loc.latitude, loc.longitude, " +
            "(l.status = 'PUBLISHED') AS verified " +
            "FROM listing l " +
            "JOIN location loc ON l.location_id = loc.id " +
            "LEFT JOIN LATERAL (" +
            "  SELECT ph.thumbnail_url FROM photo ph " +
            "  WHERE ph.listing_id = l.id AND ph.thumbnail_url IS NOT NULL " +
            "  ORDER BY ph.display_order LIMIT 1" +
            ") p ON true " +
            "WHERE l.status = 'PUBLISHED' " +
            "AND l.available_rooms > 0 " +
            "AND (6371.0 * 2 * ASIN(SQRT(" +
            "  POWER(SIN(RADIANS(loc.latitude - :lat) / 2), 2) + " +
            "  COS(RADIANS(:lat)) * COS(RADIANS(loc.latitude)) * " +
            "  POWER(SIN(RADIANS(loc.longitude - :lng) / 2), 2)" +
            "))) <= :radiusKm " +
            "ORDER BY (6371.0 * 2 * ASIN(SQRT(" +
            "  POWER(SIN(RADIANS(loc.latitude - :lat) / 2), 2) + " +
            "  COS(RADIANS(:lat)) * COS(RADIANS(loc.latitude)) * " +
            "  POWER(SIN(RADIANS(loc.longitude - :lng) / 2), 2)" +
            "))) ASC " +
            "LIMIT :maxResults",
            nativeQuery = true)
    List<Object[]> findNearby(@Param("lat") double lat,
                              @Param("lng") double lng,
                              @Param("radiusKm") double radiusKm,
                              @Param("maxResults") int maxResults);
}
