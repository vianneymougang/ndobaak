package com.ndobaak.backend.rentalcycle.controller;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.TenantDocumentType;
import com.ndobaak.backend.rentalcycle.dto.request.TenantApplicationRequest;
import com.ndobaak.backend.rentalcycle.dto.request.ValidationDecisionRequest;
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
        
        User currentUser = authService.getAuthenticatedUser();
        TenantApplicationResponse response = applicationService.createApplication(id, request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/application/documents")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TenantDocumentResponse> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") TenantDocumentType type) {
        
        User currentUser = authService.getAuthenticatedUser();
        TenantDocumentResponse response = applicationService.uploadDocument(id, file, type, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/application/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TenantApplicationResponse> submitApplication(@PathVariable Long id) {
        User currentUser = authService.getAuthenticatedUser();
        TenantApplicationResponse response = applicationService.submitApplication(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/application/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TenantApplicationResponse> validateApplication(
            @PathVariable Long id,
            @Valid @RequestBody ValidationDecisionRequest decision) {
        
        User currentUser = authService.getAuthenticatedUser();
        TenantApplicationResponse response = applicationService.validateApplication(id, decision, currentUser.getId());
        return ResponseEntity.ok(response);
    }
}
