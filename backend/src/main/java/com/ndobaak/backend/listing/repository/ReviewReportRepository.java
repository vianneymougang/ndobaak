package com.ndobaak.backend.listing.repository;

import com.ndobaak.backend.listing.entity.ReviewReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    boolean existsByReviewIdAndReporterId(Long reviewId, Long reporterId);

    long countByReviewId(Long reviewId);

    Page<ReviewReport> findByProcessedFalseOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT r FROM ReviewReport r WHERE r.review.id = :reviewId ORDER BY r.createdAt DESC")
    Page<ReviewReport> findByReviewId(Long reviewId, Pageable pageable);
}
