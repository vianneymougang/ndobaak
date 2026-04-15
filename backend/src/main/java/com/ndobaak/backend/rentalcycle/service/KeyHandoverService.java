package com.ndobaak.backend.rentalcycle.service;

import com.ndobaak.backend.auth.entity.Admin;
import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.InspectionStatus;
import com.ndobaak.backend.common.enums.InspectionType;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.notification.service.NotificationDispatcher;
import com.ndobaak.backend.payment.gateway.notchpay.NotchPayGateway;
import com.ndobaak.backend.payment.gateway.notchpay.TransferRequest;
import com.ndobaak.backend.payment.gateway.notchpay.TransferResponse;
import com.ndobaak.backend.rentalcycle.dto.request.KeyHandoverRequest;
import com.ndobaak.backend.rentalcycle.dto.response.KeyHandoverResponse;
import com.ndobaak.backend.rentalcycle.entity.KeyHandover;
import com.ndobaak.backend.rentalcycle.entity.PropertyInspection;
import com.ndobaak.backend.rentalcycle.mapper.RentalCycleMapper;
import com.ndobaak.backend.rentalcycle.repository.KeyHandoverRepository;
import com.ndobaak.backend.rentalcycle.repository.PropertyInspectionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KeyHandoverService {

    private static final Logger log = LoggerFactory.getLogger(KeyHandoverService.class);
    private static final BigDecimal AGENCY_FEE_RATE = new BigDecimal("0.05");

    private final KeyHandoverRepository keyHandoverRepository;
    private final PropertyInspectionRepository inspectionRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotchPayGateway notchPayGateway;
    private final NotificationDispatcher notificationDispatcher;
    private final RentalCycleMapper mapper;

    @Transactional
    public KeyHandoverResponse createHandover(Long bookingId, KeyHandoverRequest request, Long adminId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable."));

        if (keyHandoverRepository.existsByBookingId(bookingId)) {
            throw new BusinessException("Une remise des clés existe déjà pour cette réservation.");
        }

        PropertyInspection moveInInspection = inspectionRepository
                .findByBookingIdAndTypeAndStatus(bookingId, InspectionType.MOVE_IN, InspectionStatus.FULLY_SIGNED)
                .orElseThrow(() -> new BusinessException(
                        "Un état des lieux d'entrée signé par les deux parties est requis avant la remise des clés."));

        KeyHandover handover = new KeyHandover();
        handover.setBooking(booking);
        handover.setMoveInInspection(moveInInspection);
        handover.setHandoverDate(request.handoverDate());
        handover.setLandlordConfirmed(false);
        handover.setTenantConfirmed(false);
        handover.setTransferInitiated(false);
        handover.setObservations(request.observations());

        return mapper.toResponse(keyHandoverRepository.save(handover));
    }

    @Transactional
    public KeyHandoverResponse confirmByLandlord(Long bookingId, Long landlordId) {
        KeyHandover handover = findHandoverByBooking(bookingId);
        Booking booking = handover.getBooking();

        if (!booking.getListing().getLandlord().getId().equals(landlordId)) {
            throw new AccessDeniedException("Vous ne pouvez confirmer que les remises des clés de vos logements.");
        }

        if (handover.isLandlordConfirmed()) {
            throw new BusinessException("Vous avez déjà confirmé la remise des clés.");
        }

        handover.setLandlordConfirmed(true);
        handover.setLandlordConfirmedAt(LocalDateTime.now());

        if (handover.isTenantConfirmed()) {
            finalizeHandover(handover, booking);
        }

        return mapper.toResponse(keyHandoverRepository.save(handover));
    }

    @Transactional
    public KeyHandoverResponse confirmByTenant(Long bookingId, Long studentId) {
        KeyHandover handover = findHandoverByBooking(bookingId);
        Booking booking = handover.getBooking();

        if (!booking.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("Vous ne pouvez confirmer que vos propres remises des clés.");
        }

        if (handover.isTenantConfirmed()) {
            throw new BusinessException("Vous avez déjà confirmé la réception des clés.");
        }

        handover.setTenantConfirmed(true);
        handover.setTenantConfirmedAt(LocalDateTime.now());

        if (handover.isLandlordConfirmed()) {
            finalizeHandover(handover, booking);
        }

        return mapper.toResponse(keyHandoverRepository.save(handover));
    }

    private void finalizeHandover(KeyHandover handover, Booking booking) {
        Long bookingId = booking.getId();

        booking.setStatus(BookingStatus.KEYS_HANDED_OVER);
        bookingRepository.save(booking);

        notificationDispatcher.dispatch(
                booking.getListing().getLandlord().getId(),
                NotificationType.KEYS_HANDED_OVER,
                "Remise des clés confirmée",
                "Les deux parties ont confirmé la remise des clés pour la réservation #" + bookingId + ".",
                bookingId,
                "BOOKING",
                Map.of(
                        "bookingId", bookingId.toString(),
                        "tenantName", booking.getStudent().getFirstName() + " " + booking.getStudent().getLastName()
                ),
                Map.of("bookingId", bookingId.toString())
        );

        notificationDispatcher.dispatch(
                booking.getStudent().getId(),
                NotificationType.KEYS_HANDED_OVER,
                "Remise des clés confirmée",
                "La remise des clés a été confirmée. Bienvenue dans votre nouveau logement !",
                bookingId,
                "BOOKING",
                Map.of("bookingId", bookingId.toString()),
                Map.of("bookingId", bookingId.toString())
        );

        initiateTransferAndFinalize(handover, booking);
    }

    @Transactional
    public KeyHandoverResponse retryTransfer(Long bookingId, Long adminId) {
        KeyHandover handover = findHandoverByBooking(bookingId);

        if (handover.isTransferInitiated()) {
            throw new BusinessException("Le virement a déjà été initié avec succès.");
        }

        if (!handover.isLandlordConfirmed() || !handover.isTenantConfirmed()) {
            throw new BusinessException("Les deux parties doivent confirmer la remise des clés avant le virement.");
        }

        Booking booking = handover.getBooking();
        initiateTransferAndFinalize(handover, booking);

        return mapper.toResponse(keyHandoverRepository.save(handover));
    }

    @Transactional(readOnly = true)
    public KeyHandoverResponse getHandover(Long bookingId) {
        return mapper.toResponse(findHandoverByBooking(bookingId));
    }

    private void initiateTransferAndFinalize(KeyHandover handover, Booking booking) {
        Landlord landlord = (Landlord) booking.getListing().getLandlord();

        if (landlord.getMobileMoneyPhone() == null || landlord.getMobileMoneyPhone().isBlank()) {
            log.error("Landlord {} has no mobile money phone configured — transfer aborted for booking {}",
                    landlord.getId(), booking.getId());
            notifyAdminTransferFailed(booking, "Numéro Mobile Money du propriétaire non configuré.");
            return;
        }

        BigDecimal transferAmount = booking.getTotalAmount()
                .multiply(BigDecimal.ONE.subtract(AGENCY_FEE_RATE))
                .setScale(0, RoundingMode.FLOOR);

        TransferRequest transferRequest = new TransferRequest(
                transferAmount,
                "XAF",
                landlord.getFirstName() + " " + landlord.getLastName(),
                landlord.getMobileMoneyPhone(),
                "Loyer Ndobaâk — réservation #" + booking.getId()
        );

        try {
            TransferResponse response = notchPayGateway.initiateTransfer(transferRequest);
            handover.setTransferInitiated(true);
            handover.setTransferDate(LocalDateTime.now());
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);
            log.info("Transfer initiated for booking {}: reference={}", booking.getId(), response.reference());
        } catch (Exception e) {
            log.error("Transfer failed for booking {}: {}", booking.getId(), e.getMessage(), e);
            notifyAdminTransferFailed(booking, e.getMessage());
        }
    }

    private void notifyAdminTransferFailed(Booking booking, String reason) {
        userRepository.findAllAdmins().forEach(admin ->
                notificationDispatcher.dispatch(
                        admin.getId(),
                        NotificationType.TRANSFER_FAILED,
                        "Échec du virement propriétaire",
                        "Le virement pour la réservation #" + booking.getId() + " a échoué : " + reason,
                        booking.getId(),
                        "BOOKING",
                        Map.of(
                                "bookingId", booking.getId().toString(),
                                "reason", reason
                        ),
                        Map.of()
                )
        );
    }

    private KeyHandover findHandoverByBooking(Long bookingId) {
        return keyHandoverRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Remise des clés introuvable pour cette réservation."));
    }
}
