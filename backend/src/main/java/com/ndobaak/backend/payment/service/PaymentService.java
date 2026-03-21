package com.ndobaak.backend.payment.service;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.InstallmentStatus;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.enums.PaymentMethod;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.PaymentType;
import com.ndobaak.backend.common.enums.ScheduleFrequency;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.notification.service.NotificationDispatcher;
import com.ndobaak.backend.subscription.service.SubscriptionService;
import com.ndobaak.backend.payment.dto.InitiatePaymentRequest;
import com.ndobaak.backend.payment.dto.PayInstallmentRequest;
import com.ndobaak.backend.payment.dto.PaymentResponse;
import com.ndobaak.backend.payment.dto.PaymentScheduleResponse;
import com.ndobaak.backend.payment.dto.ReceiptResponse;
import com.ndobaak.backend.payment.entity.Installment;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.entity.PaymentSchedule;
import com.ndobaak.backend.payment.gateway.PaymentGateway;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRequest;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRouter;
import com.ndobaak.backend.payment.gateway.PaymentInitiationResult;
import com.ndobaak.backend.payment.gateway.PaymentProperties;
import com.ndobaak.backend.payment.gateway.WebhookEvent;
import com.ndobaak.backend.payment.mapper.PaymentMapper;
import com.ndobaak.backend.payment.repository.InstallmentRepository;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import com.ndobaak.backend.payment.repository.PaymentScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentScheduleRepository scheduleRepository;
    private final InstallmentRepository installmentRepository;
    private final BookingRepository bookingRepository;
    private final AuthService authService;
    private final NotificationDispatcher notificationDispatcher;
    private final PaymentGatewayRouter gatewayRouter;
    private final PaymentMapper paymentMapper;
    private final PaymentProperties paymentProperties;
    private final SubscriptionService subscriptionService;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentScheduleRepository scheduleRepository,
                          InstallmentRepository installmentRepository,
                          BookingRepository bookingRepository,
                          AuthService authService,
                          NotificationDispatcher notificationDispatcher,
                          PaymentGatewayRouter gatewayRouter,
                          PaymentMapper paymentMapper,
                          PaymentProperties paymentProperties,
                          SubscriptionService subscriptionService) {
        this.paymentRepository = paymentRepository;
        this.scheduleRepository = scheduleRepository;
        this.installmentRepository = installmentRepository;
        this.bookingRepository = bookingRepository;
        this.authService = authService;
        this.notificationDispatcher = notificationDispatcher;
        this.gatewayRouter = gatewayRouter;
        this.paymentMapper = paymentMapper;
        this.paymentProperties = paymentProperties;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request) {
        User student = authService.getAuthenticatedUser();
        Booking booking = findBookingById(request.getBookingId());

        validateBookingOwnership(booking, student);
        validateBookingConfirmed(booking);
        validateNoActivePayment(booking);

        BigDecimal paymentAmount;
        if (request.getPaymentType() == PaymentType.INTEGRAL) {
            paymentAmount = booking.getTotalAmount();
        } else {
            validateInstallmentEligibility(booking);
            ScheduleFrequency frequency = request.getInstallmentFrequency() != null
                    ? request.getInstallmentFrequency()
                    : ScheduleFrequency.MONTHLY;
            int numberOfInstallments = calculateNumberOfInstallments(booking, frequency);
            paymentAmount = booking.getTotalAmount()
                    .divide(BigDecimal.valueOf(numberOfInstallments), 0, RoundingMode.FLOOR);
        }

        BigDecimal commission = paymentAmount
                .multiply(paymentProperties.getCommissionRate())
                .setScale(0, RoundingMode.CEILING);

        String reference = UUID.randomUUID().toString();

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(paymentAmount)
                .commission(commission)
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .mobileMoneyOperator(request.getOperator())
                .paymentType(request.getPaymentType())
                .transactionId(reference)
                .status(PaymentStatus.PENDING)
                .phoneNumber(request.getPhoneNumber())
                .payerName(student.getFirstName() + " " + student.getLastName())
                .build();

        payment = paymentRepository.save(payment);

        if (request.getPaymentType() == PaymentType.INSTALLMENT) {
            ScheduleFrequency frequency = request.getInstallmentFrequency() != null
                    ? request.getInstallmentFrequency()
                    : ScheduleFrequency.MONTHLY;
            createPaymentSchedule(booking, frequency, payment);
        }

        PaymentGateway gateway = gatewayRouter.resolveGateway(request.getOperator());
        BigDecimal totalCharged = paymentAmount.add(commission);

        PaymentGatewayRequest gatewayRequest = new PaymentGatewayRequest(
                reference,
                totalCharged,
                "XAF",
                payment.getPayerName(),
                student.getEmail(),
                request.getPhoneNumber(),
                request.getOperator(),
                "Paiement Ndobaak - " + booking.getListing().getTitle(),
                paymentProperties.getFrontendCallbackUrl()
        );

        PaymentInitiationResult result = gateway.initiatePayment(gatewayRequest);

        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setProviderName(gateway.getProviderName());
        payment.setProviderReference(result.providerReference());
        paymentRepository.save(payment);

        log.info("Payment initiated: reference={}, provider={}, amount={} XAF",
                reference, gateway.getProviderName(), totalCharged);

        PaymentResponse response = paymentMapper.toResponse(payment);
        response.setAuthorizationUrl(result.authorizationUrl());
        return response;
    }

    @Transactional
    public PaymentResponse payInstallment(Long installmentId, PayInstallmentRequest request) {
        User student = authService.getAuthenticatedUser();
        Installment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Installment", "id", installmentId));

        Booking booking = installment.getPaymentSchedule().getBooking();
        validateBookingOwnership(booking, student);

        if (installment.getStatus() != InstallmentStatus.PENDING
                && installment.getStatus() != InstallmentStatus.OVERDUE) {
            throw new BusinessException("Cette échéance n'est pas en attente de paiement.");
        }

        BigDecimal commission = installment.getAmount()
                .multiply(paymentProperties.getCommissionRate())
                .setScale(0, RoundingMode.CEILING);

        String reference = UUID.randomUUID().toString();

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(installment.getAmount())
                .commission(commission)
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .mobileMoneyOperator(request.getOperator())
                .paymentType(PaymentType.INSTALLMENT)
                .transactionId(reference)
                .status(PaymentStatus.PENDING)
                .phoneNumber(request.getPhoneNumber())
                .payerName(student.getFirstName() + " " + student.getLastName())
                .build();

        payment = paymentRepository.save(payment);
        installment.setPayment(payment);
        installmentRepository.save(installment);

        PaymentGateway gateway = gatewayRouter.resolveGateway(request.getOperator());
        BigDecimal totalCharged = installment.getAmount().add(commission);

        PaymentGatewayRequest gatewayRequest = new PaymentGatewayRequest(
                reference,
                totalCharged,
                "XAF",
                payment.getPayerName(),
                student.getEmail(),
                request.getPhoneNumber(),
                request.getOperator(),
                "Échéance " + installment.getInstallmentNumber() + " - " + booking.getListing().getTitle(),
                paymentProperties.getFrontendCallbackUrl()
        );

        PaymentInitiationResult result = gateway.initiatePayment(gatewayRequest);

        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setProviderName(gateway.getProviderName());
        payment.setProviderReference(result.providerReference());
        paymentRepository.save(payment);

        PaymentResponse response = paymentMapper.toResponse(payment);
        response.setAuthorizationUrl(result.authorizationUrl());
        return response;
    }

    @Transactional
    public void handleWebhook(String providerName, String payload, String signature) {
        PaymentGateway gateway = gatewayRouter.resolveGatewayByProvider(providerName);

        if (!gateway.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid webhook signature from provider={}", providerName);
            throw new BusinessException("Signature webhook invalide.");
        }

        WebhookEvent event = gateway.parseWebhookPayload(payload);

        Payment payment = paymentRepository.findByTransactionId(event.reference())
                .orElseThrow(() -> {
                    log.warn("Webhook received for unknown reference={}", event.reference());
                    return new ResourceNotFoundException("Payment", "reference", event.reference());
                });

        if (payment.getStatus() == PaymentStatus.COMPLETED
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("Payment already processed: reference={}, status={}", event.reference(), payment.getStatus());
            return;
        }

        switch (event.type()) {
            case "payment.complete" -> handlePaymentComplete(payment);
            case "payment.failed" -> handlePaymentFailed(payment);
            default -> log.warn("Unhandled webhook event type: {}", event.type());
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByBooking(Long bookingId) {
        User currentUser = authService.getAuthenticatedUser();
        Booking booking = findBookingById(bookingId);
        validateBookingAccess(booking, currentUser);

        return paymentRepository.findByBookingId(bookingId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getTransactionHistory(Pageable pageable) {
        User currentUser = authService.getAuthenticatedUser();

        Page<Payment> payments;
        if (currentUser instanceof Student) {
            payments = paymentRepository.findByBookingStudentId(currentUser.getId(), pageable);
        } else if (currentUser instanceof Landlord) {
            payments = paymentRepository.findByBookingListingLandlordId(currentUser.getId(), pageable);
        } else {
            throw new ForbiddenException("Accès non autorisé à l'historique des transactions.");
        }

        return payments.map(paymentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(Long paymentId) {
        User currentUser = authService.getAuthenticatedUser();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        validateBookingAccess(payment.getBooking(), currentUser);

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException("Le reçu n'est disponible que pour les paiements complétés.");
        }

        return paymentMapper.toReceiptResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentScheduleResponse getPaymentSchedule(Long bookingId) {
        User currentUser = authService.getAuthenticatedUser();
        Booking booking = findBookingById(bookingId);
        validateBookingAccess(booking, currentUser);

        PaymentSchedule schedule = scheduleRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun échéancier trouvé pour cette réservation."));

        return paymentMapper.toScheduleResponse(schedule);
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
        String amountFormatted = formatAmount(payment.getAmount());

        notificationDispatcher.dispatch(
                booking.getStudent().getId(),
                NotificationType.PAYMENT_RECEIVED,
                "Paiement reçu",
                "Votre paiement de " + amountFormatted + " FCFA pour \""
                        + booking.getListing().getTitle() + "\" a été confirmé.",
                payment.getId(),
                "PAYMENT",
                Map.of(
                        "payerName", payment.getPayerName(),
                        "amount", amountFormatted + " FCFA",
                        "listingTitle", booking.getListing().getTitle(),
                        "receiptUrl", ""
                ),
                Map.of(
                        "amount", amountFormatted,
                        "reference", payment.getTransactionId()
                )
        );

        notificationDispatcher.dispatch(
                booking.getListing().getLandlord().getId(),
                NotificationType.PAYMENT_RECEIVED,
                "Paiement reçu",
                "Un paiement de " + amountFormatted + " FCFA a été reçu pour \""
                        + booking.getListing().getTitle() + "\".",
                payment.getId(),
                "PAYMENT",
                Map.of(
                        "payerName", payment.getPayerName(),
                        "amount", amountFormatted + " FCFA",
                        "listingTitle", booking.getListing().getTitle(),
                        "receiptUrl", ""
                ),
                Map.of(
                        "amount", amountFormatted,
                        "reference", payment.getTransactionId()
                )
        );

        log.info("Payment completed: reference={}, amount={}", payment.getTransactionId(), payment.getAmount());
    }

    private void handlePaymentFailed(Payment payment) {
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        if (payment.getPaymentType() == PaymentType.SUBSCRIPTION || payment.getBooking() == null) {
            subscriptionService.onSubscriptionPaymentFailed(payment.getId());
            log.warn("Subscription payment failed: reference={}", payment.getTransactionId());
            return;
        }

        Booking booking = payment.getBooking();

        notificationDispatcher.dispatch(
                booking.getStudent().getId(),
                NotificationType.PAYMENT_FAILED,
                "Échec du paiement",
                "Votre paiement pour \"" + booking.getListing().getTitle()
                        + "\" a échoué. Veuillez réessayer.",
                payment.getId(),
                "PAYMENT",
                Map.of("listingTitle", booking.getListing().getTitle()),
                Map.of()
        );

        log.warn("Payment failed: reference={}", payment.getTransactionId());
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

    private PaymentSchedule createPaymentSchedule(Booking booking, ScheduleFrequency frequency, Payment firstPayment) {
        int numberOfInstallments = calculateNumberOfInstallments(booking, frequency);
        BigDecimal installmentAmount = booking.getTotalAmount()
                .divide(BigDecimal.valueOf(numberOfInstallments), 0, RoundingMode.FLOOR);
        BigDecimal lastInstallmentAmount = booking.getTotalAmount()
                .subtract(installmentAmount.multiply(BigDecimal.valueOf(numberOfInstallments - 1)));

        PaymentSchedule schedule = PaymentSchedule.builder()
                .booking(booking)
                .totalAmount(booking.getTotalAmount())
                .frequency(frequency)
                .numberOfInstallments(numberOfInstallments)
                .build();

        schedule = scheduleRepository.save(schedule);

        List<Installment> installments = new ArrayList<>();
        int monthIncrement = frequency == ScheduleFrequency.MONTHLY ? 1 : 3;

        for (int i = 1; i <= numberOfInstallments; i++) {
            BigDecimal amount = (i == numberOfInstallments) ? lastInstallmentAmount : installmentAmount;
            LocalDate dueDate = booking.getStartDate().plusMonths((long) (i - 1) * monthIncrement);

            Installment installment = Installment.builder()
                    .paymentSchedule(schedule)
                    .installmentNumber(i)
                    .amount(amount)
                    .dueDate(dueDate)
                    .status(InstallmentStatus.PENDING)
                    .build();

            if (i == 1) {
                installment.setPayment(firstPayment);
            }

            installments.add(installmentRepository.save(installment));
        }

        schedule.setInstallments(installments);
        return schedule;
    }

    private int calculateNumberOfInstallments(Booking booking, ScheduleFrequency frequency) {
        long months = java.time.temporal.ChronoUnit.MONTHS.between(booking.getStartDate(), booking.getEndDate());
        if (months < 1) months = 1;

        return switch (frequency) {
            case MONTHLY -> (int) months;
            case QUARTERLY -> Math.max(1, (int) (months / 3));
        };
    }

    private Booking findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
    }

    private void validateBookingOwnership(Booking booking, User student) {
        if (!booking.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à effectuer un paiement pour cette réservation.");
        }
    }

    private void validateBookingConfirmed(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Le paiement n'est possible que pour une réservation confirmée.");
        }
    }

    private void validateNoActivePayment(Booking booking) {
        List<Payment> existingPayments = paymentRepository.findByBookingId(booking.getId());

        boolean hasProcessing = existingPayments.stream()
                .anyMatch(p -> p.getStatus() == PaymentStatus.PROCESSING);
        if (hasProcessing) {
            throw new BusinessException("Un paiement est déjà en cours pour cette réservation.");
        }

        boolean hasCompletedIntegral = existingPayments.stream()
                .anyMatch(p -> p.getStatus() == PaymentStatus.COMPLETED
                        && p.getPaymentType() == PaymentType.INTEGRAL);
        if (hasCompletedIntegral) {
            throw new BusinessException("Cette réservation a déjà été payée intégralement.");
        }

        boolean hasSchedule = scheduleRepository.findByBookingId(booking.getId()).isPresent();
        if (hasSchedule) {
            throw new BusinessException(
                    "Un échéancier existe déjà pour cette réservation. Utilisez le paiement par échéance.");
        }
    }

    private void validateInstallmentEligibility(Booking booking) {
        if (booking.getTotalAmount().compareTo(paymentProperties.getInstallmentThreshold()) < 0) {
            throw new BusinessException(
                    "Le paiement en plusieurs fois n'est disponible qu'à partir de "
                            + formatAmount(paymentProperties.getInstallmentThreshold()) + " FCFA.");
        }
    }

    private void validateBookingAccess(Booking booking, User user) {
        boolean isStudent = booking.getStudent().getId().equals(user.getId());
        boolean isLandlord = booking.getListing().getLandlord().getId().equals(user.getId());
        if (!isStudent && !isLandlord) {
            throw new ForbiddenException("Vous n'avez pas accès à cette réservation.");
        }
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%,.0f", amount);
    }
}
