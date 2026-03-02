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
}
