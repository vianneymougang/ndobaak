package com.ndobaak.backend.listing.service;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.geo.GeoUtils;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.dto.CreateListingRequest;
import com.ndobaak.backend.listing.dto.ListingResponse;
import com.ndobaak.backend.listing.dto.LocationRequest;
import com.ndobaak.backend.listing.dto.UpdateListingRequest;
import com.ndobaak.backend.listing.entity.Amenity;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.entity.Location;
import com.ndobaak.backend.listing.mapper.ListingMapper;
import com.ndobaak.backend.listing.repository.AmenityRepository;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.PhotoRepository;
import com.ndobaak.backend.media.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;
    @Mock
    private AmenityRepository amenityRepository;
    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private AuthService authService;
    @Mock
    private PhotoUploadService photoUploadService;
    @Mock
    private ListingMapper listingMapper;
    @Mock
    private com.ndobaak.backend.subscription.service.LandlordCapabilityPolicy landlordCapabilityPolicy;

    @InjectMocks
    private ListingService listingService;

    private static final ListingResponse DUMMY_RESPONSE = ListingResponse.builder()
            .id(1L).title("dummy").status(ListingStatus.DRAFT)
            .photos(Collections.emptyList()).amenities(Collections.emptyList())
            .build();

    @Test
    void createListingShouldReturnDraft() {
        Landlord landlord = buildLandlord(2L);
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(landlordCapabilityPolicy.hasActiveSubscription(2L)).thenReturn(true);
        when(landlordCapabilityPolicy.getMaxListings(2L)).thenReturn(20);
        when(listingRepository.countByLandlordId(2L)).thenReturn(0L);

        CreateListingRequest request = new CreateListingRequest();
        request.setTitle("Test Listing Title Here");

        Listing savedListing = Listing.builder()
                .title("Test Listing Title Here")
                .status(ListingStatus.DRAFT)
                .landlord(landlord)
                .build();
        savedListing.setId(1L);

        when(listingRepository.save(any(Listing.class))).thenReturn(savedListing);

        ListingResponse expectedResponse = ListingResponse.builder()
                .id(1L).title("Test Listing Title Here").status(ListingStatus.DRAFT)
                .photos(Collections.emptyList()).amenities(Collections.emptyList()).build();

        when(listingMapper.toResponse(eq(savedListing), eq(false))).thenReturn(expectedResponse);

        ListingResponse result = listingService.createListing(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(ListingStatus.DRAFT);
        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    void createListingWithLocationShouldCalculateDistance() {
        Landlord landlord = buildLandlord(2L);
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(landlordCapabilityPolicy.hasActiveSubscription(2L)).thenReturn(true);
        when(landlordCapabilityPolicy.getMaxListings(2L)).thenReturn(20);
        when(listingRepository.countByLandlordId(2L)).thenReturn(0L);

        CreateListingRequest request = new CreateListingRequest();
        request.setTitle("Near Campus Listing");
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setAddress("Eyang");
        locationRequest.setLatitude(3.8855);
        locationRequest.setLongitude(11.3922);
        request.setLocation(locationRequest);

        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> {
            Listing l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });
        when(listingMapper.toResponse(any(), eq(false))).thenReturn(DUMMY_RESPONSE);

        listingService.createListing(request);

        verify(listingRepository).save(argThat(listing ->
                listing.getLocation() != null &&
                listing.getLocation().getDistanceToCampus() != null &&
                listing.getLocation().getDistanceToCampus() < 1.0
        ));
    }

    @Test
    void createListingWithAmenitiesShouldLinkThem() {
        Landlord landlord = buildLandlord(2L);
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(landlordCapabilityPolicy.hasActiveSubscription(2L)).thenReturn(true);
        when(landlordCapabilityPolicy.getMaxListings(2L)).thenReturn(20);
        when(listingRepository.countByLandlordId(2L)).thenReturn(0L);

        Amenity wifi = new Amenity();
        wifi.setId(1L);
        wifi.setName("WiFi");

        when(amenityRepository.findByNameIn(Set.of("WiFi"))).thenReturn(List.of(wifi));

        CreateListingRequest request = new CreateListingRequest();
        request.setAmenityNames(Set.of("WiFi"));

        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> {
            Listing l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });
        when(listingMapper.toResponse(any(), eq(false))).thenReturn(DUMMY_RESPONSE);

        listingService.createListing(request);

        verify(listingRepository).save(argThat(listing ->
                listing.getAmenities() != null && listing.getAmenities().size() == 1
        ));
    }

    @Test
    void updateListingShouldModifyFields() {
        Landlord landlord = buildLandlord(2L);
        Listing existing = Listing.builder()
                .title("Old Title").status(ListingStatus.DRAFT).landlord(landlord).build();
        existing.setId(1L);

        when(listingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(existing));
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
        when(listingMapper.toResponse(any(), eq(false))).thenReturn(DUMMY_RESPONSE);

        UpdateListingRequest request = new UpdateListingRequest();
        request.setTitle("New Title Updated");
        request.setMonthlyPrice(new BigDecimal("50000"));

        listingService.updateListing(1L, request);

        verify(listingRepository).save(argThat(listing ->
                "New Title Updated".equals(listing.getTitle()) &&
                new BigDecimal("50000").equals(listing.getMonthlyPrice())
        ));
    }

    @Test
    void updateListingByNonOwnerShouldThrow() {
        Landlord owner = buildLandlord(2L);
        Landlord other = buildLandlord(3L);

        Listing listing = Listing.builder()
                .title("Title").landlord(owner).build();
        listing.setId(1L);

        when(listingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(listing));
        when(authService.getAuthenticatedUser()).thenReturn(other);

        assertThatThrownBy(() -> listingService.updateListing(1L, new UpdateListingRequest()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void submitForReviewShouldValidateAndChangeStatus() {
        Landlord landlord = buildLandlord(2L);
        Location location = Location.builder()
                .address("Eyang").latitude(3.8855).longitude(11.3922).build();

        Listing listing = Listing.builder()
                .title("Valid Listing Title")
                .description("A sufficiently long description that is at least fifty characters long for validation purposes")
                .monthlyPrice(new BigDecimal("40000"))
                .status(ListingStatus.DRAFT)
                .landlord(landlord)
                .location(location)
                .build();
        listing.setId(1L);

        when(listingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(listing));
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(photoRepository.countByListingId(1L)).thenReturn(5);
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
        when(listingMapper.toResponse(any(), eq(false))).thenReturn(DUMMY_RESPONSE);

        listingService.submitForReview(1L);

        verify(listingRepository).save(argThat(l ->
                l.getStatus() == ListingStatus.PENDING_REVIEW
        ));
    }

    @Test
    void submitForReviewWithShortTitleShouldThrow() {
        Landlord landlord = buildLandlord(2L);
        Listing listing = Listing.builder()
                .title("Short")
                .description("Long enough description to pass the fifty character minimum for validation")
                .monthlyPrice(new BigDecimal("40000"))
                .landlord(landlord)
                .build();
        listing.setId(1L);

        when(listingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(listing));
        when(authService.getAuthenticatedUser()).thenReturn(landlord);

        assertThatThrownBy(() -> listingService.submitForReview(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("au moins 10 caractères");
    }

    @Test
    void submitForReviewWithTooFewPhotosShouldThrow() {
        Landlord landlord = buildLandlord(2L);
        Listing listing = Listing.builder()
                .title("Valid Title For Listing")
                .description("A sufficiently long description that passes the fifty character minimum for validation")
                .monthlyPrice(new BigDecimal("40000"))
                .landlord(landlord)
                .build();
        listing.setId(1L);

        when(listingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(listing));
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(photoRepository.countByListingId(1L)).thenReturn(2);

        assertThatThrownBy(() -> listingService.submitForReview(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Au moins 3 photos");
    }

    @Test
    void submitForReviewTooFarFromCampusShouldThrow() {
        Landlord landlord = buildLandlord(2L);
        Location farLocation = Location.builder()
                .address("Far away").latitude(4.05).longitude(9.7).build();

        Listing listing = Listing.builder()
                .title("Valid Title For Listing")
                .description("A sufficiently long description that passes the fifty character minimum for validation")
                .monthlyPrice(new BigDecimal("40000"))
                .landlord(landlord)
                .location(farLocation)
                .build();
        listing.setId(1L);

        when(listingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(listing));
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(photoRepository.countByListingId(1L)).thenReturn(5);

        assertThatThrownBy(() -> listingService.submitForReview(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("moins de 10 km du campus");
    }

    @Test
    void getListingByIdShouldReturnListing() {
        Landlord landlord = buildLandlord(2L);
        Listing listing = Listing.builder()
                .title("A Listing").landlord(landlord).build();
        listing.setId(1L);

        ListingResponse expected = ListingResponse.builder()
                .id(1L).title("A Listing")
                .photos(Collections.emptyList()).amenities(Collections.emptyList()).build();

        when(listingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(listing));
        when(listingMapper.toResponse(eq(listing), anyBoolean())).thenReturn(expected);

        ListingResponse result = listingService.getListingById(1L);

        assertThat(result.getTitle()).isEqualTo("A Listing");
    }

    @Test
    void getListingByIdNotFoundShouldThrow() {
        when(listingRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.getListingById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void haversineDistanceShouldCalculateCorrectly() {
        double distance = GeoUtils.haversineDistance(3.8845, 11.3912, 3.8845, 11.3912);
        assertThat(distance).isEqualTo(0.0);

        double shortDistance = GeoUtils.haversineDistance(3.8855, 11.3922, 3.8845, 11.3912);
        assertThat(shortDistance).isLessThan(1.0);
    }

    @Test
    void deletePhotoOfOtherListingShouldThrow() {
        Landlord landlord = buildLandlord(2L);
        Listing listing = Listing.builder().title("My Listing").landlord(landlord).build();
        listing.setId(1L);

        Listing otherListing = Listing.builder().title("Other").build();
        otherListing.setId(99L);

        com.ndobaak.backend.listing.entity.Photo photo = com.ndobaak.backend.listing.entity.Photo.builder()
                .url("url").listing(otherListing).build();
        photo.setId(10L);

        when(listingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(listing));
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(photoRepository.findById(10L)).thenReturn(Optional.of(photo));

        assertThatThrownBy(() -> listingService.deletePhoto(1L, 10L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void uploadPhotoDelegatesToPhotoUploadService() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[12]);
        com.ndobaak.backend.listing.dto.PhotoUploadAck expectedAck =
                new com.ndobaak.backend.listing.dto.PhotoUploadAck(42L, 1L);
        when(photoUploadService.scheduleUpload(1L, file)).thenReturn(expectedAck);

        com.ndobaak.backend.listing.dto.PhotoUploadAck result = listingService.uploadPhoto(1L, file);
        assertThat(result).isEqualTo(expectedAck);
        verify(photoUploadService).scheduleUpload(1L, file);
    }

    private Landlord buildLandlord(Long id) {
        Landlord landlord = Landlord.builder()
                .firstName("Paul").lastName("Ndi")
                .email("paul" + id + "@test.com").phone("69000000" + id)
                .password("hash").build();
        landlord.setId(id);
        return landlord;
    }
}
