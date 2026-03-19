package com.ndobaak.backend.listing.repository;

import com.ndobaak.backend.common.enums.ReviewStatus;
import com.ndobaak.backend.listing.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<Review> findByListingIdAndStatus(Long listingId, ReviewStatus status, Pageable pageable);

    Page<Review> findByStudentId(Long studentId, Pageable pageable);

    boolean existsByStudentIdAndListingId(Long studentId, Long listingId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r " +
           "WHERE r.listing.id = :listingId AND r.status = :status " +
           "GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> findRatingDistribution(@Param("listingId") Long listingId,
                                          @Param("status") ReviewStatus status);

    @Query("SELECT AVG(r.rating) FROM Review r " +
           "WHERE r.listing.id = :listingId AND r.status = :status")
    Double findAverageRatingByListingId(@Param("listingId") Long listingId,
                                        @Param("status") ReviewStatus status);

    long countByListingIdAndStatus(Long listingId, ReviewStatus status);

    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    long countByStudentId(Long studentId);

    @Query("SELECT r FROM Review r WHERE SIZE(r.photoUrls) = 0 AND r.status = :status ORDER BY r.createdAt DESC")
    Page<Review> findByStatusOrderByCreatedAtDesc(@Param("status") ReviewStatus status, Pageable pageable);
}
