package com.ndobaak.backend.admin.controller;

import com.ndobaak.backend.admin.dto.RejectListingRequest;
import com.ndobaak.backend.admin.service.AdminListingService;
import com.ndobaak.backend.listing.dto.ListingResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminListingService adminListingService;

    public AdminController(AdminListingService adminListingService) {
        this.adminListingService = adminListingService;
    }

    @GetMapping("/listings/pending")
    public ResponseEntity<List<ListingResponse>> getPendingListings() {
        return ResponseEntity.ok(adminListingService.getPendingListings());
    }

    @PutMapping("/listings/{id}/validate")
    public ResponseEntity<ListingResponse> validateListing(@PathVariable Long id) {
        return ResponseEntity.ok(adminListingService.validateListing(id));
    }

    @PutMapping("/listings/{id}/reject")
    public ResponseEntity<ListingResponse> rejectListing(@PathVariable Long id,
                                                          @Valid @RequestBody RejectListingRequest request) {
        return ResponseEntity.ok(adminListingService.rejectListing(id, request.getReason()));
    }
}
