package com.ndobaak.backend.review.controller;

import com.ndobaak.backend.admin.dto.ReviewReportResponse;
import com.ndobaak.backend.admin.dto.ReportReviewRequest;
import com.ndobaak.backend.review.dto.CreateReviewRequest;
import com.ndobaak.backend.review.dto.RatingStatsResponse;
import com.ndobaak.backend.review.dto.ReviewResponse;
import com.ndobaak.backend.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping(value = "/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestPart("review") CreateReviewRequest request,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) throws IOException {
        ReviewResponse response = reviewService.createReview(request, photos);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/listings/{id}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByListing(
            @PathVariable Long id, Pageable pageable) {
        Page<ReviewResponse> reviews = reviewService.getReviewsByListing(id, pageable);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/reviews/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(Pageable pageable) {
        Page<ReviewResponse> reviews = reviewService.getMyReviews(pageable);
        return ResponseEntity.ok(reviews);
    }

    @DeleteMapping("/reviews/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/listings/{id}/rating-stats")
    public ResponseEntity<RatingStatsResponse> getRatingStats(@PathVariable Long id) {
        RatingStatsResponse stats = reviewService.getRatingStats(id);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/reviews/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewReportResponse> reportReview(
            @PathVariable Long id,
            @Valid @RequestBody ReportReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.reportReview(id, request.getReason()));
    }
}
