package com.ndobaak.backend.rentalcycle.controller;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.LandlordDocumentType;
import com.ndobaak.backend.common.enums.UserRole;
import com.ndobaak.backend.rentalcycle.dto.request.ValidationDecisionRequest;
import com.ndobaak.backend.rentalcycle.dto.response.LandlordDocumentResponse;
import com.ndobaak.backend.rentalcycle.service.LandlordDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/landlords")
@RequiredArgsConstructor
public class LandlordDocumentController {

    private final LandlordDocumentService documentService;
    private final AuthService authService;

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<LandlordDocumentResponse> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") LandlordDocumentType type) {
        
        User currentUser = authService.getAuthenticatedUser();
        LandlordDocumentResponse response = documentService.uploadDocument(id, file, type, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<LandlordDocumentResponse>> getDocuments(@PathVariable Long id) {
        User currentUser = authService.getAuthenticatedUser();
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.SUPER_ADMIN;
        
        List<LandlordDocumentResponse> documents = documentService.getDocuments(id, currentUser.getId(), isAdmin);
        return ResponseEntity.ok(documents);
    }

    @PatchMapping("/admin/{landlordId}/documents/{docId}/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<LandlordDocumentResponse> validateDocument(
            @PathVariable Long landlordId,
            @PathVariable Long docId,
            @Valid @RequestBody ValidationDecisionRequest decision) {
        
        User currentUser = authService.getAuthenticatedUser();
        LandlordDocumentResponse response = documentService.validateDocument(docId, decision, currentUser.getId());
        return ResponseEntity.ok(response);
    }
}
