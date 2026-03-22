package com.ndobaak.backend.payment.service;

import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.common.enums.InstallmentStatus;
import com.ndobaak.backend.common.enums.PaymentMethod;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.PaymentType;
import com.ndobaak.backend.common.enums.ScheduleFrequency;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.payment.entity.Installment;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.entity.PaymentSchedule;
import com.ndobaak.backend.payment.gateway.PaymentGateway;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRequest;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRouter;
import com.ndobaak.backend.payment.gateway.PaymentInitiationResult;
import com.ndobaak.backend.payment.gateway.PaymentProperties;
import com.ndobaak.backend.payment.gateway.WebhookEvent;
import com.ndobaak.backend.payment.repository.InstallmentRepository;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import com.ndobaak.backend.payment.repository.PaymentScheduleRepository;
import com.ndobaak.backend.subscription.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentScheduleRepository scheduleRepository;
    private final InstallmentRepository installmentRepository;
    private final PaymentGatewayRouter gatewayRouter;
    private final PaymentProperties paymentProperties;
    private final SubscriptionService subscriptionService;
    private final PaymentNotificationService notificationService;

    public PaymentWebhookService(PaymentRepository paymentRepository,
                                 PaymentScheduleRepository scheduleRepository,
                                 InstallmentRepository installmentRepository,
                                 PaymentGatewayRouter gatewayRouter,
                                 PaymentProperties paymentProperties,
                                 SubscriptionService subscriptionService,
                                 PaymentNotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.scheduleRepository = scheduleRepository;
        this.installmentRepository = installmentRepository;
        this.gatewayRouter = gatewayRouter;
        this.paymentProperties = paymentProperties;
        this.subscriptionService = subscriptionService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void handleWebhook(String providerName, String payload, String signature) {
        PaymentGateway gateway = gatewayRouter.resolveGatewayByProvider(providerName);

        if (!gateway.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid webhook signature from provider={}", providerName);
            throw new BusinessException("Signature webhook invalide.");
        }

        WebhookEvent event = gateway.parseWebhookPayload(payload);

        Payment payment = paymentRepository.findByProviderReference(event.reference())
                .orElseThrow(() -> {
                    log.warn("Webhook received for unknown reference={}", event.reference());
                    return new ResourceNotFoundException("Payment", "reference", event.reference());
                });

        if (payment.getStatus() == PaymentStatus.COMPLETED
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("Payment already processed: reference={}, status={}", event.reference(), payment.getStatus());
            return;
        }

        String eventType = event.type();
        if (eventType == null) {
            log.warn("Webhook event type is null, raw payload keys: {}", event.rawData().keySet());
            return;
        }
        switch (eventType) {
            case "payment.complete" -> handlePaymentComplete(payment);
            case "payment.failed" -> handlePaymentFailed(payment);
            default -> log.warn("Unhandled webhook event type: {}", eventType);
        }
    }

    private void handlePaymentComplete(Payment payment) {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        if (payment.getPaymentType() == PaymentType.SUBSCRIPTION || payment.getBooking() == null) {
            subscriptionService.activateSubscription(payment.getId());
            log.info("Subscription payment completed: reference={}", payment.getTransactionId());
            return;
        }

        updateInstallmentStatus(payment);

        Booking booking = payment.getBooking();
        boolean nextBlockTriggered = scheduleRepository.findByBookingId(booking.getId())
                .filter(s -> s.getFrequency() == ScheduleFrequency.BLOCK)
                .map(s -> triggerNextBlock(payment, s))
                .orElse(false);

        if (!nextBlockTriggered) {
            notificationService.sendPaymentCompletionNotifications(payment, booking);
        }
    }

    private boolean triggerNextBlock(Payment completedPayment, PaymentSchedule schedule) {
        List<Installment> pendingBlocks = installmentRepository.findByPaymentScheduleId(schedule.getId())
                .stream()
                .filter(i -> i.getStatus() == InstallmentStatus.PENDING && i.getPayment() == null)
                .sorted(Comparator.comparingInt(Installment::getInstallmentNumber))
                .toList();

        if (pendingBlocks.isEmpty()) {
            notificationService.sendPaymentCompletionNotifications(completedPayment, schedule.getBooking());
            return false;
        }

        Installment nextBlock = pendingBlocks.get(0);
        BigDecimal commission = nextBlock.getAmount()
                .multiply(paymentProperties.getCommissionRate())
                .setScale(0, RoundingMode.CEILING);

        String reference = UUID.randomUUID().toString();

        Payment nextPayment = Payment.builder()
                .booking(completedPayment.getBooking())
                .amount(nextBlock.getAmount())
                .commission(commission)
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .mobileMoneyOperator(completedPayment.getMobileMoneyOperator())
                .paymentType(PaymentType.INSTALLMENT)
                .transactionId(reference)
                .status(PaymentStatus.PENDING)
                .phoneNumber(completedPayment.getPhoneNumber())
                .payerName(completedPayment.getPayerName())
                .build();

        nextPayment = paymentRepository.save(nextPayment);
        nextBlock.setPayment(nextPayment);
        installmentRepository.save(nextBlock);

        PaymentGateway gateway = gatewayRouter.resolveGateway(completedPayment.getMobileMoneyOperator());
        BigDecimal totalCharged = nextBlock.getAmount();
        String description = "Bloc " + nextBlock.getInstallmentNumber()
                + "/" + schedule.getNumberOfInstallments()
                + " - " + completedPayment.getBooking().getListing().getTitle();

        PaymentGatewayRequest gatewayRequest = new PaymentGatewayRequest(
                reference,
                totalCharged,
                "XAF",
                completedPayment.getPayerName(),
                completedPayment.getBooking().getStudent().getEmail(),
                completedPayment.getPhoneNumber(),
                completedPayment.getMobileMoneyOperator(),
                description,
                paymentProperties.getFrontendCallbackUrl()
        );

        try {
            PaymentInitiationResult result = gateway.initiatePayment(gatewayRequest);
            nextPayment.setStatus(PaymentStatus.PROCESSING);
            nextPayment.setProviderName(gateway.getProviderName());
            nextPayment.setProviderReference(result.providerReference());
            paymentRepository.save(nextPayment);
            gateway.triggerCharge(result.providerReference(), gatewayRequest);
            log.info("Next block triggered: block={}/{}, reference={}",
                    nextBlock.getInstallmentNumber(), schedule.getNumberOfInstallments(), reference);
        } catch (Exception e) {
            log.error("Failed to trigger next block: block={}, error={}",
                    nextBlock.getInstallmentNumber(), e.getMessage());
            nextPayment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(nextPayment);
        }
        return true;
    }

    private void handlePaymentFailed(Payment payment) {
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        if (payment.getPaymentType() == PaymentType.SUBSCRIPTION || payment.getBooking() == null) {
            subscriptionService.onSubscriptionPaymentFailed(payment.getId());
            log.warn("Subscription payment failed: reference={}", payment.getTransactionId());
            return;
        }

        notificationService.sendPaymentFailedNotification(payment, payment.getBooking());
    }

    private void updateInstallmentStatus(Payment payment) {
        if (payment.getBooking() == null) {
            return;
        }
        installmentRepository.findByPaymentScheduleId(
                scheduleRepository.findByBookingId(payment.getBooking().getId())
                        .map(PaymentSchedule::getId)
                        .orElse(-1L)
        ).stream()
                .filter(i -> i.getPayment() != null && i.getPayment().getId().equals(payment.getId()))
                .findFirst()
                .ifPresent(installment -> {
                    installment.setStatus(InstallmentStatus.PAID);
                    installment.setPaidAt(LocalDateTime.now());
                    installmentRepository.save(installment);
                });
    }
}
