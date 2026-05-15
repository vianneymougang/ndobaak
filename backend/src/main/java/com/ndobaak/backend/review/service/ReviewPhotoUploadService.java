package com.ndobaak.backend.review.service;

import com.ndobaak.backend.listing.repository.ReviewRepository;
import com.ndobaak.backend.media.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewPhotoUploadService {

    private static final Logger log = LoggerFactory.getLogger(ReviewPhotoUploadService.class);

    private final StorageService storageService;
    private final ReviewRepository reviewRepository;

    @Lazy
    @Autowired
    private ReviewPhotoUploadService self;

    public ReviewPhotoUploadService(StorageService storageService, ReviewRepository reviewRepository) {
        this.storageService = storageService;
        this.reviewRepository = reviewRepository;
    }

    public void scheduleUploads(Long reviewId, Long listingId, List<MultipartFile> photos) {
        if (photos == null || photos.isEmpty()) {
            return;
        }

        List<byte[]> bytesList = new ArrayList<>();
        List<String> filenames = new ArrayList<>();
        List<String> contentTypes = new ArrayList<>();
        List<Long> sizes = new ArrayList<>();

        for (MultipartFile photo : photos) {
            try {
                bytesList.add(photo.getBytes());
                filenames.add(photo.getOriginalFilename());
                contentTypes.add(photo.getContentType());
                sizes.add(photo.getSize());
            } catch (IOException e) {
                log.error("Failed to read photo bytes for review {}: {}", reviewId, e.getMessage());
            }
        }

        if (!bytesList.isEmpty()) {
            self.processAsync(reviewId, listingId, bytesList, filenames, contentTypes, sizes);
        }
    }

    @Async
    @Transactional
    public void processAsync(Long reviewId, Long listingId, List<byte[]> bytesList,
                              List<String> filenames, List<String> contentTypes, List<Long> sizes) {
        String folder = "reviews/" + listingId;
        List<String> keys = new ArrayList<>();

        for (int i = 0; i < bytesList.size(); i++) {
            try {
                String key = storageService.upload(folder, filenames.get(i),
                        new ByteArrayInputStream(bytesList.get(i)), sizes.get(i), contentTypes.get(i));
                keys.add(key);
            } catch (Exception e) {
                log.error("Failed to upload review photo {} for review {}: {}", filenames.get(i), reviewId, e.getMessage());
            }
        }

        if (keys.isEmpty()) {
            return;
        }

        reviewRepository.findById(reviewId).ifPresent(review -> {
            review.setPhotoUrls(keys);
            reviewRepository.save(review);
            log.info("Uploaded {} photos for review {}", keys.size(), reviewId);
        });
    }
}
