package com.ndobaak.backend.rentalcycle.controller;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.rentalcycle.dto.response.KeyHandoverResponse;
import com.ndobaak.backend.rentalcycle.service.KeyHandoverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
public class AdminKeyHandoverController {

    private final KeyHandoverService keyHandoverService;
    private final AuthService authService;

    @PostMapping("/{bookingId}/retry-transfer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KeyHandoverResponse> retryTransfer(@PathVariable Long bookingId) {
        User admin = authService.getAuthenticatedUser();
        return ResponseEntity.ok(keyHandoverService.retryTransfer(bookingId, admin.getId()));
    }
}
