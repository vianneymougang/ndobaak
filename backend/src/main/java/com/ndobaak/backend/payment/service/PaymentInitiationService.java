package com.ndobaak.backend.payment.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.InstallmentStatus;
import com.ndobaak.backend.common.enums.PaymentChannel;
import com.ndobaak.backend.common.enums.PaymentMethod;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.PaymentType;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.payment.dto.InitiatePaymentRequest;
import com.ndobaak.backend.payment.dto.PayInstallmentRequest;
import com.ndobaak.backend.payment.dto.PaymentInitiationResponse;
import com.ndobaak.backend.payment.dto.PaymentResponse;
import com.ndobaak.backend.payment.entity.Installment;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.gateway.PaymentGateway;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRequest;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRouter;
import com.ndobaak.backend.payment.gateway.PaymentInitiationResult;
import com.ndobaak.backend.payment.gateway.PaymentProperties;
import com.ndobaak.backend.payment.mapper.PaymentMapper;
import com.ndobaak.backend.payment.repository.InstallmentRepository;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import com.ndobaak.backend.payment.repository.PaymentScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentInitiationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentInitiationService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentScheduleRepository scheduleRepository;
    private final InstallmentRepository installmentRepository;
    private final BookingRepository bookingRepository;
    private final AuthService authService;
    private final PaymentGatewayRouter gatewayRouter;
    private final PaymentMapper paymentMapper;
    private final PaymentProperties paymentProperties;
    private final PaymentScheduleService scheduleService;

    public PaymentInitiationService(PaymentRepository paymentRepository,
                                    PaymentScheduleRepository scheduleRepository,
                                    InstallmentRepository installmentRepository,
                                    BookingRepository bookingRepository,
                                    AuthService authService,
                                    PaymentGatewayRouter gatewayRouter,
                                    PaymentMapper paymentMapper,
                                    PaymentProperties paymentProperties,
                                    PaymentScheduleService scheduleService) {
        this.paymentRepository = paymentRepository;
        this.scheduleRepository = scheduleRepository;
        this.installmentRepository = installmentRepository;
        this.bookingRepository = bookingRepository;
        this.authService = authService;
        this.gatewayRouter = gatewayRouter;
        this.paymentMapper = paymentMapper;
        this.paymentProperties = paymentProperties;
        this.scheduleService = scheduleService;
    }

    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request) {
        User student = authService.getAuthenticatedUser();
        Booking booking = findBookingById(request.getBookingId());

        validateBookingOwnership(booking, student);
        validateBookingConfirmed(booking);
        validateNoActivePayment(booking);
        scheduleService.cleanupFailedScheduleIfPresent(booking);

        BigDecimal totalAmount = booking.getTotalAmount();
        BigDecimal threshold = paymentProperties.getInstallmentThreshold();
        boolean isSplit = totalAmount.compareTo(threshold) > 0;

        PaymentType paymentType = isSplit ? PaymentType.INSTALLMENT : PaymentType.INTEGRAL;
        BigDecimal firstBlockAmount = isSplit ? threshold : totalAmount;

        BigDecimal commission = firstBlockAmount
                .multiply(paymentProperties.getCommissionRate())
                .setScale(0, RoundingMode.CEILING);

        String reference = UUID.randomUUID().toString();
        String payerName = student.getFirstName() + " " + student.getLastName();

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(firstBlockAmount)
                .commission(commission)
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .channel(request.getOperator())
                .paymentType(paymentType)
                .transactionId(reference)
                .status(PaymentStatus.PENDING)
                .phoneNumber(request.getPhoneNumber())
                .payerName(payerName)
                .build();

        payment = paymentRepository.save(payment);

        if (isSplit) {
            scheduleService.createBlockSchedule(booking, totalAmount, threshold, payment);
        }

        PaymentGateway gateway = gatewayRouter.resolveGateway(request.getOperator());
        BigDecimal totalCharged = firstBlockAmount;
        String description = isSplit
                ? "Bloc 1 - " + booking.getListing().getTitle()
                : "Paiement Ndobaak - " + booking.getListing().getTitle();

        PaymentGatewayRequest gatewayRequest = new PaymentGatewayRequest(
                reference,
                totalCharged,
                "XAF",
                payerName,
                student.getEmail(),
                request.getPhoneNumber(),
                request.getOperator(),
                description,
                paymentProperties.getFrontendCallbackUrl()
        );

        PaymentInitiationResult result = gateway.initiatePayment(gatewayRequest);

        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setProviderName(gateway.getProviderName());
        payment.setProviderReference(result.providerReference());
        paymentRepository.save(payment);

        try {
            gateway.triggerCharge(result.providerReference(), gatewayRequest);
        } catch (Exception e) {
            log.warn("USSD push failed (non-fatal): reference={}, error={}", reference, e.getMessage());
        }

        log.info("Payment initiated: reference={}, provider={}, amount={} XAF, split={}",
                reference, gateway.getProviderName(), totalCharged, isSplit);

        return paymentMapper.toResponse(payment);
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
                .channel(request.getOperator())
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
    public PaymentInitiationResponse initiateBookingPayment(Long bookingId, Long studentId) {
        Booking booking = findBookingById(bookingId);

        if (!booking.getStudent().getId().equals(studentId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à initier le paiement pour cette réservation.");
        }

        if (booking.getStatus() != BookingStatus.LEASE_SIGNED) {
            throw new BusinessException(
                "Le paiement n'est possible qu'après signature du contrat par les deux parties.");
        }

        boolean hasActivePayment = paymentRepository.findByBookingId(bookingId).stream()
                .anyMatch(p -> p.getStatus() == PaymentStatus.PENDING
                        || p.getStatus() == PaymentStatus.PROCESSING);
        if (hasActivePayment) {
            throw new BusinessException("Un paiement est déjà en cours pour cette réservation.");
        }

        String reference = "NDK-" + bookingId + "-" + System.currentTimeMillis();
        String payerName = booking.getStudent().getFirstName() + " " + booking.getStudent().getLastName();
        String description = "Réservation Ndobaâk #" + bookingId;

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .commission(BigDecimal.ZERO)
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .paymentType(PaymentType.INTEGRAL)
                .transactionId(reference)
                .status(PaymentStatus.PENDING)
                .payerName(payerName)
                .build();
        payment = paymentRepository.save(payment);

        PaymentGateway gateway = gatewayRouter.resolveGatewayByProvider(
                paymentProperties.getPrimaryGateway());

        PaymentInitiationResult result = gateway.initiateRedirectPayment(
                reference,
                booking.getTotalAmount(),
                "XAF",
                payerName,
                booking.getStudent().getEmail(),
                description,
                paymentProperties.getFrontendCallbackUrl()
        );

        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setProviderName(gateway.getProviderName());
        payment.setProviderReference(result.providerReference());
        paymentRepository.save(payment);

        log.info("Rental cycle payment initiated: bookingId={}, reference={}, provider={}",
                bookingId, reference, gateway.getProviderName());

        return new PaymentInitiationResponse(result.authorizationUrl(), reference);
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

        boolean hasCompleted = existingPayments.stream()
                .anyMatch(p -> p.getStatus() == PaymentStatus.COMPLETED);
        if (hasCompleted) {
            boolean allBlocksCompleted = scheduleRepository.findByBookingId(booking.getId())
                    .map(s -> s.getInstallments().stream()
                            .allMatch(i -> i.getStatus() == InstallmentStatus.PAID))
                    .orElse(true);
            if (allBlocksCompleted) {
                throw new BusinessException("Cette réservation a déjà été payée.");
            }
        }
    }
}
