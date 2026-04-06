package com.ndobaak.backend.payment.service;

import com.ndobaak.backend.payment.dto.InitiatePaymentRequest;
import com.ndobaak.backend.payment.dto.PayInstallmentRequest;
import com.ndobaak.backend.payment.dto.PaymentResponse;
import com.ndobaak.backend.payment.dto.PaymentScheduleResponse;
import com.ndobaak.backend.payment.dto.ReceiptResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    private final PaymentInitiationService initiationService;
    private final PaymentWebhookService webhookService;
    private final PaymentQueryService queryService;
    private final PaymentScheduleService scheduleService;

    public PaymentService(PaymentInitiationService initiationService,
                          PaymentWebhookService webhookService,
                          PaymentQueryService queryService,
                          PaymentScheduleService scheduleService) {
        this.initiationService = initiationService;
        this.webhookService = webhookService;
        this.queryService = queryService;
        this.scheduleService = scheduleService;
    }

    public PaymentResponse initiatePayment(InitiatePaymentRequest request) {
        return initiationService.initiatePayment(request);
    }

    public PaymentResponse payInstallment(Long installmentId, PayInstallmentRequest request) {
        return initiationService.payInstallment(installmentId, request);
    }

    public void handleWebhook(String providerName, String payload, String signature) {
        webhookService.handleWebhook(providerName, payload, signature);
    }

    public List<PaymentResponse> getPaymentsByBooking(Long bookingId) {
        return queryService.getPaymentsByBooking(bookingId);
    }

    public Page<PaymentResponse> getTransactionHistory(Pageable pageable) {
        return queryService.getTransactionHistory(pageable);
    }

    public ReceiptResponse getReceipt(Long paymentId) {
        return queryService.getReceipt(paymentId);
    }

    public PaymentScheduleResponse getPaymentSchedule(Long bookingId) {
        return scheduleService.getPaymentSchedule(bookingId);
    }
}
