package com.ndobaak.backend.review.service;

import com.ndobaak.backend.auth.entity.Admin;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.ReviewStatus;
import com.ndobaak.backend.common.enums.UserRole;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.entity.Review;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.ReviewRepository;
import com.ndobaak.backend.media.service.StorageService;
import com.ndobaak.backend.notification.service.NotificationService;
import com.ndobaak.backend.review.service.ReviewPhotoUploadService;
import com.ndobaak.backend.review.dto.CreateReviewRequest;
import com.ndobaak.backend.review.dto.RatingStatsResponse;
import com.ndobaak.backend.review.dto.ReviewResponse;
import com.ndobaak.backend.review.mapper.ReviewMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ListingRepository listingRepository;
    @Mock
    private AuthService authService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private StorageService storageService;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private ReviewPhotoUploadService reviewPhotoUploadService;

    @InjectMocks
    private ReviewService reviewService;

    private Student buildStudent(Long id) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName("Alice");
        student.setLastName("Martin");
        return student;
    }

    private Listing buildListing(Long id) {
        return Listing.builder()
                .id(id)
                .title("Studio Eyang")
                .landlord(buildLandlord(10L))
                .build();
    }

    private com.ndobaak.backend.auth.entity.Landlord buildLandlord(Long id) {
        com.ndobaak.backend.auth.entity.Landlord landlord = new com.ndobaak.backend.auth.entity.Landlord();
        landlord.setId(id);
        landlord.setFirstName("Owner");
        landlord.setLastName("Test");
        return landlord;
    }

    private Review buildReview(Long id, Student student, Listing listing) {
        return Review.builder()
                .id(id)
                .rating(4)
                .comment("Très bien")
                .student(student)
                .listing(listing)
                .status(ReviewStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .photoUrls(new ArrayList<>())
                .build();
    }

    private CreateReviewRequest buildRequest(Long listingId) {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setListingId(listingId);
        request.setRating(4);
        request.setComment("Très bon logement, je recommande");
        return request;
    }

    private ReviewResponse buildDummyResponse(Long id) {
        return ReviewResponse.builder().id(id).rating(4).build();
    }

    @Test
    void createReviewShouldPersistAndNotifyLandlord() {
        Student student = buildStudent(1L);
        Listing listing = buildListing(100L);
        CreateReviewRequest request = buildRequest(100L);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsByStudentIdAndListingIdAndStatusIn(eq(1L), eq(100L), any()))
                .thenReturn(true);
        when(reviewRepository.existsByStudentIdAndListingId(1L, 100L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(reviewMapper.toResponse(any())).thenReturn(buildDummyResponse(1L));

        ReviewResponse response = reviewService.createReview(request, null);

        assertThat(response.getId()).isEqualTo(1L);
        verify(reviewRepository).save(any(Review.class));
        verify(notificationService).createNotification(eq(10L), any(), anyString(), anyString(), anyLong(), eq("REVIEW"));
    }

    @Test
    void createReviewShouldThrowWhenNoEligibleBooking() {
        Student student = buildStudent(1L);
        Listing listing = buildListing(100L);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsByStudentIdAndListingIdAndStatusIn(eq(1L), eq(100L), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview(buildRequest(100L), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("réservation");
    }

    @Test
    void createReviewShouldThrowWhenAlreadyReviewed() {
        Student student = buildStudent(1L);
        Listing listing = buildListing(100L);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsByStudentIdAndListingIdAndStatusIn(eq(1L), eq(100L), any()))
                .thenReturn(true);
        when(reviewRepository.existsByStudentIdAndListingId(1L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(buildRequest(100L), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà");
    }

    @Test
    void createReviewShouldThrowWhenListingNotFound() {
        when(authService.getAuthenticatedUser()).thenReturn(buildStudent(1L));
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(buildRequest(999L), null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createReviewShouldDelegatePhotoUpload() {
        Student student = buildStudent(1L);
        Listing listing = buildListing(100L);
        CreateReviewRequest request = buildRequest(100L);

        MultipartFile photo = mock(MultipartFile.class);
        when(photo.isEmpty()).thenReturn(false);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsByStudentIdAndListingIdAndStatusIn(any(), any(), any())).thenReturn(true);
        when(reviewRepository.existsByStudentIdAndListingId(any(), any())).thenReturn(false);
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(reviewMapper.toResponse(any())).thenReturn(buildDummyResponse(1L));

        reviewService.createReview(request, List.of(photo));

        verify(reviewPhotoUploadService).scheduleUploads(eq(1L), eq(100L), any());
    }

    @Test
    void createReviewShouldThrowWhenTooManyPhotos() {
        Student student = buildStudent(1L);
        Listing listing = buildListing(100L);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsByStudentIdAndListingIdAndStatusIn(any(), any(), any())).thenReturn(true);
        when(reviewRepository.existsByStudentIdAndListingId(any(), any())).thenReturn(false);

        List<MultipartFile> photos = List.of(
                mock(MultipartFile.class), mock(MultipartFile.class),
                mock(MultipartFile.class), mock(MultipartFile.class));

        assertThatThrownBy(() -> reviewService.createReview(buildRequest(100L), photos))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Maximum");
    }

    @Test
    void getReviewsByListingShouldReturnApprovedOnly() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Review> page = new PageImpl<>(List.of(), pageable, 0);

        when(listingRepository.existsById(100L)).thenReturn(true);
        when(reviewRepository.findByListingIdAndStatus(100L, ReviewStatus.APPROVED, pageable)).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getReviewsByListing(100L, pageable);

        assertThat(result).isEmpty();
        verify(reviewRepository).findByListingIdAndStatus(100L, ReviewStatus.APPROVED, pageable);
    }

    @Test
    void getReviewsByListingShouldThrowWhenListingNotFound() {
        when(listingRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.getReviewsByListing(999L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMyReviewsShouldReturnStudentReviews() {
        Student student = buildStudent(1L);
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Review> page = new PageImpl<>(List.of(), pageable, 0);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(reviewRepository.findByStudentId(1L, pageable)).thenReturn(page);

        reviewService.getMyReviews(pageable);

        verify(reviewRepository).findByStudentId(1L, pageable);
    }

    @Test
    void deleteReviewByOwnerShouldSucceed() {
        Student student = buildStudent(1L);
        Listing listing = buildListing(100L);
        Review review = buildReview(1L, student, listing);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1L);

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReviewByAdminShouldSucceed() {
        Student student = buildStudent(1L);
        Listing listing = buildListing(100L);
        Review review = buildReview(1L, student, listing);

        Admin admin = new Admin();
        admin.setId(99L);

        when(authService.getAuthenticatedUser()).thenReturn(admin);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1L);

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReviewShouldThrowWhenNotOwnerOrAdmin() {
        Student student = buildStudent(1L);
        Student otherStudent = buildStudent(99L);
        Listing listing = buildListing(100L);
        Review review = buildReview(1L, student, listing);

        when(authService.getAuthenticatedUser()).thenReturn(otherStudent);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteReviewShouldThrowWhenNotFound() {
        when(authService.getAuthenticatedUser()).thenReturn(buildStudent(1L));
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteReviewShouldDeletePhotos() {
        Student student = buildStudent(1L);
        Listing listing = buildListing(100L);
        Review review = buildReview(1L, student, listing);
        review.setPhotoUrls(List.of("reviews/100/a.jpg", "reviews/100/b.jpg"));

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1L);

        verify(storageService).delete("reviews/100/a.jpg");
        verify(storageService).delete("reviews/100/b.jpg");
    }

    @Test
    void getRatingStatsShouldReturnAverageAndDistribution() {
        when(listingRepository.existsById(100L)).thenReturn(true);
        when(reviewRepository.findAverageRatingByListingId(100L, ReviewStatus.APPROVED)).thenReturn(4.3);
        when(reviewRepository.countByListingIdAndStatus(100L, ReviewStatus.APPROVED)).thenReturn(15L);
        when(reviewRepository.findRatingDistribution(100L, ReviewStatus.APPROVED))
                .thenReturn(List.of(
                        new Object[]{5, 8L},
                        new Object[]{4, 4L},
                        new Object[]{3, 2L},
                        new Object[]{2, 1L}
                ));

        RatingStatsResponse stats = reviewService.getRatingStats(100L);

        assertThat(stats.getAverageRating()).isEqualTo(4.3);
        assertThat(stats.getTotalReviews()).isEqualTo(15);
        assertThat(stats.getDistribution().get(5)).isEqualTo(8L);
        assertThat(stats.getDistribution().get(4)).isEqualTo(4L);
        assertThat(stats.getDistribution().get(1)).isEqualTo(0L);
    }

    @Test
    void getRatingStatsShouldReturnZeroWhenNoReviews() {
        when(listingRepository.existsById(100L)).thenReturn(true);
        when(reviewRepository.findAverageRatingByListingId(100L, ReviewStatus.APPROVED)).thenReturn(null);
        when(reviewRepository.countByListingIdAndStatus(100L, ReviewStatus.APPROVED)).thenReturn(0L);
        when(reviewRepository.findRatingDistribution(100L, ReviewStatus.APPROVED)).thenReturn(Collections.emptyList());

        RatingStatsResponse stats = reviewService.getRatingStats(100L);

        assertThat(stats.getAverageRating()).isEqualTo(0.0);
        assertThat(stats.getTotalReviews()).isZero();
    }

    @Test
    void getRatingStatsShouldThrowWhenListingNotFound() {
        when(listingRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.getRatingStats(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
