package com.ndobaak.backend.subscription.service;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.enums.SubscriptionStatus;
import com.ndobaak.backend.notification.service.NotificationDispatcher;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import com.ndobaak.backend.subscription.entity.Subscription;
import com.ndobaak.backend.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionEventHandlerImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private NotificationDispatcher notificationDispatcher;

    @InjectMocks
    private SubscriptionEventHandlerImpl handler;

    private User landlord;

    @BeforeEach
    void setUp() {
        landlord = buildLandlord(10L, "Alice", "Martin");
    }

    @Test
    void onPaymentSucceededShouldActivateSubscriptionAndDispatchNotification() {
        Payment payment = buildPayment(1L);
        Subscription subscription = buildSubscription(5L, SubscriptionStatus.PENDING_PAYMENT);
        subscription.setLastPayment(payment);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findByLastPaymentId(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        handler.onPaymentSucceeded(1L);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getStartDate()).isNotNull();
        assertThat(subscription.getEndDate()).isEqualTo(subscription.getStartDate().plusYears(1));
        verify(notificationDispatcher).dispatch(
                eq(10L),
                eq(NotificationType.SUBSCRIPTION_ACTIVATED),
                anyString(),
                anyString(),
                eq(5L),
                eq("SUBSCRIPTION"),
                anyMap(),
                anyMap()
        );
    }

    @Test
    void onPaymentSucceededShouldExtendEndDateWhenRenewing() {
        Payment payment = buildPayment(1L);
        Subscription subscription = buildSubscription(5L, SubscriptionStatus.ACTIVE);
        LocalDate existingEnd = LocalDate.now().plusDays(30);
        subscription.setEndDate(existingEnd);
        subscription.setLastPayment(payment);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findByLastPaymentId(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        handler.onPaymentSucceeded(1L);

        assertThat(subscription.getEndDate()).isEqualTo(existingEnd.plusYears(1));
    }

    @Test
    void onPaymentFailedShouldResetToPendingAndDispatchNotification() {
        Payment payment = buildPayment(1L);
        Subscription subscription = buildSubscription(5L, SubscriptionStatus.PENDING_PAYMENT);
        subscription.setLastPayment(payment);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findByLastPaymentId(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        handler.onPaymentFailed(1L);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
        verify(notificationDispatcher).dispatch(
                eq(10L),
                eq(NotificationType.PAYMENT_FAILED),
                anyString(),
                anyString(),
                eq(5L),
                eq("SUBSCRIPTION"),
                anyMap(),
                anyMap()
        );
    }

    @Test
    void onPaymentSucceededShouldDoNothingWhenPaymentNotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        handler.onPaymentSucceeded(99L);

        verify(subscriptionRepository, org.mockito.Mockito.never()).save(any());
        verify(notificationDispatcher, org.mockito.Mockito.never()).dispatch(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    private User buildLandlord(Long id, String firstName, String lastName) {
        Landlord user = Landlord.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(firstName.toLowerCase() + "@example.com")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Payment buildPayment(Long id) {
        Payment payment = new Payment();
        ReflectionTestUtils.setField(payment, "id", id);
        payment.setTransactionId("txn-" + id);
        return payment;
    }

    private Subscription buildSubscription(Long id, SubscriptionStatus status) {
        Subscription subscription = Subscription.builder()
                .landlord(landlord)
                .amount(BigDecimal.valueOf(100_000))
                .maxListings(20)
                .build();
        ReflectionTestUtils.setField(subscription, "id", id);
        subscription.setStatus(status);
        subscription.setStartDate(LocalDate.now().minusYears(1));
        subscription.setEndDate(LocalDate.now().plusDays(10));
        return subscription;
    }
}
