package com.ndobaak.backend.admin.service;

import com.ndobaak.backend.admin.dto.AdminReviewResponse;
import com.ndobaak.backend.admin.dto.ReviewReportResponse;
import com.ndobaak.backend.auth.entity.Admin;
import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.enums.ReviewStatus;
import com.ndobaak.backend.common.enums.UserStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.entity.Review;
import com.ndobaak.backend.listing.entity.ReviewReport;
import com.ndobaak.backend.listing.repository.ReviewReportRepository;
import com.ndobaak.backend.listing.repository.ReviewRepository;
import com.ndobaak.backend.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewReportRepository reviewReportRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuthService authService;

    @InjectMocks
    private AdminReviewService adminReviewService;

    private Student student;
    private Landlord landlord;
    private Listing listing;
    private Review pendingReview;
    private Admin adminUser;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .firstName("Marie")
                .lastName("Curie")
                .email("marie@example.com")
                .status(UserStatus.ACTIVE)
                .build();
        student.setId(2L);

        landlord = Landlord.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .email("jean@example.com")
                .status(UserStatus.ACTIVE)
                .build();
        landlord.setId(1L);

        listing = Listing.builder()
                .title("Studio Eyang")
                .landlord(landlord)
                .build();
        listing.setId(10L);

        pendingReview = Review.builder()
                .rating(4)
                .comment("Très bien")
                .status(ReviewStatus.PENDING)
                .student(student)
                .listing(listing)
                .createdAt(LocalDateTime.now())
                .build();
        pendingReview.setId(100L);

        adminUser = Admin.builder()
                .firstName("Root")
                .lastName("Admin")
                .email("admin@example.com")
                .status(UserStatus.ACTIVE)
                .build();
        adminUser.setId(99L);

        pageable = PageRequest.of(0, 20);
    }

    @Test
    void getPendingReviewsShouldReturnPageOfPendingStatus() {
        Page<Review> reviewPage = new PageImpl<>(List.of(pendingReview));
        when(reviewRepository.findByStatus(ReviewStatus.PENDING, pageable)).thenReturn(reviewPage);
        when(reviewReportRepository.countByReviewId(100L)).thenReturn(0L);

        Page<AdminReviewResponse> result = adminReviewService.getPendingReviews(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ReviewStatus.PENDING);
        verify(reviewRepository).findByStatus(ReviewStatus.PENDING, pageable);
    }

    @Test
    void getFlaggedReviewsShouldReturnPageFromUnprocessedReports() {
        ReviewReport report = ReviewReport.builder()
                .review(pendingReview)
                .reporter(student)
                .reason("Contenu inapproprié")
                .processed(false)
                .createdAt(LocalDateTime.now())
                .build();
        report.setId(50L);

        Page<ReviewReport> reportPage = new PageImpl<>(List.of(report));
        when(reviewReportRepository.findByProcessedFalseOrderByCreatedAtDesc(pageable)).thenReturn(reportPage);
        when(reviewReportRepository.countByReviewId(100L)).thenReturn(1L);

        Page<AdminReviewResponse> result = adminReviewService.getFlaggedReviews(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReportCount()).isEqualTo(1L);
        verify(reviewReportRepository).findByProcessedFalseOrderByCreatedAtDesc(pageable);
    }

    @Test
    void approveReviewShouldSetStatusApprovedAndReturn() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(pendingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewReportRepository.countByReviewId(100L)).thenReturn(0L);

        AdminReviewResponse result = adminReviewService.approveReview(100L);

        assertThat(result.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        verify(reviewRepository).save(argThat(r -> r.getStatus() == ReviewStatus.APPROVED));
    }

    @Test
    void approveReviewNotFoundShouldThrow() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminReviewService.approveReview(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectReviewShouldSetStatusRejectedAndReturn() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(pendingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewReportRepository.countByReviewId(100L)).thenReturn(0L);

        AdminReviewResponse result = adminReviewService.rejectReview(100L);

        assertThat(result.getStatus()).isEqualTo(ReviewStatus.REJECTED);
        verify(reviewRepository).save(argThat(r -> r.getStatus() == ReviewStatus.REJECTED));
    }

    @Test
    void rejectReviewNotFoundShouldThrow() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminReviewService.rejectReview(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReportsForReviewShouldReturnPageOfReports() {
        ReviewReport report = ReviewReport.builder()
                .review(pendingReview)
                .reporter(student)
                .reason("Faux avis")
                .processed(false)
                .createdAt(LocalDateTime.now())
                .build();
        report.setId(50L);

        when(reviewRepository.existsById(100L)).thenReturn(true);
        Page<ReviewReport> reportPage = new PageImpl<>(List.of(report));
        when(reviewReportRepository.findByReviewId(100L, pageable)).thenReturn(reportPage);

        Page<ReviewReportResponse> result = adminReviewService.getReportsForReview(100L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReviewId()).isEqualTo(100L);
        assertThat(result.getContent().get(0).getReason()).isEqualTo("Faux avis");
    }

    @Test
    void getReportsForReviewNotFoundShouldThrow() {
        when(reviewRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> adminReviewService.getReportsForReview(999L, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolveReportApproveReviewShouldSetApprovedAndNotNotifyLandlord() {
        ReviewReport report = ReviewReport.builder()
                .review(pendingReview)
                .reporter(student)
                .reason("Spam")
                .processed(false)
                .createdAt(LocalDateTime.now())
                .build();
        report.setId(50L);

        when(authService.getAuthenticatedUser()).thenReturn(adminUser);
        when(reviewReportRepository.findById(50L)).thenReturn(Optional.of(report));
        when(reviewReportRepository.save(any(ReviewReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        adminReviewService.resolveReport(50L, true);

        verify(reviewRepository).save(argThat(r -> r.getStatus() == ReviewStatus.APPROVED));
        verify(notificationService, never()).createNotification(
                anyLong(), any(), anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void resolveReportRejectReviewShouldSetRejectedAndNotifyLandlord() {
        ReviewReport report = ReviewReport.builder()
                .review(pendingReview)
                .reporter(student)
                .reason("Contenu offensant")
                .processed(false)
                .createdAt(LocalDateTime.now())
                .build();
        report.setId(50L);

        when(authService.getAuthenticatedUser()).thenReturn(adminUser);
        when(reviewReportRepository.findById(50L)).thenReturn(Optional.of(report));
        when(reviewReportRepository.save(any(ReviewReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        adminReviewService.resolveReport(50L, false);

        verify(reviewRepository).save(argThat(r -> r.getStatus() == ReviewStatus.REJECTED));
        verify(notificationService).createNotification(
                eq(landlord.getId()),
                eq(NotificationType.REVIEW_FLAGGED),
                anyString(), anyString(),
                eq(pendingReview.getId()),
                eq("REVIEW"));
    }

    @Test
    void resolveAlreadyProcessedReportShouldThrow() {
        ReviewReport report = ReviewReport.builder()
                .review(pendingReview)
                .reporter(student)
                .reason("Spam")
                .processed(true)
                .createdAt(LocalDateTime.now())
                .build();
        report.setId(50L);

        when(authService.getAuthenticatedUser()).thenReturn(adminUser);
        when(reviewReportRepository.findById(50L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> adminReviewService.resolveReport(50L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà été traité");
    }

    @Test
    void resolveReportNotFoundShouldThrow() {
        when(authService.getAuthenticatedUser()).thenReturn(adminUser);
        when(reviewReportRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminReviewService.resolveReport(999L, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
