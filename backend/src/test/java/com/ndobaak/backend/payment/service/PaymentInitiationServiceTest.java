package com.ndobaak.backend.payment.service;

import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.PaymentChannel;
import com.ndobaak.backend.common.enums.InstallmentStatus;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.PaymentType;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.payment.dto.InitiatePaymentRequest;
import com.ndobaak.backend.payment.dto.PayInstallmentRequest;
import com.ndobaak.backend.payment.dto.PaymentResponse;
import com.ndobaak.backend.payment.entity.Installment;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.entity.PaymentSchedule;
import com.ndobaak.backend.payment.gateway.PaymentGateway;
import com.ndobaak.backend.payment.gateway.PaymentGatewayRouter;
import com.ndobaak.backend.payment.gateway.PaymentInitiationResult;
import com.ndobaak.backend.payment.gateway.PaymentProperties;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentInitiationServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentScheduleRepository scheduleRepository;
    @Mock private InstallmentRepository installmentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private AuthService authService;
    @Mock private PaymentGatewayRouter gatewayRouter;
    @Mock private PaymentMapper paymentMapper;
    @Mock private PaymentProperties paymentProperties;
    @Mock private PaymentScheduleService scheduleService;
    @Mock private PaymentGateway gateway;

    @InjectMocks
    private PaymentInitiationService service;

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
                .build();

        booking = Booking.builder()
                .student(student).listing(listing)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 10, 1))
                .monthlyRent(new BigDecimal("50000"))
                .totalAmount(new BigDecimal("300000"))
                .status(BookingStatus.CONFIRMED)
                .build();
        booking.setId(1L);
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
        when(paymentRepository.existsByBookingIdAndStatus(eq(1L), any(PaymentStatus.class))).thenReturn(false);
        when(paymentProperties.getInstallmentThreshold()).thenReturn(new BigDecimal("500000"));
        when(paymentProperties.getCommissionRate()).thenReturn(new BigDecimal("0.05"));
        when(paymentProperties.getFrontendCallbackUrl()).thenReturn("http://localhost:4200/callback");
        stubGateway();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        PaymentResponse dummyResponse = PaymentResponse.builder().id(1L)
                .amount(new BigDecimal("300000")).status(PaymentStatus.PROCESSING).build();
        when(paymentMapper.toResponse(any())).thenReturn(dummyResponse);

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setBookingId(1L);
        request.setOperator(PaymentChannel.MTN_MOMO);
        request.setPhoneNumber("+237670000000");

        PaymentResponse response = service.initiatePayment(request);

        assertThat(response).isNotNull();
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(gateway).initiatePayment(any());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());
        Payment finalState = captor.getAllValues().get(1);
        assertThat(finalState.getAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(finalState.getPaymentType()).isEqualTo(PaymentType.INTEGRAL);
        assertThat(finalState.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    }

    @Test
    void initiateSplitPaymentShouldDelegateScheduleCreation() {
        booking.setTotalAmount(new BigDecimal("600000"));
        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.existsByBookingIdAndStatus(eq(1L), any(PaymentStatus.class))).thenReturn(false);
        when(paymentProperties.getInstallmentThreshold()).thenReturn(new BigDecimal("500000"));
        when(paymentProperties.getCommissionRate()).thenReturn(new BigDecimal("0.05"));
        when(paymentProperties.getFrontendCallbackUrl()).thenReturn("http://localhost:4200/callback");
        stubGateway();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        PaymentResponse dummyResponse = PaymentResponse.builder().id(1L).build();
        when(paymentMapper.toResponse(any())).thenReturn(dummyResponse);

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setBookingId(1L);
        request.setOperator(PaymentChannel.ORANGE_MONEY);
        request.setPhoneNumber("+237690000000");

        service.initiatePayment(request);

        verify(scheduleService).createBlockSchedule(any(), any(), any(), any());
    }

    @Test
    void initiatePaymentShouldThrowWhenNotOwner() {
        Student other = new Student();
        other.setId(99L);
        when(authService.getAuthenticatedUser()).thenReturn(other);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setBookingId(1L);

        assertThatThrownBy(() -> service.initiatePayment(request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void initiatePaymentShouldThrowWhenNotConfirmed() {
        booking.setStatus(BookingStatus.PENDING);
        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setBookingId(1L);

        assertThatThrownBy(() -> service.initiatePayment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("confirmée");
    }

    @Test
    void initiatePaymentShouldThrowWhenProcessingPaymentExists() {
        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.existsByBookingIdAndStatus(1L, PaymentStatus.PROCESSING)).thenReturn(true);

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setBookingId(1L);

        assertThatThrownBy(() -> service.initiatePayment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("en cours");
    }

    @Test
    void initiatePaymentShouldThrowWhenAlreadyPaid() {
        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.existsByBookingIdAndStatus(1L, PaymentStatus.PROCESSING)).thenReturn(false);
        when(paymentRepository.existsByBookingIdAndStatus(1L, PaymentStatus.COMPLETED)).thenReturn(true);
        when(scheduleRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setBookingId(1L);

        assertThatThrownBy(() -> service.initiatePayment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("payée");
    }

    @Test
    void initiatePaymentShouldThrowWhenBookingNotFound() {
        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setBookingId(999L);

        assertThatThrownBy(() -> service.initiatePayment(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void payInstallmentShouldCreatePaymentForPendingInstallment() {
        PaymentSchedule ps = PaymentSchedule.builder().booking(booking).build();
        Installment installment = Installment.builder()
                .installmentNumber(2).amount(new BigDecimal("100000"))
                .dueDate(LocalDate.of(2026, 5, 1))
                .status(InstallmentStatus.PENDING).paymentSchedule(ps).build();
        installment.setId(1L);

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
        PaymentResponse dummyResponse = PaymentResponse.builder().id(2L).build();
        when(paymentMapper.toResponse(any())).thenReturn(dummyResponse);

        PayInstallmentRequest request = new PayInstallmentRequest();
        request.setOperator(PaymentChannel.MTN_MOMO);
        request.setPhoneNumber("+237670000000");

        service.payInstallment(1L, request);

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
        request.setOperator(PaymentChannel.MTN_MOMO);
        request.setPhoneNumber("+237670000000");

        assertThatThrownBy(() -> service.payInstallment(1L, request))
                .isInstanceOf(BusinessException.class);
    }
}
