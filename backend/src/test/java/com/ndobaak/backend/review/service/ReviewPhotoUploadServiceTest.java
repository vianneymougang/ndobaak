package com.ndobaak.backend.review.service;

import com.ndobaak.backend.listing.entity.Review;
import com.ndobaak.backend.listing.repository.ReviewRepository;
import com.ndobaak.backend.media.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewPhotoUploadServiceTest {

    @Mock
    private StorageService storageService;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewPhotoUploadService service;

    private Review buildReview(Long id) {
        Review review = new Review();
        review.setId(id);
        review.setPhotoUrls(new ArrayList<>());
        return review;
    }

    @Test
    void processAsync_uploadsPhotosAndUpdatesReview() {
        Review review = buildReview(1L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(storageService.upload(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("reviews/100/photo.jpg");

        service.processAsync(1L, 100L,
                List.of("hello".getBytes()),
                List.of("photo.jpg"),
                List.of("image/jpeg"),
                List.of(5L));

        verify(storageService).upload(eq("reviews/100"), eq("photo.jpg"), any(), eq(5L), eq("image/jpeg"));
        verify(reviewRepository).save(review);
    }

    @Test
    void processAsync_withMultiplePhotos_uploadsAll() {
        Review review = buildReview(1L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(storageService.upload(anyString(), anyString(), any(), anyLong(), anyString()))
                .thenReturn("reviews/100/a.jpg")
                .thenReturn("reviews/100/b.jpg");

        service.processAsync(1L, 100L,
                List.of("a".getBytes(), "b".getBytes()),
                List.of("a.jpg", "b.jpg"),
                List.of("image/jpeg", "image/jpeg"),
                List.of(1L, 2L));

        verify(storageService, times(2)).upload(anyString(), anyString(), any(), anyLong(), anyString());
        verify(reviewRepository).save(review);
    }

    @Test
    void processAsync_whenAllUploadsFail_doesNotUpdateReview() {
        when(storageService.upload(anyString(), anyString(), any(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("MinIO unreachable"));

        service.processAsync(1L, 100L,
                List.of("hello".getBytes()),
                List.of("photo.jpg"),
                List.of("image/jpeg"),
                List.of(5L));

        verify(reviewRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void processAsync_withPartialFailure_savesOnlySuccessfulKeys() {
        Review review = buildReview(1L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(storageService.upload(anyString(), eq("a.jpg"), any(), anyLong(), anyString()))
                .thenReturn("reviews/100/a.jpg");
        when(storageService.upload(anyString(), eq("b.jpg"), any(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("upload failed"));

        service.processAsync(1L, 100L,
                List.of("a".getBytes(), "b".getBytes()),
                List.of("a.jpg", "b.jpg"),
                List.of("image/jpeg", "image/jpeg"),
                List.of(1L, 2L));

        verify(reviewRepository).save(review);
    }
}
