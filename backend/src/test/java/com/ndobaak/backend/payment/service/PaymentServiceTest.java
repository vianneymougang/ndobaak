package com.ndobaak.backend.payment.service;

import com.ndobaak.backend.payment.dto.InitiatePaymentRequest;
import com.ndobaak.backend.payment.dto.PayInstallmentRequest;
import com.ndobaak.backend.payment.dto.PaymentResponse;
import com.ndobaak.backend.payment.dto.PaymentScheduleResponse;
import com.ndobaak.backend.payment.dto.ReceiptResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentInitiationService initiationService;
    @Mock
    private PaymentWebhookService webhookService;
    @Mock
    private PaymentQueryService queryService;
    @Mock
    private PaymentScheduleService scheduleService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void initiatePaymentShouldDelegateToInitiationService() {
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        PaymentResponse expected = PaymentResponse.builder().id(1L).build();
        when(initiationService.initiatePayment(request)).thenReturn(expected);

        PaymentResponse result = paymentService.initiatePayment(request);

        assertThat(result).isSameAs(expected);
        verify(initiationService).initiatePayment(request);
    }

    @Test
    void payInstallmentShouldDelegateToInitiationService() {
        PayInstallmentRequest request = new PayInstallmentRequest();
        PaymentResponse expected = PaymentResponse.builder().id(2L).build();
        when(initiationService.payInstallment(1L, request)).thenReturn(expected);

        PaymentResponse result = paymentService.payInstallment(1L, request);

        assertThat(result).isSameAs(expected);
        verify(initiationService).payInstallment(1L, request);
    }

    @Test
    void handleWebhookShouldDelegateToWebhookService() {
        paymentService.handleWebhook("mock", "payload", "sig");

        verify(webhookService).handleWebhook("mock", "payload", "sig");
    }

    @Test
    void getPaymentsByBookingShouldDelegateToQueryService() {
        List<PaymentResponse> expected = List.of(PaymentResponse.builder().id(1L).build());
        when(queryService.getPaymentsByBooking(1L)).thenReturn(expected);

        List<PaymentResponse> result = paymentService.getPaymentsByBooking(1L);

        assertThat(result).isSameAs(expected);
        verify(queryService).getPaymentsByBooking(1L);
    }

    @Test
    void getTransactionHistoryShouldDelegateToQueryService() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<PaymentResponse> expected = new PageImpl<>(List.of());
        when(queryService.getTransactionHistory(pageable)).thenReturn(expected);

        Page<PaymentResponse> result = paymentService.getTransactionHistory(pageable);

        assertThat(result).isSameAs(expected);
        verify(queryService).getTransactionHistory(pageable);
    }

    @Test
    void getReceiptShouldDelegateToQueryService() {
        ReceiptResponse expected = ReceiptResponse.builder().transactionId("ref").build();
        when(queryService.getReceipt(1L)).thenReturn(expected);

        ReceiptResponse result = paymentService.getReceipt(1L);

        assertThat(result).isSameAs(expected);
        verify(queryService).getReceipt(1L);
    }

    @Test
    void getPaymentScheduleShouldDelegateToScheduleService() {
        PaymentScheduleResponse expected = PaymentScheduleResponse.builder().id(1L).build();
        when(scheduleService.getPaymentSchedule(1L)).thenReturn(expected);

        PaymentScheduleResponse result = paymentService.getPaymentSchedule(1L);

        assertThat(result).isSameAs(expected);
        verify(scheduleService).getPaymentSchedule(1L);
    }
}
