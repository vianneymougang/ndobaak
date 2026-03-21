package com.ndobaak.backend.admin.service;

import com.ndobaak.backend.admin.dto.AdminReviewResponse;
import com.ndobaak.backend.admin.dto.ReviewReportResponse;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.enums.ReviewStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.entity.Review;
import com.ndobaak.backend.listing.entity.ReviewReport;
import com.ndobaak.backend.listing.repository.ReviewReportRepository;
import com.ndobaak.backend.listing.repository.ReviewRepository;
import com.ndobaak.backend.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReviewService {

    private static final Logger log = LoggerFactory.getLogger(AdminReviewService.class);

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final NotificationService notificationService;
    private final AuthService authService;

    public AdminReviewService(ReviewRepository reviewRepository,
                               ReviewReportRepository reviewReportRepository,
                               NotificationService notificationService,
                               AuthService authService) {
        this.reviewRepository = reviewRepository;
        this.reviewReportRepository = reviewReportRepository;
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> getPendingReviews(Pageable pageable) {
        return reviewRepository.findByStatus(ReviewStatus.PENDING, pageable)
                .map(this::toAdminResponse);
    }

    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> getFlaggedReviews(Pageable pageable) {
        return reviewReportRepository.findByProcessedFalseOrderByCreatedAtDesc(pageable)
                .map(report -> toAdminResponse(report.getReview()));
    }

    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> getAllReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable).map(this::toAdminResponse);
    }

    @Transactional
    public AdminReviewResponse approveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        review.setStatus(ReviewStatus.APPROVED);
        reviewRepository.save(review);

        log.info("Review {} approved by admin", reviewId);
        return toAdminResponse(review);
    }

    @Transactional
    public AdminReviewResponse rejectReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        review.setStatus(ReviewStatus.REJECTED);
        reviewRepository.save(review);

        log.info("Review {} rejected by admin", reviewId);
        return toAdminResponse(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewReportResponse> getReportsForReview(Long reviewId, Pageable pageable) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Review", "id", reviewId);
        }
        return reviewReportRepository.findByReviewId(reviewId, pageable)
                .map(this::toReportResponse);
    }

    @Transactional
    public ReviewReportResponse resolveReport(Long reportId, boolean approveReview) {
        User admin = authService.getAuthenticatedUser();

        ReviewReport report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewReport", "id", reportId));

        if (report.isProcessed()) {
            throw new BusinessException("Ce signalement a déjà été traité.");
        }

        report.resolve(admin);
        reviewReportRepository.save(report);

        Review review = report.getReview();
        if (approveReview) {
            review.setStatus(ReviewStatus.APPROVED);
        } else {
            review.setStatus(ReviewStatus.REJECTED);
        }
        reviewRepository.save(review);

        if (!approveReview) {
            notificationService.createNotification(
                    review.getListing().getLandlord().getId(),
                    NotificationType.REVIEW_FLAGGED,
                    "Avis signalé supprimé",
                    "Un avis problématique sur votre annonce a été supprimé après vérification.",
                    review.getId(),
                    "REVIEW"
            );
        }

        log.info("Report {} resolved by admin {}. Review {} -> {}",
                reportId, admin.getId(), review.getId(),
                approveReview ? "APPROVED" : "REJECTED");

        return toReportResponse(report);
    }

    private AdminReviewResponse toAdminResponse(Review review) {
        long reportCount = reviewReportRepository.countByReviewId(review.getId());
        return AdminReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .studentName(review.getStudent().getFirstName() + " " + review.getStudent().getLastName())
                .studentId(review.getStudent().getId())
                .listingTitle(review.getListing().getTitle())
                .listingId(review.getListing().getId())
                .reportCount(reportCount)
                .photoUrls(review.getPhotoUrls())
                .build();
    }

    private ReviewReportResponse toReportResponse(ReviewReport report) {
        return ReviewReportResponse.builder()
                .id(report.getId())
                .reviewId(report.getReview().getId())
                .reporterName(report.getReporter().getFirstName() + " " + report.getReporter().getLastName())
                .reporterId(report.getReporter().getId())
                .reason(report.getReason())
                .processed(report.isProcessed())
                .resolvedByName(report.getResolvedBy() != null
                        ? report.getResolvedBy().getFirstName() + " " + report.getResolvedBy().getLastName()
                        : null)
                .resolvedAt(report.getResolvedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
