package com.ndobaak.backend.rentalcycle.controller;

import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.rentalcycle.dto.request.ValidationDecisionRequest;
import com.ndobaak.backend.rentalcycle.dto.response.TenantApplicationResponse;
import com.ndobaak.backend.rentalcycle.service.TenantApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTenantApplicationController {

    private final TenantApplicationService applicationService;
    private final AuthService authService;

    @PatchMapping("/{id}/application/validate")
    public ResponseEntity<TenantApplicationResponse> validateApplication(
            @PathVariable Long id,
            @Valid @RequestBody ValidationDecisionRequest decision) {
        Long adminId = authService.getAuthenticatedUser().getId();
        return ResponseEntity.ok(applicationService.validateApplication(id, decision, adminId));
    }
}
