package com.ndobaak.backend.subscription.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.enums.PaymentChannel;
import com.ndobaak.backend.common.enums.PaymentMethod;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.PaymentType;
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
import com.ndobaak.backend.subscription.config.SubscriptionProperties;
import com.ndobaak.backend.subscription.dto.SubscribeRequest;
import com.ndobaak.backend.subscription.dto.SubscriptionResponse;
import com.ndobaak.backend.subscription.dto.SubscriptionMapper;
import com.ndobaak.backend.subscription.entity.Subscription;
import com.ndobaak.backend.subscription.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String RENEW_URL = "https://ndobaak.com/subscription";

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayRouter gatewayRouter;
    private final PaymentProperties paymentProperties;
    private final SubscriptionProperties subscriptionProperties;
    private final AuthService authService;
    private final NotificationDispatcher notificationDispatcher;
    private final SubscriptionMapper subscriptionMapper;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                                PaymentRepository paymentRepository,
                                PaymentGatewayRouter gatewayRouter,
                                PaymentProperties paymentProperties,
                                SubscriptionProperties subscriptionProperties,
                                AuthService authService,
                                NotificationDispatcher notificationDispatcher,
                                SubscriptionMapper subscriptionMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.gatewayRouter = gatewayRouter;
        this.paymentProperties = paymentProperties;
        this.subscriptionProperties = subscriptionProperties;
        this.authService = authService;
        this.notificationDispatcher = notificationDispatcher;
        this.subscriptionMapper = subscriptionMapper;
    }

    @Transactional
    public SubscriptionResponse subscribe(SubscribeRequest request) {
        User landlord = authService.getAuthenticatedUser();

        if (subscriptionRepository.existsByLandlordIdAndStatus(landlord.getId(), SubscriptionStatus.ACTIVE)) {
            throw new BusinessException("Vous avez déjà un abonnement actif.");
        }

        Subscription subscription = subscriptionRepository.findByLandlordId(landlord.getId())
                .orElseGet(() -> Subscription.builder()
                        .landlord(landlord)
                        .amount(subscriptionProperties.getAmount())
                        .maxListings(20)
                        .build());

        subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);

        String reference = UUID.randomUUID().toString();
        Payment payment = createSubscriptionPayment(landlord, request, reference);
        payment = paymentRepository.save(payment);

        PaymentInitiationResult result = initiateGatewayPayment(landlord, request, reference, payment);

        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setProviderReference(result.providerReference());
        paymentRepository.save(payment);

        subscription.setLastPayment(payment);
        subscriptionRepository.save(subscription);

        log.info("Subscription payment initiated for landlord={}, reference={}", landlord.getId(), reference);

        SubscriptionResponse response = subscriptionMapper.toResponse(subscription);
        response.setPaymentReference(reference);
        return response;
    }

    @Transactional
    public SubscriptionResponse renew(SubscribeRequest request) {
        User landlord = authService.getAuthenticatedUser();

        Subscription subscription = subscriptionRepository.findByLandlordId(landlord.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aucun abonnement trouvé."));

        if (subscription.getStatus() == SubscriptionStatus.PENDING_PAYMENT) {
            throw new BusinessException("Un paiement d'abonnement est déjà en cours.");
        }

        String reference = UUID.randomUUID().toString();
        Payment payment = createSubscriptionPayment(landlord, request, reference);
        payment = paymentRepository.save(payment);

        PaymentInitiationResult result = initiateGatewayPayment(landlord, request, reference, payment);

        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setProviderReference(result.providerReference());
        paymentRepository.save(payment);

        subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);
        subscription.setLastPayment(payment);
        subscriptionRepository.save(subscription);

        log.info("Subscription renewal initiated for landlord={}, reference={}", landlord.getId(), reference);

        SubscriptionResponse response = subscriptionMapper.toResponse(subscription);
        response.setPaymentReference(reference);
        return response;
    }

    @Transactional
    public void cancelPendingSubscription() {
        User landlord = authService.getAuthenticatedUser();

        Subscription subscription = subscriptionRepository.findByLandlordId(landlord.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aucun abonnement trouvé."));

        if (subscription.getStatus() != SubscriptionStatus.PENDING_PAYMENT) {
            throw new BusinessException("Seul un paiement en cours peut être annulé.");
        }

        Payment lastPayment = subscription.getLastPayment();
        if (lastPayment != null && lastPayment.getProviderReference() != null) {
            try {
                PaymentGateway gateway = gatewayRouter.resolveGatewayByProvider(lastPayment.getProviderName());
                gateway.cancelPayment(lastPayment.getProviderReference());
            } catch (Exception e) {
                log.warn("Failed to cancel payment on gateway: ref={}, error={}",
                        lastPayment.getProviderReference(), e.getMessage());
            }
            lastPayment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(lastPayment);
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);

        log.info("Pending subscription cancelled for landlord={}", landlord.getId());
    }

    @Transactional
    public SubscriptionResponse cancelAutoRenew() {
        User landlord = authService.getAuthenticatedUser();

        Subscription subscription = subscriptionRepository.findByLandlordIdAndStatus(
                        landlord.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("Aucun abonnement actif à modifier."));

        subscription.setAutoRenew(false);
        subscriptionRepository.save(subscription);

        return subscriptionMapper.toResponse(subscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getMySubscription() {
        User landlord = authService.getAuthenticatedUser();
        return subscriptionRepository.findByLandlordId(landlord.getId())
                .map(subscriptionMapper::toResponse)
                .orElse(null);
    }

    @Transactional
    public void deactivateExpiredSubscriptions() {
        List<Subscription> expired = subscriptionRepository
                .findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, LocalDate.now());

        for (Subscription subscription : expired) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);

            String endDateFormatted = subscription.getEndDate().format(DATE_FMT);

            notificationDispatcher.dispatch(
                    subscription.getLandlord().getId(),
                    NotificationType.SUBSCRIPTION_EXPIRED,
                    "Abonnement expiré",
                    "Votre abonnement Ndobaak a expiré. Renouvelez-le pour continuer à publier des annonces.",
                    subscription.getId(),
                    "SUBSCRIPTION",
                    Map.of(
                            "landlordName", subscription.getLandlord().getFirstName(),
                            "endDate", endDateFormatted,
                            "renewUrl", RENEW_URL
                    ),
                    Map.of()
            );

            log.info("Subscription expired for landlord={}", subscription.getLandlord().getId());
        }
    }

    @Transactional
    public void sendExpiryReminders() {
        LocalDate in30Days = LocalDate.now().plusDays(30);
        LocalDate in7Days = LocalDate.now().plusDays(7);

        sendReminderForDate(in30Days, 30);
        sendReminderForDate(in7Days, 7);
    }

    private void sendReminderForDate(LocalDate targetDate, int daysRemaining) {
        List<Subscription> expiringSoon = subscriptionRepository
                .findByStatusAndEndDateBetween(
                        SubscriptionStatus.ACTIVE,
                        targetDate,
                        targetDate.plusDays(1)
                );

        for (Subscription subscription : expiringSoon) {
            String endDateFormatted = subscription.getEndDate().format(DATE_FMT);
            String daysStr = String.valueOf(daysRemaining);

            notificationDispatcher.dispatch(
                    subscription.getLandlord().getId(),
                    NotificationType.SUBSCRIPTION_EXPIRING_SOON,
                    "Abonnement bientôt expiré",
                    "Votre abonnement Ndobaak expire dans " + daysRemaining + " jours. Renouvelez-le pour continuer à publier.",
                    subscription.getId(),
                    "SUBSCRIPTION",
                    Map.of(
                            "landlordName", subscription.getLandlord().getFirstName(),
                            "endDate", endDateFormatted,
                            "daysRemaining", daysStr,
                            "renewUrl", RENEW_URL
                    ),
                    Map.of("daysRemaining", daysStr)
            );
        }
    }

    private Payment createSubscriptionPayment(User landlord, SubscribeRequest request, String reference) {
        return Payment.builder()
                .amount(subscriptionProperties.getAmount())
                .commission(BigDecimal.ZERO)
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .channel(request.getOperator())
                .paymentType(PaymentType.SUBSCRIPTION)
                .transactionId(reference)
                .status(PaymentStatus.PENDING)
                .phoneNumber(request.getPhoneNumber())
                .payerName(landlord.getFirstName() + " " + landlord.getLastName())
                .build();
    }

    private PaymentInitiationResult initiateGatewayPayment(User landlord, SubscribeRequest request,
                                                            String reference, Payment payment) {
        PaymentGateway gateway = gatewayRouter.resolveGateway(request.getOperator());
        payment.setProviderName(gateway.getProviderName());

        PaymentGatewayRequest gatewayRequest = new PaymentGatewayRequest(
                reference,
                subscriptionProperties.getAmount(),
                "XAF",
                landlord.getFirstName() + " " + landlord.getLastName(),
                landlord.getEmail(),
                request.getPhoneNumber(),
                request.getOperator(),
                "Abonnement annuel Ndobaak - " + subscriptionProperties.getAmount().toPlainString() + " FCFA",
                paymentProperties.getFrontendCallbackUrl()
        );

        PaymentInitiationResult result = gateway.initiatePayment(gatewayRequest);
        try {
            gateway.triggerCharge(result.providerReference(), gatewayRequest);
        } catch (Exception e) {
            log.warn("USSD push failed for subscription (non-fatal): reference={}, error={}", reference, e.getMessage());
        }
        return result;
    }
}
