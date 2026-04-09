package com.ndobaak.backend.payment.controller;

import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.payment.dto.PaymentInitiationResponse;
import com.ndobaak.backend.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingPaymentController {

    private final PaymentService paymentService;
    private final AuthService authService;

    @PostMapping("/{id}/payment/initiate")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(@PathVariable Long id) {
        Long studentId = authService.getAuthenticatedUser().getId();
        return ResponseEntity.ok(paymentService.initiateBookingPayment(id, studentId));
    }
}
