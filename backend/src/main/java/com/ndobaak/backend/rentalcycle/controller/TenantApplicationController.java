package com.ndobaak.backend.rentalcycle.controller;

import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.TenantDocumentType;
import com.ndobaak.backend.rentalcycle.dto.request.TenantApplicationRequest;
import com.ndobaak.backend.rentalcycle.dto.response.TenantApplicationResponse;
import com.ndobaak.backend.rentalcycle.dto.response.TenantDocumentResponse;
import com.ndobaak.backend.rentalcycle.service.TenantApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class TenantApplicationController {

    private final TenantApplicationService applicationService;
    private final AuthService authService;

    @PostMapping("/{id}/application")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TenantApplicationResponse> createApplication(
            @PathVariable Long id,
            @Valid @RequestBody TenantApplicationRequest request) {
        Long studentId = authService.getAuthenticatedUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.createApplication(id, request, studentId));
    }

    @GetMapping("/{id}/application")
    @PreAuthorize("hasAnyRole('STUDENT', 'LANDLORD', 'ADMIN')")
    public ResponseEntity<TenantApplicationResponse> getApplication(@PathVariable Long id) {
        Long userId = authService.getAuthenticatedUser().getId();
        return ResponseEntity.ok(applicationService.getApplication(id, userId));
    }

    @PostMapping("/{id}/application/documents")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TenantDocumentResponse> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") TenantDocumentType type) {
        Long studentId = authService.getAuthenticatedUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.uploadDocument(id, file, type, studentId));
    }

    @PostMapping("/{id}/application/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TenantApplicationResponse> submitApplication(@PathVariable Long id) {
        Long studentId = authService.getAuthenticatedUser().getId();
        return ResponseEntity.ok(applicationService.submitApplication(id, studentId));
    }
}
