package com.ndobaak.backend.listing.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.geo.GeoUtils;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.dto.CreateListingRequest;
import com.ndobaak.backend.listing.dto.ListingResponse;
import com.ndobaak.backend.listing.dto.LocationRequest;
import com.ndobaak.backend.listing.entity.Amenity;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.entity.Location;
import com.ndobaak.backend.listing.entity.Photo;
import com.ndobaak.backend.listing.mapper.ListingMapper;
import com.ndobaak.backend.listing.repository.AmenityRepository;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.PhotoRepository;
import com.ndobaak.backend.media.service.ImageProcessingService;
import com.ndobaak.backend.media.service.StorageService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ListingService {

    private static final double MAX_CAMPUS_DISTANCE_KM = 10.0;
    private static final String WEBP_CONTENT_TYPE = "image/webp";
    private static final long MAX_UPLOAD_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = {0x57, 0x45, 0x42, 0x50};

    private final ListingRepository listingRepository;
    private final AmenityRepository amenityRepository;
    private final PhotoRepository photoRepository;
    private final StorageService storageService;
    private final ImageProcessingService imageProcessingService;
    private final AuthService authService;
    private final ListingMapper listingMapper;
    private final com.ndobaak.backend.subscription.service.SubscriptionService subscriptionService;

    public ListingService(ListingRepository listingRepository, AmenityRepository amenityRepository,
                          PhotoRepository photoRepository, StorageService storageService,
                          ImageProcessingService imageProcessingService, AuthService authService,
                          ListingMapper listingMapper,
                          com.ndobaak.backend.subscription.service.SubscriptionService subscriptionService) {
        this.listingRepository = listingRepository;
        this.amenityRepository = amenityRepository;
        this.photoRepository = photoRepository;
        this.storageService = storageService;
        this.imageProcessingService = imageProcessingService;
        this.authService = authService;
        this.listingMapper = listingMapper;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public ListingResponse createListing(CreateListingRequest request) {
        User landlord = authService.getAuthenticatedUser();

        if (!subscriptionService.hasActiveSubscription(landlord.getId())) {
            throw new com.ndobaak.backend.common.exception.BusinessException(
                    "Vous devez souscrire à un abonnement pour publier des annonces.");
        }

        long currentListingCount = listingRepository.countByLandlordId(landlord.getId());
        int maxListings = subscriptionService.getMaxListings(landlord.getId());
        if (currentListingCount >= maxListings) {
            throw new com.ndobaak.backend.common.exception.BusinessException(
                    "Vous avez atteint la limite de " + maxListings
                            + " annonces autorisées par votre abonnement.");
        }

        Listing listing = Listing.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .roomType(request.getRoomType())
                .monthlyPrice(request.getMonthlyPrice())
                .availableRooms(request.getAvailableRooms() != null ? request.getAvailableRooms() : 1)
                .availableDate(request.getAvailableDate())
                .status(ListingStatus.DRAFT)
                .landlord(landlord)
                .build();

        if (request.getLocation() != null) {
            listing.setLocation(buildLocation(request.getLocation()));
        }

        if (request.getAmenityNames() != null && !request.getAmenityNames().isEmpty()) {
            List<Amenity> amenities = amenityRepository.findByNameIn(request.getAmenityNames());
            listing.setAmenities(new HashSet<>(amenities));
        }

        listing = listingRepository.save(listing);
        return listingMapper.toResponse(listing, false);
    }

    @Transactional
    public ListingResponse updateListing(Long listingId, CreateListingRequest request) {
        Listing listing = getOwnedListing(listingId);

        if (request.getTitle() != null) listing.setTitle(request.getTitle());
        if (request.getDescription() != null) listing.setDescription(request.getDescription());
        if (request.getRoomType() != null) listing.setRoomType(request.getRoomType());
        if (request.getMonthlyPrice() != null) listing.setMonthlyPrice(request.getMonthlyPrice());
        if (request.getAvailableRooms() != null) listing.setAvailableRooms(request.getAvailableRooms());
        if (request.getAvailableDate() != null) listing.setAvailableDate(request.getAvailableDate());

        if (request.getLocation() != null) {
            if (listing.getLocation() != null) {
                updateLocation(listing.getLocation(), request.getLocation());
            } else {
                listing.setLocation(buildLocation(request.getLocation()));
            }
        }

        if (request.getAmenityNames() != null) {
            List<Amenity> amenities = amenityRepository.findByNameIn(request.getAmenityNames());
            listing.setAmenities(new HashSet<>(amenities));
        }

        listing = listingRepository.save(listing);
        return listingMapper.toResponse(listing, false);
    }

    @Transactional
    public synchronized ListingResponse uploadPhoto(Long listingId, MultipartFile file) throws IOException {
        validateUploadedFile(file);
        Listing listing = getOwnedListing(listingId);

        int photoCount = photoRepository.countByListingId(listingId);
        if (photoCount >= 10) {
            throw new BusinessException("Maximum 10 photos per listing");
        }

        ImageProcessingService.ProcessedImages processed = imageProcessingService.process(file.getInputStream());

        String folder = "listings/" + listingId;

        String originalKey = storageService.upload(folder + "/original",
                "photo.webp",
                new ByteArrayInputStream(processed.original()),
                processed.original().length,
                WEBP_CONTENT_TYPE);

        String mediumKey = storageService.upload(folder + "/medium",
                "photo.webp",
                new ByteArrayInputStream(processed.medium()),
                processed.medium().length,
                WEBP_CONTENT_TYPE);

        String thumbnailKey = storageService.upload(folder + "/thumbnail",
                "photo.webp",
                new ByteArrayInputStream(processed.thumbnail()),
                processed.thumbnail().length,
                WEBP_CONTENT_TYPE);

        int nextOrder = photoRepository.findMaxDisplayOrderByListingId(listingId)
                .map(max -> max + 1)
                .orElse(0);

        Photo photo = Photo.builder()
                .url(originalKey)
                .mediumUrl(mediumKey)
                .thumbnailUrl(thumbnailKey)
                .fileSize((long) processed.original().length)
                .displayOrder(nextOrder)
                .listing(listing)
                .build();

        photoRepository.save(photo);

        Listing updatedListing = listingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));
        return listingMapper.toResponse(updatedListing, false);
    }

    @Transactional
    public void deletePhoto(Long listingId, Long photoId) {
        getOwnedListing(listingId);

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoId));

        if (!photo.getListing().getId().equals(listingId)) {
            throw new ForbiddenException("Photo does not belong to this listing");
        }

        storageService.delete(photo.getUrl());
        storageService.delete(photo.getMediumUrl());
        storageService.delete(photo.getThumbnailUrl());
        photoRepository.delete(photo);
    }

    @Transactional
    public ListingResponse submitForReview(Long listingId) {
        Listing listing = getOwnedListing(listingId);

        if (listing.getTitle() == null || listing.getTitle().length() < 10) {
            throw new BusinessException("Title must be at least 10 characters");
        }
        if (listing.getDescription() == null || listing.getDescription().length() < 50) {
            throw new BusinessException("Description must be at least 50 characters");
        }
        if (listing.getMonthlyPrice() == null || listing.getMonthlyPrice().signum() <= 0) {
            throw new BusinessException("Price must be greater than 0");
        }

        int photoCount = photoRepository.countByListingId(listingId);
        if (photoCount < 3) {
            throw new BusinessException("At least 3 photos are required to submit for review");
        }
        if (photoCount > 10) {
            throw new BusinessException("Maximum 10 photos allowed");
        }

        if (listing.getLocation() != null) {
            double distance = GeoUtils.haversineDistance(
                    listing.getLocation().getLatitude(),
                    listing.getLocation().getLongitude(),
                    GeoUtils.CAMPUS_LAT, GeoUtils.CAMPUS_LNG);
            if (distance > MAX_CAMPUS_DISTANCE_KM) {
                throw new BusinessException("Listing must be within 10km of campus");
            }
            listing.getLocation().setDistanceToCampus(GeoUtils.roundDistance(distance));
        }

        listing.setStatus(ListingStatus.PENDING_REVIEW);
        listing = listingRepository.save(listing);
        return listingMapper.toResponse(listing, false);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getMyListings() {
        User landlord = authService.getAuthenticatedUser();
        return listingRepository.findByLandlordIdOrderByCreatedAtDesc(landlord.getId())
                .stream()
                .map(l -> listingMapper.toResponse(l, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public ListingResponse getListingById(Long id) {
        Listing listing = listingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", id));

        boolean maskContact = shouldMaskContact(listing);
        return listingMapper.toResponse(listing, maskContact);
    }

    private boolean shouldMaskContact(Listing listing) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }
        try {
            User currentUser = authService.getAuthenticatedUser();
            return !currentUser.getId().equals(listing.getLandlord().getId());
        } catch (Exception e) {
            return true;
        }
    }

    private Listing getOwnedListing(Long listingId) {
        Listing listing = listingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        User currentUser = authService.getAuthenticatedUser();
        if (!listing.getLandlord().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only manage your own listings");
        }
        return listing;
    }

    private Location buildLocation(LocationRequest request) {
        Location location = Location.builder()
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .neighborhood(request.getNeighborhood())
                .city(request.getCity() != null ? request.getCity() : "Yaoundé")
                .build();

        if (request.getLatitude() != null && request.getLongitude() != null) {
            double distance = GeoUtils.haversineDistance(
                    request.getLatitude(), request.getLongitude(),
                    GeoUtils.CAMPUS_LAT, GeoUtils.CAMPUS_LNG);
            location.setDistanceToCampus(GeoUtils.roundDistance(distance));
        }
        return location;
    }

    private void updateLocation(Location location, LocationRequest request) {
        if (request.getAddress() != null) location.setAddress(request.getAddress());
        if (request.getLatitude() != null) location.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) location.setLongitude(request.getLongitude());
        if (request.getNeighborhood() != null) location.setNeighborhood(request.getNeighborhood());
        if (request.getCity() != null) location.setCity(request.getCity());

        if (location.getLatitude() != null && location.getLongitude() != null) {
            double distance = GeoUtils.haversineDistance(
                    location.getLatitude(), location.getLongitude(),
                    GeoUtils.CAMPUS_LAT, GeoUtils.CAMPUS_LNG);
            location.setDistanceToCampus(GeoUtils.roundDistance(distance));
        }
    }

    private void validateUploadedFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("Le fichier est vide.");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new BusinessException("Le fichier dépasse la taille maximale de 10 Mo.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BusinessException("Format de fichier non accepté. Formats autorisés : JPEG, PNG, WebP.");
        }
        validateMagicBytes(file.getInputStream());
    }

    private void validateMagicBytes(InputStream inputStream) throws IOException {
        byte[] header = new byte[12];
        int bytesRead = inputStream.read(header);
        if (bytesRead < 4) {
            throw new BusinessException("Format de fichier non accepté. Formats autorisés : JPEG, PNG, WebP.");
        }

        boolean isJpeg = bytesRead >= 3
                && header[0] == JPEG_MAGIC[0] && header[1] == JPEG_MAGIC[1] && header[2] == JPEG_MAGIC[2];
        boolean isPng = bytesRead >= 4
                && header[0] == PNG_MAGIC[0] && header[1] == PNG_MAGIC[1]
                && header[2] == PNG_MAGIC[2] && header[3] == PNG_MAGIC[3];
        boolean isWebp = bytesRead >= 12
                && header[0] == RIFF_MAGIC[0] && header[1] == RIFF_MAGIC[1]
                && header[2] == RIFF_MAGIC[2] && header[3] == RIFF_MAGIC[3]
                && header[8] == WEBP_MAGIC[0] && header[9] == WEBP_MAGIC[1]
                && header[10] == WEBP_MAGIC[2] && header[11] == WEBP_MAGIC[3];

        if (!isJpeg && !isPng && !isWebp) {
            throw new BusinessException("Format de fichier non accepté. Formats autorisés : JPEG, PNG, WebP.");
        }
    }
}
