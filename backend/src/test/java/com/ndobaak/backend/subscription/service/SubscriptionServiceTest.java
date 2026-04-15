package com.ndobaak.backend.subscription.service;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.PaymentChannel;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.enums.SubscriptionStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.notification.service.NotificationDispatcher;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.gateway.PaymentGateway;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRequest;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRouter;
import com.ndobaak.backend.payment.gateway.PaymentInitiationResult;
import com.ndobaak.backend.payment.gateway.PaymentProperties;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import com.ndobaak.backend.subscription.dto.SubscribeRequest;
import com.ndobaak.backend.subscription.dto.SubscriptionMapper;
import com.ndobaak.backend.subscription.dto.SubscriptionResponse;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentGatewayRouter gatewayRouter;
    @Mock
    private PaymentProperties paymentProperties;
    @Mock
    private AuthService authService;
    @Mock
    private NotificationDispatcher notificationDispatcher;
    @Mock
    private SubscriptionMapper subscriptionMapper;
    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User landlord;
    private SubscribeRequest subscribeRequest;
    private Payment savedPayment;
    private PaymentInitiationResult initiationResult;
    private SubscriptionResponse subscriptionResponse;

    @BeforeEach
    void setUp() {
        landlord = buildLandlord(10L, "Alice", "Martin", "alice@example.com");
        subscribeRequest = buildSubscribeRequest(PaymentChannel.MTN_MOMO, "677000001");
        savedPayment = buildPayment(1L);
        initiationResult = new PaymentInitiationResult("PROV-REF-001", "https://pay.example.com/auth", "PENDING");
        subscriptionResponse = SubscriptionResponse.builder()
                .id(5L)
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .maxListings(20)
                .amount(BigDecimal.valueOf(100_000))
                .build();
    }

    @Test
    void subscribeShouldCreateNewSubscriptionWhenNoneExists() {
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(subscriptionRepository.existsByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE)).thenReturn(false);
        when(subscriptionRepository.findByLandlordId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(gatewayRouter.resolveGateway(PaymentChannel.MTN_MOMO)).thenReturn(paymentGateway);
        when(paymentGateway.getProviderName()).thenReturn("NotchPay");
        when(paymentGateway.initiatePayment(any(PaymentGatewayRequest.class))).thenReturn(initiationResult);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionMapper.toResponse(any(Subscription.class))).thenReturn(subscriptionResponse);
        when(paymentProperties.getFrontendCallbackUrl()).thenReturn("https://ndobaak.com/callback");

        SubscriptionResponse result = subscriptionService.subscribe(subscribeRequest);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void subscribeShouldReuseExistingSubscriptionWhenAlreadyExists() {
        Subscription existing = buildSubscription(5L, SubscriptionStatus.EXPIRED);
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(subscriptionRepository.existsByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE)).thenReturn(false);
        when(subscriptionRepository.findByLandlordId(10L)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(gatewayRouter.resolveGateway(PaymentChannel.MTN_MOMO)).thenReturn(paymentGateway);
        when(paymentGateway.getProviderName()).thenReturn("NotchPay");
        when(paymentGateway.initiatePayment(any(PaymentGatewayRequest.class))).thenReturn(initiationResult);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionMapper.toResponse(any(Subscription.class))).thenReturn(subscriptionResponse);
        when(paymentProperties.getFrontendCallbackUrl()).thenReturn("https://ndobaak.com/callback");

        subscriptionService.subscribe(subscribeRequest);

        assertThat(existing.getStatus()).isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
    }

    @Test
    void subscribeShouldThrowWhenLandlordAlreadyHasActiveSubscription() {
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(subscriptionRepository.existsByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> subscriptionService.subscribe(subscribeRequest))
                .isInstanceOf(BusinessException.class);

        verify(paymentRepository, never()).save(any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void subscribeShouldSetPaymentReferenceOnResponse() {
        SubscriptionResponse mutableResponse = SubscriptionResponse.builder()
                .id(5L).status(SubscriptionStatus.PENDING_PAYMENT).build();
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(subscriptionRepository.existsByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE)).thenReturn(false);
        when(subscriptionRepository.findByLandlordId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(gatewayRouter.resolveGateway(PaymentChannel.MTN_MOMO)).thenReturn(paymentGateway);
        when(paymentGateway.getProviderName()).thenReturn("NotchPay");
        when(paymentGateway.initiatePayment(any(PaymentGatewayRequest.class))).thenReturn(initiationResult);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionMapper.toResponse(any(Subscription.class))).thenReturn(mutableResponse);
        when(paymentProperties.getFrontendCallbackUrl()).thenReturn("https://ndobaak.com/callback");

        SubscriptionResponse result = subscriptionService.subscribe(subscribeRequest);

        assertThat(result.getPaymentReference()).isNotNull();
    }

    @Test
    void renewShouldInitiateNewPaymentForExistingSubscription() {
        Subscription subscription = buildSubscription(5L, SubscriptionStatus.EXPIRED);
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(subscriptionRepository.findByLandlordId(10L)).thenReturn(Optional.of(subscription));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(gatewayRouter.resolveGateway(PaymentChannel.MTN_MOMO)).thenReturn(paymentGateway);
        when(paymentGateway.getProviderName()).thenReturn("NotchPay");
        when(paymentGateway.initiatePayment(any(PaymentGatewayRequest.class))).thenReturn(initiationResult);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionMapper.toResponse(any(Subscription.class))).thenReturn(subscriptionResponse);
        when(paymentProperties.getFrontendCallbackUrl()).thenReturn("https://ndobaak.com/callback");

        SubscriptionResponse result = subscriptionService.renew(subscribeRequest);

        assertThat(result).isNotNull();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void renewShouldThrowWhenNoSubscriptionExists() {
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(subscriptionRepository.findByLandlordId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.renew(subscribeRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void renewShouldThrowWhenPaymentAlreadyPending() {
        Subscription subscription = buildSubscription(5L, SubscriptionStatus.PENDING_PAYMENT);
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(subscriptionRepository.findByLandlordId(10L)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.renew(subscribeRequest))
                .isInstanceOf(BusinessException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void cancelAutoRenewShouldDisableAutoRenewOnActiveSubscription() {
        Subscription subscription = buildSubscription(5L, SubscriptionStatus.ACTIVE);
        subscription.setAutoRenew(true);
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(subscriptionRepository.findByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionMapper.toResponse(any(Subscription.class))).thenReturn(subscriptionResponse);

        subscriptionService.cancelAutoRenew();

        assertThat(subscription.isAutoRenew()).isFalse();
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void cancelAutoRenewShouldThrowWhenNoActiveSubscription() {
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(subscriptionRepository.findByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.cancelAutoRenew())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getMySubscriptionShouldReturnResponseWhenExists() {
        Subscription subscription = buildSubscription(5L, SubscriptionStatus.ACTIVE);
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(subscriptionRepository.findByLandlordId(10L)).thenReturn(Optional.of(subscription));
        when(subscriptionMapper.toResponse(subscription)).thenReturn(subscriptionResponse);

        SubscriptionResponse result = subscriptionService.getMySubscription();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(5L);
    }

    @Test
    void getMySubscriptionShouldReturnNullWhenNoneExists() {
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(subscriptionRepository.findByLandlordId(10L)).thenReturn(Optional.empty());

        SubscriptionResponse result = subscriptionService.getMySubscription();

        assertThat(result).isNull();
    }

    @Test
    void hasActiveSubscriptionShouldReturnTrueWhenActive() {
        when(subscriptionRepository.existsByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE)).thenReturn(true);

        assertThat(subscriptionService.hasActiveSubscription(10L)).isTrue();
    }

    @Test
    void hasActiveSubscriptionShouldReturnFalseWhenNotActive() {
        when(subscriptionRepository.existsByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE)).thenReturn(false);

        assertThat(subscriptionService.hasActiveSubscription(10L)).isFalse();
    }

    @Test
    void getMaxListingsShouldReturnLimitFromActiveSubscription() {
        Subscription subscription = buildSubscription(5L, SubscriptionStatus.ACTIVE);
        subscription.setMaxListings(20);
        when(subscriptionRepository.findByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));

        int max = subscriptionService.getMaxListings(10L);

        assertThat(max).isEqualTo(20);
    }

    @Test
    void getMaxListingsShouldReturnZeroWhenNoActiveSubscription() {
        when(subscriptionRepository.findByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        int max = subscriptionService.getMaxListings(10L);

        assertThat(max).isZero();
    }

    @Test
    void deactivateExpiredSubscriptionsShouldMarkExpiredAndDispatchNotification() {
        Subscription subscription = buildSubscription(5L, SubscriptionStatus.ACTIVE);
        subscription.setEndDate(LocalDate.now().minusDays(1));
        when(subscriptionRepository.findByStatusAndEndDateBefore(eq(SubscriptionStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.deactivateExpiredSubscriptions();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(notificationDispatcher).dispatch(
                eq(10L),
                eq(NotificationType.SUBSCRIPTION_EXPIRED),
                anyString(),
                anyString(),
                eq(5L),
                eq("SUBSCRIPTION"),
                anyMap(),
                anyMap()
        );
    }

    @Test
    void activateSubscriptionShouldSetActiveStatusAndDispatchNotification() {
        Payment payment = buildPayment(1L);
        Subscription subscription = buildSubscription(5L, SubscriptionStatus.PENDING_PAYMENT);
        subscription.setLastPayment(payment);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findAll()).thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.activateSubscription(1L);

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

    private User buildLandlord(Long id, String firstName, String lastName, String email) {
        Landlord user = Landlord.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private SubscribeRequest buildSubscribeRequest(PaymentChannel operator, String phoneNumber) {
        SubscribeRequest request = new SubscribeRequest();
        request.setOperator(operator);
        request.setPhoneNumber(phoneNumber);
        return request;
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
