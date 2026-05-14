package com.ndobaak.backend.listing.controller;

import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.RoomType;
import com.ndobaak.backend.listing.dto.CreateListingRequest;
import com.ndobaak.backend.listing.dto.ListingResponse;
import com.ndobaak.backend.listing.dto.ListingSearchRequest;
import com.ndobaak.backend.listing.dto.ListingSearchResponse;
import com.ndobaak.backend.listing.dto.PhotoUploadAck;
import com.ndobaak.backend.listing.dto.SearchPageResponse;
import com.ndobaak.backend.listing.dto.UpdateListingRequest;
import com.ndobaak.backend.listing.service.ListingSearchService;
import com.ndobaak.backend.listing.service.ListingService;
import com.ndobaak.backend.listing.service.ListingViewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/listings")
public class ListingController {

    private final ListingService listingService;
    private final ListingSearchService listingSearchService;
    private final ListingViewService listingViewService;
    private final AuthService authService;

    public ListingController(ListingService listingService,
                             ListingSearchService listingSearchService,
                             ListingViewService listingViewService,
                             AuthService authService) {
        this.listingService = listingService;
        this.listingSearchService = listingSearchService;
        this.listingViewService = listingViewService;
        this.authService = authService;
    }

    @GetMapping("/search")
    public ResponseEntity<SearchPageResponse<ListingSearchResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) Double maxDistance,
            @RequestParam(required = false) Set<String> amenities,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) LocalDate availableBefore,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        ListingSearchRequest request = new ListingSearchRequest();
        request.setKeyword(keyword);
        request.setRoomType(roomType);
        request.setPriceMin(priceMin);
        request.setPriceMax(priceMax);
        request.setMaxDistance(maxDistance);
        request.setAmenities(amenities);
        request.setMinRating(minRating);
        request.setAvailableBefore(availableBefore);
        request.setSortBy(sortBy);
        request.setUserLat(userLat);
        request.setUserLng(userLng);
        request.setPage(page);
        request.setSize(Math.min(size, 50));

        return ResponseEntity.ok(listingSearchService.search(request));
    }

    @PostMapping
    public ResponseEntity<ListingResponse> createListing(@Valid @RequestBody CreateListingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listingService.createListing(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> updateListing(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateListingRequest request) {
        return ResponseEntity.ok(listingService.updateListing(id, request));
    }

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoUploadAck> uploadPhoto(@PathVariable Long id,
                                                      @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(listingService.uploadPhoto(id, file));
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id, @PathVariable Long photoId) {
        listingService.deletePhoto(id, photoId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<ListingResponse> submitForReview(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.submitForReview(id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ListingResponse>> getMyListings() {
        return ResponseEntity.ok(listingService.getMyListings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable Long id, HttpServletRequest request) {
        Long userId = resolveUserId();
        String clientIp = resolveClientIp(request);
        listingViewService.recordView(id, userId, clientIp);
        return ResponseEntity.ok(listingService.getListingById(id));
    }

    private Long resolveUserId() {
        try {
            return authService.getAuthenticatedUser().getId();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
