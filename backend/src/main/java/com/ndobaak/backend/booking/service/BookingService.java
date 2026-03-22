package com.ndobaak.backend.booking.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.dto.BookingResponse;
import com.ndobaak.backend.booking.dto.CreateBookingRequest;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.mapper.BookingMapper;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.notification.service.NotificationDispatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class BookingService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final AuthService authService;
    private final NotificationDispatcher notificationDispatcher;
    private final BookingMapper bookingMapper;

    public BookingService(BookingRepository bookingRepository, ListingRepository listingRepository,
                          AuthService authService, NotificationDispatcher notificationDispatcher,
                          BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.authService = authService;
        this.notificationDispatcher = notificationDispatcher;
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        User student = authService.getAuthenticatedUser();

        Listing listing = listingRepository.findByIdWithDetails(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", request.getListingId()));

        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw new BusinessException("Cette annonce n'est pas disponible à la réservation");
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BusinessException("La date de fin doit être après la date de début");
        }

        boolean hasActiveBooking = bookingRepository.existsByStudentIdAndListingIdAndStatusIn(
                student.getId(), listing.getId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        if (hasActiveBooking) {
            throw new BusinessException("Vous avez déjà une réservation active pour cette annonce");
        }

        long months = ChronoUnit.MONTHS.between(request.getStartDate(), request.getEndDate());
        if (months < 1) months = 1;

        BigDecimal monthlyRent = listing.getMonthlyPrice();
        BigDecimal totalAmount = monthlyRent.multiply(BigDecimal.valueOf(months));

        Booking booking = Booking.builder()
                .student(student)
                .listing(listing)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .contractDuration((int) months)
                .monthlyRent(monthlyRent)
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .build();

        booking = bookingRepository.save(booking);

        String studentFullName = student.getFirstName() + " " + student.getLastName();
        notificationDispatcher.dispatch(
                listing.getLandlord().getId(),
                NotificationType.BOOKING_CREATED,
                "Nouvelle demande de réservation",
                studentFullName + " souhaite réserver : " + listing.getTitle(),
                booking.getId(),
                "BOOKING",
                Map.of(
                        "landlordName", listing.getLandlord().getFirstName(),
                        "studentName", studentFullName,
                        "listingTitle", listing.getTitle(),
                        "startDate", request.getStartDate().format(DATE_FMT)
                ),
                Map.of()
        );

        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = findBookingById(bookingId);
        checkBookingAccess(booking, authService.getAuthenticatedUser());
        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getStudentBookings(BookingStatus status, Pageable pageable) {
        User student = authService.getAuthenticatedUser();
        Page<Booking> bookings = (status != null)
                ? bookingRepository.findByStudentIdAndStatus(student.getId(), status, pageable)
                : bookingRepository.findByStudentId(student.getId(), pageable);
        return bookings.map(bookingMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getLandlordBookings(BookingStatus status, Pageable pageable) {
        User landlord = authService.getAuthenticatedUser();
        Page<Booking> bookings = (status != null)
                ? bookingRepository.findByListingLandlordIdAndStatus(landlord.getId(), status, pageable)
                : bookingRepository.findByListingLandlordId(landlord.getId(), pageable);
        return bookings.map(bookingMapper::toResponse);
    }

    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {
        User landlord = authService.getAuthenticatedUser();
        Booking booking = findBookingById(bookingId);

        if (!booking.getListing().getLandlord().getId().equals(landlord.getId())) {
            throw new ForbiddenException("Vous ne pouvez confirmer que les réservations de vos annonces");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Seules les réservations en attente peuvent être confirmées");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setLandlordResponseDate(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        String studentFullName = booking.getStudent().getFirstName() + " " + booking.getStudent().getLastName();
        notificationDispatcher.dispatch(
                booking.getStudent().getId(),
                NotificationType.BOOKING_CONFIRMED,
                "Réservation confirmée",
                "Votre réservation pour \"" + booking.getListing().getTitle() + "\" a été confirmée.",
                booking.getId(),
                "BOOKING",
                Map.of(
                        "studentName", studentFullName,
                        "listingTitle", booking.getListing().getTitle(),
                        "startDate", booking.getStartDate().format(DATE_FMT),
                        "endDate", booking.getEndDate().format(DATE_FMT),
                        "totalAmount", String.format("%,.0f FCFA", booking.getTotalAmount())
                ),
                Map.of(
                        "listingTitle", booking.getListing().getTitle(),
                        "startDate", booking.getStartDate().format(DATE_FMT),
                        "endDate", booking.getEndDate().format(DATE_FMT)
                )
        );

        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse rejectBooking(Long bookingId, String reason) {
        User landlord = authService.getAuthenticatedUser();
        Booking booking = findBookingById(bookingId);

        if (!booking.getListing().getLandlord().getId().equals(landlord.getId())) {
            throw new ForbiddenException("Vous ne pouvez rejeter que les réservations de vos annonces");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Seules les réservations en attente peuvent être rejetées");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setLandlordResponseDate(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        String notificationMessage = "Votre réservation pour \"" + booking.getListing().getTitle() + "\" a été refusée.";
        if (reason != null && !reason.isBlank()) {
            notificationMessage += " Motif : " + reason;
        }

        notificationDispatcher.dispatch(
                booking.getStudent().getId(),
                NotificationType.BOOKING_CANCELLED,
                "Réservation refusée",
                notificationMessage,
                booking.getId(),
                "BOOKING",
                Map.of("listingTitle", booking.getListing().getTitle()),
                Map.of()
        );

        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        User currentUser = authService.getAuthenticatedUser();
        Booking booking = findBookingById(bookingId);
        checkBookingAccess(booking, currentUser);

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Seules les réservations en attente ou confirmées peuvent être annulées");
        }

        Long otherUserId = currentUser.getId().equals(booking.getStudent().getId())
                ? booking.getListing().getLandlord().getId()
                : booking.getStudent().getId();

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason("Annulé par l'utilisateur");
        booking = bookingRepository.save(booking);

        notificationDispatcher.dispatch(
                otherUserId,
                NotificationType.BOOKING_CANCELLED,
                "Réservation annulée",
                "La réservation pour \"" + booking.getListing().getTitle() + "\" a été annulée.",
                booking.getId(),
                "BOOKING",
                Map.of("listingTitle", booking.getListing().getTitle()),
                Map.of()
        );

        return bookingMapper.toResponse(booking);
    }

    private Booking findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
    }

    private void checkBookingAccess(Booking booking, User user) {
        boolean isStudent = user.getId().equals(booking.getStudent().getId());
        boolean isLandlord = user.getId().equals(booking.getListing().getLandlord().getId());
        if (!isStudent && !isLandlord) {
            throw new ForbiddenException("Vous n'avez pas accès à cette réservation");
        }
    }
}
