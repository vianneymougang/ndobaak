package com.ndobaak.backend.subscription.service;

import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.enums.SubscriptionStatus;
import com.ndobaak.backend.notification.service.NotificationDispatcher;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import com.ndobaak.backend.subscription.entity.Subscription;
import com.ndobaak.backend.subscription.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class SubscriptionEventHandlerImpl implements SubscriptionPaymentEventHandler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEventHandlerImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String RENEW_URL = "https://ndobaak.com/subscription";

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationDispatcher notificationDispatcher;

    public SubscriptionEventHandlerImpl(SubscriptionRepository subscriptionRepository,
                                        PaymentRepository paymentRepository,
                                        NotificationDispatcher notificationDispatcher) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.notificationDispatcher = notificationDispatcher;
    }

    @Override
    @Transactional
    public void onPaymentSucceeded(Long paymentId) {
        paymentRepository.findById(paymentId).ifPresent(payment ->
                subscriptionRepository.findByLastPaymentId(paymentId).ifPresent(subscription -> {
                    LocalDate start = LocalDate.now();
                    if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                        start = subscription.getEndDate();
                    }
                    subscription.setStatus(SubscriptionStatus.ACTIVE);
                    subscription.setStartDate(start);
                    subscription.setEndDate(start.plusYears(1));
                    subscriptionRepository.save(subscription);

                    String landlordName = subscription.getLandlord().getFirstName();
                    String endDateFormatted = subscription.getEndDate().format(DATE_FMT);

                    notificationDispatcher.dispatch(
                            subscription.getLandlord().getId(),
                            NotificationType.SUBSCRIPTION_ACTIVATED,
                            "Abonnement activé",
                            "Votre abonnement Ndobaak est actif jusqu'au "
                                    + endDateFormatted + ". Vous pouvez publier jusqu'à "
                                    + subscription.getMaxListings() + " annonces.",
                            subscription.getId(),
                            "SUBSCRIPTION",
                            Map.of(
                                    "landlordName", landlordName,
                                    "endDate", endDateFormatted,
                                    "renewUrl", RENEW_URL
                            ),
                            Map.of()
                    );

                    log.info("Subscription activated for landlord={}, until={}",
                            subscription.getLandlord().getId(), subscription.getEndDate());
                })
        );
    }

    @Override
    @Transactional
    public void onPaymentFailed(Long paymentId) {
        paymentRepository.findById(paymentId).ifPresent(payment ->
                subscriptionRepository.findByLastPaymentId(paymentId).ifPresent(subscription -> {
                    subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);
                    subscriptionRepository.save(subscription);

                    notificationDispatcher.dispatch(
                            subscription.getLandlord().getId(),
                            NotificationType.PAYMENT_FAILED,
                            "Échec du paiement d'abonnement",
                            "Le paiement de votre abonnement a échoué. Veuillez réessayer.",
                            subscription.getId(),
                            "SUBSCRIPTION",
                            Map.of("landlordName", subscription.getLandlord().getFirstName()),
                            Map.of()
                    );
                })
        );
    }
}
