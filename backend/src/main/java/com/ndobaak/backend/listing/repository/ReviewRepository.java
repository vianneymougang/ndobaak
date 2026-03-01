package com.ndobaak.backend.listing.repository;

import com.ndobaak.backend.common.enums.ReviewStatus;
import com.ndobaak.backend.listing.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r.listing.id, AVG(r.rating), COUNT(r) FROM Review r " +
           "WHERE r.status = :status AND r.listing.id IN :listingIds " +
           "GROUP BY r.listing.id")
    List<Object[]> findAverageRatingsByListingIds(@Param("listingIds") List<Long> listingIds,
                                                   @Param("status") ReviewStatus status);
}
