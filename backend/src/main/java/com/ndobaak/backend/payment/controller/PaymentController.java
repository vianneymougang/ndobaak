package com.ndobaak.backend.payment.controller;

import com.ndobaak.backend.payment.dto.InitiatePaymentRequest;
import com.ndobaak.backend.payment.dto.PayInstallmentRequest;
import com.ndobaak.backend.payment.dto.PaymentResponse;
import com.ndobaak.backend.payment.dto.PaymentScheduleResponse;
import com.ndobaak.backend.payment.dto.ReceiptResponse;
import com.ndobaak.backend.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request) {
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/webhook/{provider}")
    public ResponseEntity<Void> handleWebhook(
            @PathVariable String provider,
            HttpServletRequest request) throws IOException {
        String payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String signature = request.getHeader("X-Notchpay-Signature");

        paymentService.handleWebhook(provider, payload, signature);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByBooking(
            @PathVariable Long bookingId) {
        List<PaymentResponse> payments = paymentService.getPaymentsByBooking(bookingId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PaymentResponse>> getTransactionHistory(Pageable pageable) {
        Page<PaymentResponse> history = paymentService.getTransactionHistory(pageable);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable Long id) {
        ReceiptResponse receipt = paymentService.getReceipt(id);
        return ResponseEntity.ok(receipt);
    }

    @PostMapping("/installments/{id}/pay")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PaymentResponse> payInstallment(
            @PathVariable Long id,
            @Valid @RequestBody PayInstallmentRequest request) {
        PaymentResponse response = paymentService.payInstallment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/schedule/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentScheduleResponse> getPaymentSchedule(
            @PathVariable Long bookingId) {
        PaymentScheduleResponse schedule = paymentService.getPaymentSchedule(bookingId);
        return ResponseEntity.ok(schedule);
    }
}
