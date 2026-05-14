package com.ndobaak.backend.payment.controller;

import com.ndobaak.backend.payment.dto.InitiatePaymentRequest;
import com.ndobaak.backend.payment.dto.PayInstallmentRequest;
import com.ndobaak.backend.payment.dto.PaymentResponse;
import com.ndobaak.backend.payment.dto.PaymentScheduleResponse;
import com.ndobaak.backend.payment.dto.ReceiptResponse;
import com.ndobaak.backend.payment.service.PaymentInitiationService;
import com.ndobaak.backend.payment.service.PaymentQueryService;
import com.ndobaak.backend.payment.service.PaymentScheduleService;
import com.ndobaak.backend.payment.service.PaymentWebhookService;
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

    private final PaymentInitiationService initiationService;
    private final PaymentWebhookService webhookService;
    private final PaymentQueryService queryService;
    private final PaymentScheduleService scheduleService;

    public PaymentController(PaymentInitiationService initiationService,
                             PaymentWebhookService webhookService,
                             PaymentQueryService queryService,
                             PaymentScheduleService scheduleService) {
        this.initiationService = initiationService;
        this.webhookService = webhookService;
        this.queryService = queryService;
        this.scheduleService = scheduleService;
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(initiationService.initiatePayment(request));
    }

    @PostMapping("/webhook/{provider}")
    public ResponseEntity<Void> handleWebhook(
            @PathVariable String provider,
            HttpServletRequest request) throws IOException {
        String payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String signature = request.getHeader("x-notch-signature");
        webhookService.handleWebhook(provider, payload, signature);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(queryService.getPaymentsByBooking(bookingId));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PaymentResponse>> getTransactionHistory(Pageable pageable) {
        return ResponseEntity.ok(queryService.getTransactionHistory(pageable));
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getReceipt(id));
    }

    @PostMapping("/installments/{id}/pay")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PaymentResponse> payInstallment(
            @PathVariable Long id,
            @Valid @RequestBody PayInstallmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(initiationService.payInstallment(id, request));
    }

    @GetMapping("/schedule/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentScheduleResponse> getPaymentSchedule(@PathVariable Long bookingId) {
        return ResponseEntity.ok(scheduleService.getPaymentSchedule(bookingId));
    }
}
