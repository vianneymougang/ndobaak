package com.ndobaak.backend.payment.service;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.payment.dto.PaymentResponse;
import com.ndobaak.backend.payment.dto.ReceiptResponse;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.mapper.PaymentMapper;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final AuthService authService;
    private final PaymentMapper paymentMapper;

    public PaymentQueryService(PaymentRepository paymentRepository,
                               BookingRepository bookingRepository,
                               AuthService authService,
                               PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.authService = authService;
        this.paymentMapper = paymentMapper;
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

    Booking findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
    }

    void validateBookingAccess(Booking booking, User user) {
        boolean isStudent = booking.getStudent().getId().equals(user.getId());
        boolean isLandlord = booking.getListing().getLandlord().getId().equals(user.getId());
        if (!isStudent && !isLandlord) {
            throw new ForbiddenException("Vous n'avez pas accès à cette réservation.");
        }
    }
}
