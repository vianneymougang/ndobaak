package com.ndobaak.backend.payment.service;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.InstallmentStatus;
import com.ndobaak.backend.common.enums.MobileMoneyOperator;
import com.ndobaak.backend.common.enums.PaymentMethod;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.PaymentType;
import com.ndobaak.backend.common.enums.ScheduleFrequency;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.notification.service.NotificationDispatcher;
import com.ndobaak.backend.payment.dto.InitiatePaymentRequest;
import com.ndobaak.backend.payment.dto.PayInstallmentRequest;
import com.ndobaak.backend.payment.dto.PaymentResponse;
import com.ndobaak.backend.payment.dto.PaymentScheduleResponse;
import com.ndobaak.backend.payment.dto.ReceiptResponse;
import com.ndobaak.backend.payment.entity.Installment;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.entity.PaymentSchedule;
import com.ndobaak.backend.payment.gateway.PaymentGateway;
import com.ndobaak.backend.subscription.service.SubscriptionService;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRouter;
import com.ndobaak.backend.payment.gateway.PaymentInitiationResult;
import com.ndobaak.backend.payment.gateway.PaymentProperties;
import com.ndobaak.backend.payment.gateway.WebhookEvent;
import com.ndobaak.backend.payment.mapper.PaymentMapper;
import com.ndobaak.backend.payment.repository.InstallmentRepository;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import com.ndobaak.backend.payment.repository.PaymentScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentScheduleRepository scheduleRepository;
    @Mock
    private InstallmentRepository installmentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private AuthService authService;
    @Mock
    private NotificationDispatcher notificationDispatcher;
    @Mock
    private PaymentGatewayRouter gatewayRouter;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private PaymentProperties paymentProperties;
    @Mock
    private PaymentGateway gateway;
    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private PaymentService paymentService;

    private Student student;
    private Landlord landlord;
    private Listing listing;
    private Booking booking;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setEmail("john@test.com");

        landlord = new Landlord();
        landlord.setId(10L);
        landlord.setFirstName("Jane");
        landlord.setLastName("Smith");

        listing = Listing.builder()
                .id(100L)
                .title("Studio Eyang")
                .monthlyPrice(new BigDecimal("50000"))
                .landlord(landlord)
                .build();

        booking = Booking.builder()
                .student(student)
                .listing(listing)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 10, 1))
                .monthlyRent(new BigDecimal("50000"))
                .totalAmount(new BigDecimal("300000"))
                .status(BookingStatus.CONFIRMED)
                .build();
        booking.setId(1L);
    }

    private InitiatePaymentRequest buildIntegralRequest() {
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setBookingId(1L);
        request.setOperator(MobileMoneyOperator.MTN_MOMO);
        request.setPhoneNumber("+237670000000");
        return request;
    }

    private InitiatePaymentRequest buildSplitRequest() {
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setBookingId(1L);
        request.setOperator(MobileMoneyOperator.ORANGE_MONEY);
        request.setPhoneNumber("+237690000000");
        return request;
    }

    private PaymentResponse buildDummyResponse() {
        return PaymentResponse.builder()
                .id(1L)
                .amount(new BigDecimal("300000"))
                .status(PaymentStatus.PROCESSING)
                .build();
    }

    private void stubGateway() {
        when(gatewayRouter.resolveGateway(any())).thenReturn(gateway);
        when(gateway.getProviderName()).thenReturn("mock");
        when(gateway.initiatePayment(any())).thenReturn(
                new PaymentInitiationResult("prov-ref", "https://pay.example.com", "pending"));
    }

    @Test
    void initiateIntegralPaymentShouldCreatePaymentAndCallGateway() {
        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Collections.emptyList());
        when(scheduleRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(paymentProperties.getInstallmentThreshold()).thenReturn(new BigDecimal("500000"));
        when(paymentProperties.getCommissionRate()).thenReturn(new BigDecimal("0.05"));
        when(paymentProperties.getFrontendCallbackUrl()).thenReturn("http://localhost:4200/callback");
        stubGateway();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(paymentMapper.toResponse(any())).thenReturn(buildDummyResponse());

        PaymentResponse response = paymentService.initiatePayment(buildIntegralRequest());

        assertThat(response).isNotNull();
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(gateway).initiatePayment(any());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());
        Payment finalState = captor.getAllValues().get(1);
        assertThat(finalState.getAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(finalState.getCommission()).isEqualByComparingTo(new BigDecimal("15000"));
        assertThat(finalState.getPaymentType()).isEqualTo(PaymentType.INTEGRAL);
        assertThat(finalState.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(finalState.getProviderName()).isEqualTo("mock");
    }

    @Test
    void initiateBlockPaymentShouldCreateScheduleWithTwoBlocks() {
        booking.setTotalAmount(new BigDecimal("600000"));
        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Collections.emptyList());
        when(scheduleRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(paymentProperties.getInstallmentThreshold()).thenReturn(new BigDecimal("500000"));
        when(paymentProperties.getCommissionRate()).thenReturn(new BigDecimal("0.05"));
        when(paymentProperties.getFrontendCallbackUrl()).thenReturn("http://localhost:4200/callback");
        stubGateway();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(scheduleRepository.save(any(PaymentSchedule.class))).thenAnswer(inv -> {
            PaymentSchedule s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(installmentRepository.save(any(Installment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any())).thenReturn(buildDummyResponse());

        paymentService.initiatePayment(buildSplitRequest());

        verify(scheduleRepository, times(2)).save(any(PaymentSchedule.class));
        verify(installmentRepository, times(2)).save(any(Installment.class));
    }

    @Test
    void initiatePaymentShouldThrowWhenNotBookingOwner() {
        Student otherStudent = new Student();
        otherStudent.setId(99L);

        when(authService.getAuthenticatedUser()).thenReturn(otherStudent);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.initiatePayment(buildIntegralRequest()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void initiatePaymentShouldThrowWhenBookingNotConfirmed() {
        booking.setStatus(BookingStatus.PENDING);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.initiatePayment(buildIntegralRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("confirmée");
    }

    @Test
    void initiatePaymentShouldThrowWhenProcessingPaymentExists() {
        Payment existing = Payment.builder()
                .status(PaymentStatus.PROCESSING)
                .paymentType(PaymentType.INTEGRAL)
                .build();

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> paymentService.initiatePayment(buildIntegralRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("en cours");
    }

    @Test
    void initiatePaymentShouldThrowWhenAlreadyPaid() {
        Payment existing = Payment.builder()
                .status(PaymentStatus.COMPLETED)
                .paymentType(PaymentType.INTEGRAL)
                .build();

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(List.of(existing));
        when(scheduleRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.initiatePayment(buildIntegralRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("payée");
    }

    @Test
    void initiatePaymentShouldThrowWhenBookingNotFound() {
        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        InitiatePaymentRequest request = buildIntegralRequest();
        request.setBookingId(999L);

        assertThatThrownBy(() -> paymentService.initiatePayment(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void payInstallmentShouldCreatePaymentForPendingInstallment() {
        PaymentSchedule ps = PaymentSchedule.builder().booking(booking).build();
        Installment installment = Installment.builder()
                .installmentNumber(2)
                .amount(new BigDecimal("100000"))
                .dueDate(LocalDate.of(2026, 5, 1))
                .status(InstallmentStatus.PENDING)
                .paymentSchedule(ps)
                .build();
        installment.setId(1L);

        PayInstallmentRequest request = new PayInstallmentRequest();
        request.setOperator(MobileMoneyOperator.MTN_MOMO);
        request.setPhoneNumber("+237670000000");

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(installmentRepository.findById(1L)).thenReturn(Optional.of(installment));
        when(paymentProperties.getCommissionRate()).thenReturn(new BigDecimal("0.05"));
        when(paymentProperties.getFrontendCallbackUrl()).thenReturn("http://localhost:4200/callback");
        stubGateway();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });
        when(paymentMapper.toResponse(any())).thenReturn(buildDummyResponse());

        paymentService.payInstallment(1L, request);

        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(installmentRepository).save(installment);
    }

    @Test
    void payInstallmentShouldThrowWhenAlreadyPaid() {
        Installment installment = Installment.builder()
                .status(InstallmentStatus.PAID)
                .paymentSchedule(PaymentSchedule.builder().booking(booking).build())
                .build();
        installment.setId(1L);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(installmentRepository.findById(1L)).thenReturn(Optional.of(installment));

        PayInstallmentRequest request = new PayInstallmentRequest();
        request.setOperator(MobileMoneyOperator.MTN_MOMO);
        request.setPhoneNumber("+237670000000");

        assertThatThrownBy(() -> paymentService.payInstallment(1L, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void handleWebhookCompleteShouldUpdateStatusAndNotify() {
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(new BigDecimal("300000"))
                .transactionId("ref-123")
                .status(PaymentStatus.PROCESSING)
                .payerName("John Doe")
                .build();
        payment.setId(1L);

        WebhookEvent event = new WebhookEvent(
                "payment.complete", "ref-123", "complete",
                new BigDecimal("315000"), "XAF", Map.of());

        when(gatewayRouter.resolveGatewayByProvider("mock")).thenReturn(gateway);
        when(gateway.verifyWebhookSignature("payload", "sig")).thenReturn(true);
        when(gateway.parseWebhookPayload("payload")).thenReturn(event);
        when(paymentRepository.findByProviderReference("ref-123")).thenReturn(Optional.of(payment));
        when(scheduleRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        paymentService.handleWebhook("mock", "payload", "sig");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getPaidAt()).isNotNull();
        verify(paymentRepository).save(payment);
        verify(notificationDispatcher, times(2)).dispatch(
                anyLong(), any(), anyString(), anyString(), anyLong(), eq("PAYMENT"), anyMap(), anyMap());
    }

    @Test
    void handleWebhookFailedShouldUpdateStatusAndNotifyStudent() {
        Payment payment = Payment.builder()
                .booking(booking)
                .transactionId("ref-456")
                .status(PaymentStatus.PROCESSING)
                .build();
        payment.setId(1L);

        WebhookEvent event = new WebhookEvent(
                "payment.failed", "ref-456", "failed",
                new BigDecimal("300000"), "XAF", Map.of());

        when(gatewayRouter.resolveGatewayByProvider("mock")).thenReturn(gateway);
        when(gateway.verifyWebhookSignature("payload", "sig")).thenReturn(true);
        when(gateway.parseWebhookPayload("payload")).thenReturn(event);
        when(paymentRepository.findByProviderReference("ref-456")).thenReturn(Optional.of(payment));

        paymentService.handleWebhook("mock", "payload", "sig");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(notificationDispatcher).dispatch(
                eq(1L), any(), anyString(), anyString(), anyLong(), eq("PAYMENT"), anyMap(), anyMap());
    }

    @Test
    void handleWebhookShouldThrowOnInvalidSignature() {
        when(gatewayRouter.resolveGatewayByProvider("mock")).thenReturn(gateway);
        when(gateway.verifyWebhookSignature("payload", "bad-sig")).thenReturn(false);

        assertThatThrownBy(() -> paymentService.handleWebhook("mock", "payload", "bad-sig"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Signature");
    }

    @Test
    void handleWebhookShouldSkipAlreadyCompletedPayment() {
        Payment payment = Payment.builder()
                .booking(booking)
                .transactionId("ref-789")
                .status(PaymentStatus.COMPLETED)
                .build();
        payment.setId(1L);

        WebhookEvent event = new WebhookEvent(
                "payment.complete", "ref-789", "complete",
                new BigDecimal("300000"), "XAF", Map.of());

        when(gatewayRouter.resolveGatewayByProvider("mock")).thenReturn(gateway);
        when(gateway.verifyWebhookSignature("payload", "sig")).thenReturn(true);
        when(gateway.parseWebhookPayload("payload")).thenReturn(event);
        when(paymentRepository.findByProviderReference("ref-789")).thenReturn(Optional.of(payment));

        paymentService.handleWebhook("mock", "payload", "sig");

        verify(paymentRepository, never()).save(any());
        verify(notificationDispatcher, never()).dispatch(
                anyLong(), any(), anyString(), anyString(), anyLong(), anyString(), anyMap(), anyMap());
    }

    @Test
    void getPaymentsByBookingShouldReturnListForAuthorizedUser() {
        Payment payment = Payment.builder().booking(booking).build();
        payment.setId(1L);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(buildDummyResponse());

        List<PaymentResponse> result = paymentService.getPaymentsByBooking(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getPaymentsByBookingShouldThrowWhenUnauthorized() {
        Student otherStudent = new Student();
        otherStudent.setId(99L);

        when(authService.getAuthenticatedUser()).thenReturn(otherStudent);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.getPaymentsByBooking(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getTransactionHistoryAsStudentShouldQueryByStudentId() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(), pageable, 0);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(paymentRepository.findByBookingStudentId(1L, pageable)).thenReturn(page);

        paymentService.getTransactionHistory(pageable);

        verify(paymentRepository).findByBookingStudentId(1L, pageable);
    }

    @Test
    void getTransactionHistoryAsLandlordShouldQueryByLandlordId() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(), pageable, 0);

        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(paymentRepository.findByBookingListingLandlordId(10L, pageable)).thenReturn(page);

        paymentService.getTransactionHistory(pageable);

        verify(paymentRepository).findByBookingListingLandlordId(10L, pageable);
    }

    @Test
    void getReceiptShouldReturnReceiptForCompletedPayment() {
        Payment payment = Payment.builder()
                .booking(booking)
                .status(PaymentStatus.COMPLETED)
                .build();
        payment.setId(1L);
        ReceiptResponse receipt = ReceiptResponse.builder().transactionId("ref").build();

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentMapper.toReceiptResponse(payment)).thenReturn(receipt);

        ReceiptResponse result = paymentService.getReceipt(1L);

        assertThat(result.getTransactionId()).isEqualTo("ref");
    }

    @Test
    void getReceiptShouldThrowWhenPaymentNotCompleted() {
        Payment payment = Payment.builder()
                .booking(booking)
                .status(PaymentStatus.PROCESSING)
                .build();
        payment.setId(1L);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getReceipt(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("complétés");
    }

    @Test
    void getPaymentScheduleShouldReturnSchedule() {
        PaymentSchedule schedule = PaymentSchedule.builder()
                .booking(booking)
                .totalAmount(new BigDecimal("300000"))
                .frequency(ScheduleFrequency.MONTHLY)
                .numberOfInstallments(6)
                .build();
        schedule.setId(1L);
        PaymentScheduleResponse scheduleResponse = PaymentScheduleResponse.builder()
                .id(1L)
                .totalAmount(new BigDecimal("300000"))
                .build();

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(scheduleRepository.findByBookingId(1L)).thenReturn(Optional.of(schedule));
        when(paymentMapper.toScheduleResponse(schedule)).thenReturn(scheduleResponse);

        PaymentScheduleResponse result = paymentService.getPaymentSchedule(1L);

        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
    }

    @Test
    void getPaymentScheduleShouldThrowWhenNoSchedule() {
        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(scheduleRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentSchedule(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
