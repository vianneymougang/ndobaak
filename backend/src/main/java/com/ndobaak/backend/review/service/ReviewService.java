package com.ndobaak.backend.review.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.NotificationType;
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
import com.ndobaak.backend.review.dto.CreateReviewRequest;
import com.ndobaak.backend.review.dto.RatingStatsResponse;
import com.ndobaak.backend.review.dto.ReviewResponse;
import com.ndobaak.backend.review.mapper.ReviewMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);
    private static final int MAX_PHOTOS = 3;

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final StorageService storageService;
    private final ReviewMapper reviewMapper;

    public ReviewService(ReviewRepository reviewRepository,
                         BookingRepository bookingRepository,
                         ListingRepository listingRepository,
                         AuthService authService,
                         NotificationService notificationService,
                         StorageService storageService,
                         ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.authService = authService;
        this.notificationService = notificationService;
        this.storageService = storageService;
        this.reviewMapper = reviewMapper;
    }

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, List<MultipartFile> photos) throws IOException {
        User student = authService.getAuthenticatedUser();
        Long listingId = request.getListingId();

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        boolean hasEligibleBooking = bookingRepository.existsByStudentIdAndListingIdAndStatusIn(
                student.getId(), listingId,
                List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));

        if (!hasEligibleBooking) {
            throw new BusinessException("Vous devez avoir une réservation confirmée ou terminée pour laisser un avis.");
        }

        if (reviewRepository.existsByStudentIdAndListingId(student.getId(), listingId)) {
            throw new BusinessException("Vous avez déjà laissé un avis pour ce logement.");
        }

        List<String> photoKeys = uploadReviewPhotos(listingId, photos);

        Review review = Review.builder()
                .rating(request.getRating())
                .comment(request.getComment())
                .student(student)
                .listing(listing)
                .status(ReviewStatus.APPROVED)
                .photoUrls(photoKeys)
                .build();

        Review saved = reviewRepository.save(review);

        notificationService.createNotification(
                listing.getLandlord().getId(),
                NotificationType.REVIEW_POSTED,
                "Nouvel avis sur votre logement",
                String.format("%s a laissé un avis %d/5 sur \"%s\".",
                        student.getFirstName(), request.getRating(), listing.getTitle()),
                saved.getId(),
                "REVIEW");

        log.info("Review {} created by student {} on listing {}", saved.getId(), student.getId(), listingId);

        return reviewMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByListing(Long listingId, Pageable pageable) {
        if (!listingRepository.existsById(listingId)) {
            throw new ResourceNotFoundException("Listing", "id", listingId);
        }

        return reviewRepository.findByListingIdAndStatus(listingId, ReviewStatus.APPROVED, pageable)
                .map(reviewMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(Pageable pageable) {
        User student = authService.getAuthenticatedUser();
        return reviewRepository.findByStudentId(student.getId(), pageable)
                .map(reviewMapper::toResponse);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        User user = authService.getAuthenticatedUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        boolean isOwner = review.getStudent().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Vous ne pouvez supprimer que vos propres avis.");
        }

        review.getPhotoUrls().forEach(storageService::delete);
        reviewRepository.delete(review);

        log.info("Review {} deleted by user {}", reviewId, user.getId());
    }

    @Transactional(readOnly = true)
    public RatingStatsResponse getRatingStats(Long listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new ResourceNotFoundException("Listing", "id", listingId);
        }

        Double average = reviewRepository.findAverageRatingByListingId(listingId, ReviewStatus.APPROVED);
        long total = reviewRepository.countByListingIdAndStatus(listingId, ReviewStatus.APPROVED);

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            distribution.put(i, 0L);
        }

        List<Object[]> rawDistribution = reviewRepository.findRatingDistribution(listingId, ReviewStatus.APPROVED);
        for (Object[] row : rawDistribution) {
            Integer star = (Integer) row[0];
            Long count = (Long) row[1];
            distribution.put(star, count);
        }

        return RatingStatsResponse.builder()
                .averageRating(average != null ? Math.round(average * 10.0) / 10.0 : 0.0)
                .totalReviews(total)
                .distribution(distribution)
                .build();
    }

    private List<String> uploadReviewPhotos(Long listingId, List<MultipartFile> photos) throws IOException {
        if (photos == null || photos.isEmpty()) {
            return new ArrayList<>();
        }

        if (photos.size() > MAX_PHOTOS) {
            throw new BusinessException("Maximum " + MAX_PHOTOS + " photos par avis.");
        }

        List<String> keys = new ArrayList<>();
        String folder = "reviews/" + listingId;

        for (MultipartFile photo : photos) {
            if (photo.isEmpty()) continue;

            String key = storageService.upload(folder,
                    photo.getOriginalFilename(),
                    photo.getInputStream(),
                    photo.getSize(),
                    photo.getContentType());
            keys.add(key);
        }

        return keys;
    }
}
