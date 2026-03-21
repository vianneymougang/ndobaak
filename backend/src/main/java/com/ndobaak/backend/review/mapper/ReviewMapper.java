package com.ndobaak.backend.review.mapper;

import com.ndobaak.backend.listing.entity.Review;
import com.ndobaak.backend.media.service.StorageService;
import com.ndobaak.backend.review.dto.ReviewResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReviewMapper {

    private final StorageService storageService;

    public ReviewMapper(StorageService storageService) {
        this.storageService = storageService;
    }

    public ReviewResponse toResponse(Review review) {
        String studentName = review.getStudent().getFirstName() + " " + review.getStudent().getLastName();
        String studentAvatar = storageService.resolvePublicUrl(review.getStudent().getProfilePhotoUrl());

        List<String> resolvedPhotoUrls = review.getPhotoUrls().stream()
                .map(storageService::resolvePublicUrl)
                .toList();

        return ReviewResponse.builder()
                .id(review.getId())
                .studentName(studentName)
                .studentAvatar(studentAvatar)
                .rating(review.getRating())
                .comment(review.getComment())
                .photoUrls(resolvedPhotoUrls.isEmpty() ? null : resolvedPhotoUrls)
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
