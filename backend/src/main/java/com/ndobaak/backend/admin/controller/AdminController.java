package com.ndobaak.backend.admin.controller;

import com.ndobaak.backend.admin.dto.AdminReviewResponse;
import com.ndobaak.backend.admin.dto.AdminTransactionResponse;
import com.ndobaak.backend.admin.dto.AdminUserDetailResponse;
import com.ndobaak.backend.admin.dto.AdminUserListResponse;
import com.ndobaak.backend.admin.dto.DeactivateUserRequest;
import com.ndobaak.backend.admin.dto.GlobalStatsResponse;
import com.ndobaak.backend.admin.dto.RejectionMotifRequest;
import com.ndobaak.backend.admin.dto.ResolveReportRequest;
import com.ndobaak.backend.admin.dto.ReviewReportResponse;
import com.ndobaak.backend.admin.service.AdminListingService;
import com.ndobaak.backend.admin.service.AdminReviewService;
import com.ndobaak.backend.admin.service.AdminStatisticsService;
import com.ndobaak.backend.admin.service.AdminTransactionService;
import com.ndobaak.backend.admin.service.AdminUserService;
import com.ndobaak.backend.listing.dto.ListingResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminListingService adminListingService;
    private final AdminUserService adminUserService;
    private final AdminReviewService adminReviewService;
    private final AdminStatisticsService adminStatisticsService;
    private final AdminTransactionService adminTransactionService;

    public AdminController(AdminListingService adminListingService,
                           AdminUserService adminUserService,
                           AdminReviewService adminReviewService,
                           AdminStatisticsService adminStatisticsService,
                           AdminTransactionService adminTransactionService) {
        this.adminListingService = adminListingService;
        this.adminUserService = adminUserService;
        this.adminReviewService = adminReviewService;
        this.adminStatisticsService = adminStatisticsService;
        this.adminTransactionService = adminTransactionService;
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
                                                          @Valid @RequestBody RejectionMotifRequest request) {
        return ResponseEntity.ok(adminListingService.rejectListing(id, request));
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserListResponse>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(adminUserService.listUsers(role, status, search, pageable));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDetailResponse> getUserDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserDetail(id));
    }

    @PutMapping("/users/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id,
                                                @Valid @RequestBody DeactivateUserRequest request) {
        adminUserService.deactivateUser(id, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/reactivate")
    public ResponseEntity<Void> reactivateUser(@PathVariable Long id) {
        adminUserService.reactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reviews/pending")
    public ResponseEntity<Page<AdminReviewResponse>> getPendingReviews(Pageable pageable) {
        return ResponseEntity.ok(adminReviewService.getPendingReviews(pageable));
    }

    @GetMapping("/reviews/flagged")
    public ResponseEntity<Page<AdminReviewResponse>> getFlaggedReviews(Pageable pageable) {
        return ResponseEntity.ok(adminReviewService.getFlaggedReviews(pageable));
    }

    @GetMapping("/reviews")
    public ResponseEntity<Page<AdminReviewResponse>> getAllReviews(Pageable pageable) {
        return ResponseEntity.ok(adminReviewService.getAllReviews(pageable));
    }

    @PutMapping("/reviews/{id}/approve")
    public ResponseEntity<AdminReviewResponse> approveReview(@PathVariable Long id) {
        return ResponseEntity.ok(adminReviewService.approveReview(id));
    }

    @PutMapping("/reviews/{id}/reject")
    public ResponseEntity<AdminReviewResponse> rejectReview(@PathVariable Long id) {
        return ResponseEntity.ok(adminReviewService.rejectReview(id));
    }

    @GetMapping("/reviews/{id}/reports")
    public ResponseEntity<Page<ReviewReportResponse>> getReportsForReview(
            @PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(adminReviewService.getReportsForReview(id, pageable));
    }

    @PutMapping("/reports/{id}/resolve")
    public ResponseEntity<ReviewReportResponse> resolveReport(
            @PathVariable Long id,
            @RequestBody ResolveReportRequest request) {
        return ResponseEntity.ok(adminReviewService.resolveReport(id, request.isApproveReview()));
    }

    @GetMapping("/statistics")
    public ResponseEntity<GlobalStatsResponse> getGlobalStats() {
        return ResponseEntity.ok(adminStatisticsService.getGlobalStats());
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<AdminTransactionResponse>> listTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            Pageable pageable) {
        return ResponseEntity.ok(adminTransactionService.listTransactions(status, type, pageable));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<AdminTransactionResponse> getTransactionDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminTransactionService.getTransactionDetail(id));
    }

    @GetMapping("/transactions/anomalies")
    public ResponseEntity<List<AdminTransactionResponse>> getAnomalies() {
        return ResponseEntity.ok(adminTransactionService.detectAnomalies());
    }
}
