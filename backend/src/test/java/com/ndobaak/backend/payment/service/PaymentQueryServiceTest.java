package com.ndobaak.backend.payment.service;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.payment.dto.PaymentResponse;
import com.ndobaak.backend.payment.dto.PaymentScheduleResponse;
import com.ndobaak.backend.payment.dto.ReceiptResponse;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.mapper.PaymentMapper;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentQueryServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private AuthService authService;
    @Mock private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentQueryService service;

    private Student student;
    private Landlord landlord;
    private Booking booking;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);

        landlord = new Landlord();
        landlord.setId(10L);

        Listing listing = Listing.builder()
                .id(100L).title("Studio Eyang")
                .monthlyPrice(new BigDecimal("50000"))
                .landlord(landlord).build();

        booking = Booking.builder()
                .student(student).listing(listing)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 10, 1))
                .totalAmount(new BigDecimal("300000"))
                .status(BookingStatus.CONFIRMED).build();
        booking.setId(1L);
    }

    @Test
    void getPaymentsByBookingShouldReturnListForStudent() {
        Payment payment = Payment.builder().booking(booking).build();
        payment.setId(1L);
        PaymentResponse dummyResponse = PaymentResponse.builder().id(1L).build();

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(dummyResponse);

        List<PaymentResponse> result = service.getPaymentsByBooking(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getPaymentsByBookingShouldThrowWhenUnauthorized() {
        Student other = new Student();
        other.setId(99L);

        when(authService.getAuthenticatedUser()).thenReturn(other);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.getPaymentsByBooking(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getTransactionHistoryAsStudentShouldQueryByStudentId() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(), pageable, 0);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(paymentRepository.findByBookingStudentId(1L, pageable)).thenReturn(page);

        service.getTransactionHistory(pageable);

        verify(paymentRepository).findByBookingStudentId(1L, pageable);
    }

    @Test
    void getTransactionHistoryAsLandlordShouldQueryByLandlordId() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(), pageable, 0);

        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(paymentRepository.findByBookingListingLandlordId(10L, pageable)).thenReturn(page);

        service.getTransactionHistory(pageable);

        verify(paymentRepository).findByBookingListingLandlordId(10L, pageable);
    }

    @Test
    void getReceiptShouldReturnReceiptForCompletedPayment() {
        Payment payment = Payment.builder()
                .booking(booking).status(PaymentStatus.COMPLETED).build();
        payment.setId(1L);
        ReceiptResponse receipt = ReceiptResponse.builder().transactionId("ref").build();

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentMapper.toReceiptResponse(payment)).thenReturn(receipt);

        ReceiptResponse result = service.getReceipt(1L);

        assertThat(result.getTransactionId()).isEqualTo("ref");
    }

    @Test
    void getReceiptShouldThrowWhenPaymentNotCompleted() {
        Payment payment = Payment.builder()
                .booking(booking).status(PaymentStatus.PROCESSING).build();
        payment.setId(1L);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.getReceipt(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("complétés");
    }
}
