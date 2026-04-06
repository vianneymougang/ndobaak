package com.ndobaak.backend.rentalcycle.controller;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.rentalcycle.dto.request.ValidationDecisionRequest;
import com.ndobaak.backend.rentalcycle.dto.response.LandlordDocumentResponse;
import com.ndobaak.backend.rentalcycle.service.LandlordDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/landlords")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLandlordDocumentController {

    private final LandlordDocumentService documentService;
    private final AuthService authService;

    @PatchMapping("/{landlordId}/documents/{docId}/validate")
    public ResponseEntity<LandlordDocumentResponse> validateDocument(
            @PathVariable Long landlordId,
            @PathVariable Long docId,
            @Valid @RequestBody ValidationDecisionRequest decision) {
        
        User currentUser = authService.getAuthenticatedUser();
        LandlordDocumentResponse response = documentService.validateDocument(docId, decision, currentUser.getId());
        return ResponseEntity.ok(response);
    }
}
