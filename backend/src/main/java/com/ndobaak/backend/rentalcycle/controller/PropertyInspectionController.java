package com.ndobaak.backend.rentalcycle.controller;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.rentalcycle.dto.request.InspectionItemRequest;
import com.ndobaak.backend.rentalcycle.dto.request.InspectionRequest;
import com.ndobaak.backend.rentalcycle.dto.response.InspectionItemResponse;
import com.ndobaak.backend.rentalcycle.dto.response.InspectionPhotoResponse;
import com.ndobaak.backend.rentalcycle.dto.response.PropertyInspectionResponse;
import com.ndobaak.backend.rentalcycle.service.PropertyInspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class PropertyInspectionController {

    private final PropertyInspectionService inspectionService;
    private final AuthService authService;

    @PostMapping("/{bookingId}/inspection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PropertyInspectionResponse> createInspection(
            @PathVariable Long bookingId,
            @Valid @RequestBody InspectionRequest request) {
        User admin = authService.getAuthenticatedUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inspectionService.createInspection(bookingId, request, admin.getId()));
    }

    @GetMapping("/{bookingId}/inspection")
    @PreAuthorize("hasAnyRole('ADMIN', 'LANDLORD', 'STUDENT')")
    public ResponseEntity<List<PropertyInspectionResponse>> getInspections(@PathVariable Long bookingId) {
        return ResponseEntity.ok(inspectionService.getInspections(bookingId));
    }

    @GetMapping("/{bookingId}/inspection/{inspId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LANDLORD', 'STUDENT')")
    public ResponseEntity<PropertyInspectionResponse> getInspection(
            @PathVariable Long bookingId,
            @PathVariable Long inspId) {
        return ResponseEntity.ok(inspectionService.getInspection(bookingId, inspId));
    }

    @PostMapping("/{bookingId}/inspection/{inspId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InspectionItemResponse> addItem(
            @PathVariable Long bookingId,
            @PathVariable Long inspId,
            @Valid @RequestBody InspectionItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inspectionService.addItem(bookingId, inspId, request));
    }

    @PostMapping("/{bookingId}/inspection/{inspId}/photos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InspectionPhotoResponse> uploadPhoto(
            @PathVariable Long bookingId,
            @PathVariable Long inspId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long itemId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inspectionService.uploadPhoto(bookingId, inspId, file, itemId));
    }

    @PostMapping("/{bookingId}/inspection/{inspId}/sign")
    @PreAuthorize("hasAnyRole('LANDLORD', 'STUDENT')")
    public ResponseEntity<PropertyInspectionResponse> signInspection(
            @PathVariable Long bookingId,
            @PathVariable Long inspId) {
        User user = authService.getAuthenticatedUser();
        return ResponseEntity.ok(inspectionService.signInspection(bookingId, inspId, user.getId(), user.getRole()));
    }
}
