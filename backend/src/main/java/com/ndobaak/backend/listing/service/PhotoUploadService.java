package com.ndobaak.backend.listing.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.PhotoStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ErrorCode;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.common.util.FileValidator;
import com.ndobaak.backend.listing.dto.PhotoUploadAck;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.entity.Photo;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.PhotoRepository;
import com.ndobaak.backend.media.service.ImageProcessingService;
import com.ndobaak.backend.media.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class PhotoUploadService {

    private static final Logger log = LoggerFactory.getLogger(PhotoUploadService.class);
    private static final String WEBP_CONTENT_TYPE = "image/webp";

    private final ListingRepository listingRepository;
    private final PhotoRepository photoRepository;
    private final StorageService storageService;
    private final ImageProcessingService imageProcessingService;
    private final AuthService authService;

    public PhotoUploadService(ListingRepository listingRepository,
                              PhotoRepository photoRepository,
                              StorageService storageService,
                              ImageProcessingService imageProcessingService,
                              AuthService authService) {
        this.listingRepository = listingRepository;
        this.photoRepository = photoRepository;
        this.storageService = storageService;
        this.imageProcessingService = imageProcessingService;
        this.authService = authService;
    }

    @Transactional
    public PhotoUploadAck scheduleUpload(Long listingId, MultipartFile file) throws IOException {
        FileValidator.validateImageFile(file);

        Listing listing = listingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        User currentUser = authService.getAuthenticatedUser();
        if (!listing.getLandlord().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Vous ne pouvez gérer que vos propres annonces.");
        }

        int photoCount = photoRepository.countByListingId(listingId);
        if (photoCount >= 10) {
            throw new BusinessException(ErrorCode.PHOTO_LIMIT_REACHED);
        }

        byte[] bytes = file.getBytes();
        int displayOrder = photoCount;

        Photo photo = Photo.builder()
                .url("")
                .displayOrder(displayOrder)
                .status(PhotoStatus.PROCESSING)
                .listing(listing)
                .build();

        photo = photoRepository.save(photo);
        Long photoId = photo.getId();

        processAsync(photoId, listingId, bytes);

        return new PhotoUploadAck(photoId, listingId);
    }

    @Async
    @Transactional
    public void processAsync(Long photoId, Long listingId, byte[] bytes) {
        try {
            ImageProcessingService.ProcessedImages processed =
                    imageProcessingService.process(new ByteArrayInputStream(bytes));

            String folder = "listings/" + listingId;

            String originalKey = storageService.upload(folder + "/original", "photo.webp",
                    new ByteArrayInputStream(processed.original()), processed.original().length, WEBP_CONTENT_TYPE);
            String mediumKey = storageService.upload(folder + "/medium", "photo.webp",
                    new ByteArrayInputStream(processed.medium()), processed.medium().length, WEBP_CONTENT_TYPE);
            String thumbnailKey = storageService.upload(folder + "/thumbnail", "photo.webp",
                    new ByteArrayInputStream(processed.thumbnail()), processed.thumbnail().length, WEBP_CONTENT_TYPE);

            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoId));

            photo.setUrl(originalKey);
            photo.setMediumUrl(mediumKey);
            photo.setThumbnailUrl(thumbnailKey);
            photo.setFileSize((long) processed.original().length);
            photo.setStatus(PhotoStatus.READY);
            photoRepository.save(photo);
        } catch (Exception e) {
            log.error("Photo processing failed for photoId={}", photoId, e);
            photoRepository.findById(photoId).ifPresent(p -> {
                photoRepository.delete(p);
            });
        }
    }
}
