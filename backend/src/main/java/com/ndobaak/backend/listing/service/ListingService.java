package com.ndobaak.backend.listing.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.ListingStatus;
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

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;

@Service
public class ListingService {

    private static final double CAMPUS_LAT = 3.8480;
    private static final double CAMPUS_LNG = 11.5021;
    private static final double MAX_CAMPUS_DISTANCE_KM = 10.0;

    private final ListingRepository listingRepository;
    private final AmenityRepository amenityRepository;
    private final PhotoRepository photoRepository;
    private final StorageService storageService;
    private final ImageProcessingService imageProcessingService;
    private final AuthService authService;
    private final ListingMapper listingMapper;

    public ListingService(ListingRepository listingRepository, AmenityRepository amenityRepository,
                          PhotoRepository photoRepository, StorageService storageService,
                          ImageProcessingService imageProcessingService, AuthService authService,
                          ListingMapper listingMapper) {
        this.listingRepository = listingRepository;
        this.amenityRepository = amenityRepository;
        this.photoRepository = photoRepository;
        this.storageService = storageService;
        this.imageProcessingService = imageProcessingService;
        this.authService = authService;
        this.listingMapper = listingMapper;
    }

    @Transactional
    public ListingResponse createListing(CreateListingRequest request) {
        User landlord = authService.getAuthenticatedUser();

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
    public ListingResponse uploadPhoto(Long listingId, MultipartFile file) throws IOException {
        Listing listing = getOwnedListing(listingId);

        int photoCount = photoRepository.countByListingId(listingId);
        if (photoCount >= 10) {
            throw new BusinessException("Maximum 10 photos per listing");
        }

        ImageProcessingService.ResizedImages resized = imageProcessingService.resize(file.getInputStream());

        String originalUrl = storageService.upload("listings/" + listingId + "/original",
                file.getOriginalFilename(),
                new ByteArrayInputStream(resized.original()),
                resized.original().length,
                file.getContentType());

        String mediumUrl = storageService.upload("listings/" + listingId + "/medium",
                file.getOriginalFilename(),
                new ByteArrayInputStream(resized.medium()),
                resized.medium().length,
                file.getContentType());

        String thumbnailUrl = storageService.upload("listings/" + listingId + "/thumbnail",
                file.getOriginalFilename(),
                new ByteArrayInputStream(resized.thumbnail()),
                resized.thumbnail().length,
                file.getContentType());

        Photo photo = Photo.builder()
                .url(originalUrl)
                .mediumUrl(mediumUrl)
                .thumbnailUrl(thumbnailUrl)
                .fileSize(file.getSize())
                .displayOrder(photoCount)
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
            double distance = haversineDistance(
                    listing.getLocation().getLatitude(),
                    listing.getLocation().getLongitude(),
                    CAMPUS_LAT, CAMPUS_LNG);
            if (distance > MAX_CAMPUS_DISTANCE_KM) {
                throw new BusinessException("Listing must be within 10km of campus");
            }
            listing.getLocation().setDistanceToCampus(Math.round(distance * 100.0) / 100.0);
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
            double distance = haversineDistance(
                    request.getLatitude(), request.getLongitude(),
                    CAMPUS_LAT, CAMPUS_LNG);
            location.setDistanceToCampus(Math.round(distance * 100.0) / 100.0);
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
            double distance = haversineDistance(
                    location.getLatitude(), location.getLongitude(),
                    CAMPUS_LAT, CAMPUS_LNG);
            location.setDistanceToCampus(Math.round(distance * 100.0) / 100.0);
        }
    }

    static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
