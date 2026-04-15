package com.ndobaak.backend.rentalcycle.controller;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.rentalcycle.dto.request.KeyHandoverRequest;
import com.ndobaak.backend.rentalcycle.dto.response.KeyHandoverResponse;
import com.ndobaak.backend.rentalcycle.service.KeyHandoverService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class KeyHandoverController {

    private final KeyHandoverService keyHandoverService;
    private final AuthService authService;

    @PostMapping("/{bookingId}/key-handover")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KeyHandoverResponse> createHandover(
            @PathVariable Long bookingId,
            @Valid @RequestBody KeyHandoverRequest request) {
        User admin = authService.getAuthenticatedUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(keyHandoverService.createHandover(bookingId, request, admin.getId()));
    }

    @GetMapping("/{bookingId}/key-handover")
    @PreAuthorize("hasAnyRole('ADMIN', 'LANDLORD', 'STUDENT')")
    public ResponseEntity<KeyHandoverResponse> getHandover(@PathVariable Long bookingId) {
        return ResponseEntity.ok(keyHandoverService.getHandover(bookingId));
    }

    @PostMapping("/{bookingId}/key-handover/confirm")
    @PreAuthorize("hasAnyRole('LANDLORD', 'STUDENT')")
    public ResponseEntity<KeyHandoverResponse> confirmHandover(@PathVariable Long bookingId) {
        User user = authService.getAuthenticatedUser();
        return switch (user.getRole()) {
            case LANDLORD -> ResponseEntity.ok(keyHandoverService.confirmByLandlord(bookingId, user.getId()));
            case STUDENT -> ResponseEntity.ok(keyHandoverService.confirmByTenant(bookingId, user.getId()));
            default -> ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        };
    }
}
