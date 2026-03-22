package com.ndobaak.backend.payment.service;

import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.PaymentType;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.gateway.PaymentGateway;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRouter;
import com.ndobaak.backend.payment.gateway.PaymentProperties;
import com.ndobaak.backend.payment.gateway.WebhookEvent;
import com.ndobaak.backend.payment.repository.InstallmentRepository;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import com.ndobaak.backend.payment.repository.PaymentScheduleRepository;
import com.ndobaak.backend.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentScheduleRepository scheduleRepository;
    @Mock private InstallmentRepository installmentRepository;
    @Mock private PaymentGatewayRouter gatewayRouter;
    @Mock private PaymentProperties paymentProperties;
    @Mock private SubscriptionService subscriptionService;
    @Mock private PaymentNotificationService notificationService;
    @Mock private PaymentGateway gateway;

    @InjectMocks
    private PaymentWebhookService service;

    private Student student;
    private Booking booking;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setEmail("john@test.com");

        Listing listing = Listing.builder()
                .id(100L).title("Studio Eyang")
                .monthlyPrice(new BigDecimal("50000"))
                .landlord(new com.ndobaak.backend.auth.entity.Landlord())
                .build();
        listing.getLandlord().setId(10L);

        booking = Booking.builder()
                .student(student).listing(listing)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 10, 1))
                .totalAmount(new BigDecimal("300000"))
                .status(BookingStatus.CONFIRMED)
                .build();
        booking.setId(1L);
    }

    @Test
    void handleWebhookCompleteShouldUpdateStatusAndNotify() {
        Payment payment = Payment.builder()
                .booking(booking).amount(new BigDecimal("300000"))
                .transactionId("ref-123").status(PaymentStatus.PROCESSING)
                .payerName("John Doe").build();
        payment.setId(1L);

        WebhookEvent event = new WebhookEvent(
                "payment.complete", "ref-123", "complete",
                new BigDecimal("315000"), "XAF", Map.of());

        when(gatewayRouter.resolveGatewayByProvider("mock")).thenReturn(gateway);
        when(gateway.verifyWebhookSignature("payload", "sig")).thenReturn(true);
        when(gateway.parseWebhookPayload("payload")).thenReturn(event);
        when(paymentRepository.findByProviderReference("ref-123")).thenReturn(Optional.of(payment));
        when(scheduleRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        service.handleWebhook("mock", "payload", "sig");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getPaidAt()).isNotNull();
        verify(paymentRepository).save(payment);
        verify(notificationService).sendPaymentCompletionNotifications(payment, booking);
    }

    @Test
    void handleWebhookFailedShouldUpdateStatusAndNotifyStudent() {
        Payment payment = Payment.builder()
                .booking(booking).transactionId("ref-456")
                .status(PaymentStatus.PROCESSING).build();
        payment.setId(1L);

        WebhookEvent event = new WebhookEvent(
                "payment.failed", "ref-456", "failed",
                new BigDecimal("300000"), "XAF", Map.of());

        when(gatewayRouter.resolveGatewayByProvider("mock")).thenReturn(gateway);
        when(gateway.verifyWebhookSignature("payload", "sig")).thenReturn(true);
        when(gateway.parseWebhookPayload("payload")).thenReturn(event);
        when(paymentRepository.findByProviderReference("ref-456")).thenReturn(Optional.of(payment));

        service.handleWebhook("mock", "payload", "sig");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(notificationService).sendPaymentFailedNotification(payment, booking);
    }

    @Test
    void handleWebhookShouldThrowOnInvalidSignature() {
        when(gatewayRouter.resolveGatewayByProvider("mock")).thenReturn(gateway);
        when(gateway.verifyWebhookSignature("payload", "bad-sig")).thenReturn(false);

        assertThatThrownBy(() -> service.handleWebhook("mock", "payload", "bad-sig"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Signature");
    }

    @Test
    void handleWebhookShouldSkipAlreadyCompletedPayment() {
        Payment payment = Payment.builder()
                .booking(booking).transactionId("ref-789")
                .status(PaymentStatus.COMPLETED).build();
        payment.setId(1L);

        WebhookEvent event = new WebhookEvent(
                "payment.complete", "ref-789", "complete",
                new BigDecimal("300000"), "XAF", Map.of());

        when(gatewayRouter.resolveGatewayByProvider("mock")).thenReturn(gateway);
        when(gateway.verifyWebhookSignature("payload", "sig")).thenReturn(true);
        when(gateway.parseWebhookPayload("payload")).thenReturn(event);
        when(paymentRepository.findByProviderReference("ref-789")).thenReturn(Optional.of(payment));

        service.handleWebhook("mock", "payload", "sig");

        verify(paymentRepository, never()).save(any());
        verify(notificationService, never()).sendPaymentCompletionNotifications(any(), any());
    }
}
