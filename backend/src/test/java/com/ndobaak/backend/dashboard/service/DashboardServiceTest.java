package com.ndobaak.backend.dashboard.service;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.dashboard.dto.LandlordDashboardResponse;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.notification.entity.Notification;
import com.ndobaak.backend.notification.repository.NotificationRepository;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AuthService authService;
    @Mock
    private ListingRepository listingRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Landlord buildLandlord(Long id) {
        Landlord landlord = new Landlord();
        landlord.setId(id);
        landlord.setFirstName("Jane");
        landlord.setLastName("Smith");
        landlord.setEmail("jane@test.com");
        return landlord;
    }

    private Student buildStudent(Long id, String firstName, String lastName) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(firstName.toLowerCase() + "@test.com");
        return student;
    }

    private Listing buildListing(Long id, Landlord landlord) {
        return Listing.builder()
                .id(id)
                .title("Studio Eyang")
                .landlord(landlord)
                .build();
    }

    private Booking buildBooking(Long id, Student student, Listing listing, BookingStatus status,
                                 LocalDate startDate, LocalDateTime createdAt) {
        Booking booking = Booking.builder()
                .student(student)
                .listing(listing)
                .startDate(startDate)
                .endDate(startDate.plusMonths(3))
                .monthlyRent(new BigDecimal("35000"))
                .totalAmount(new BigDecimal("105000"))
                .status(status)
                .build();
        booking.setId(id);
        booking.setCreatedAt(createdAt);
        return booking;
    }

    private Notification buildNotification(Long id, Landlord landlord, NotificationType type,
                                           String title, String message, Long referenceId,
                                           LocalDateTime createdAt) {
        return Notification.builder()
                .id(id)
                .user(landlord)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .createdAt(createdAt)
                .build();
    }

    private void stubEmptyDashboard(Long landlordId) {
        when(listingRepository.countByLandlordId(landlordId)).thenReturn(0L);
        when(bookingRepository.countByListingLandlordIdAndStatusIn(eq(landlordId), any())).thenReturn(0L);
        when(bookingRepository.countByListingLandlordIdAndStatus(landlordId, BookingStatus.PENDING)).thenReturn(0L);
        when(paymentRepository.sumAmountByLandlordIdAndStatus(landlordId, PaymentStatus.COMPLETED))
                .thenReturn(BigDecimal.ZERO);
        when(bookingRepository.findByListingLandlordIdAndStatus(eq(landlordId), eq(BookingStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(landlordId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
    }

    @Test
    void getLandlordDashboardShouldReturnAllKpis() {
        Landlord landlord = buildLandlord(10L);
        Student student = buildStudent(1L, "John", "Doe");
        Listing listing = buildListing(100L, landlord);
        LocalDateTime now = LocalDateTime.of(2026, 3, 18, 14, 30, 0);
        Booking pendingBooking = buildBooking(1L, student, listing, BookingStatus.PENDING,
                LocalDate.of(2026, 4, 1), now);
        Notification notification = buildNotification(1L, landlord, NotificationType.BOOKING_CREATED,
                "Nouvelle reservation", "Une reservation a ete creee.", 1L, now);

        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(listingRepository.countByLandlordId(10L)).thenReturn(5L);
        when(bookingRepository.countByListingLandlordIdAndStatusIn(eq(10L),
                eq(List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED)))).thenReturn(3L);
        when(bookingRepository.countByListingLandlordIdAndStatus(10L, BookingStatus.PENDING)).thenReturn(2L);
        when(paymentRepository.sumAmountByLandlordIdAndStatus(10L, PaymentStatus.COMPLETED))
                .thenReturn(new BigDecimal("210000"));
        when(bookingRepository.findByListingLandlordIdAndStatus(eq(10L), eq(BookingStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pendingBooking)));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));

        LandlordDashboardResponse response = dashboardService.getLandlordDashboard();

        assertThat(response.getTotalListings()).isEqualTo(5L);
        assertThat(response.getActiveBookings()).isEqualTo(3L);
        assertThat(response.getPendingRequests()).isEqualTo(2L);
        assertThat(response.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("210000"));
        assertThat(response.getPendingBookings()).hasSize(1);
        assertThat(response.getRecentActivity()).hasSize(1);
    }

    @Test
    void getLandlordDashboardShouldReturnZerosWhenNoData() {
        Landlord landlord = buildLandlord(10L);
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        stubEmptyDashboard(10L);

        LandlordDashboardResponse response = dashboardService.getLandlordDashboard();

        assertThat(response.getTotalListings()).isZero();
        assertThat(response.getActiveBookings()).isZero();
        assertThat(response.getPendingRequests()).isZero();
        assertThat(response.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getPendingBookings()).isEmpty();
        assertThat(response.getRecentActivity()).isEmpty();
    }

    @Test
    void getLandlordDashboardShouldHandleNullRevenue() {
        Landlord landlord = buildLandlord(10L);
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(listingRepository.countByLandlordId(10L)).thenReturn(0L);
        when(bookingRepository.countByListingLandlordIdAndStatusIn(eq(10L), any())).thenReturn(0L);
        when(bookingRepository.countByListingLandlordIdAndStatus(10L, BookingStatus.PENDING)).thenReturn(0L);
        when(paymentRepository.sumAmountByLandlordIdAndStatus(10L, PaymentStatus.COMPLETED))
                .thenReturn(null);
        when(bookingRepository.findByListingLandlordIdAndStatus(eq(10L), eq(BookingStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        LandlordDashboardResponse response = dashboardService.getLandlordDashboard();

        assertThat(response.getTotalRevenue()).isNull();
    }

    @Test
    void getLandlordDashboardShouldMapPendingBookingsCorrectly() {
        Landlord landlord = buildLandlord(10L);
        Student student = buildStudent(1L, "Paul", "Biya");
        Listing listing = buildListing(100L, landlord);
        listing.setTitle("Chambre Moderne Eyang");
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 15, 9, 45, 30);
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        Booking booking = buildBooking(42L, student, listing, BookingStatus.PENDING, startDate, createdAt);

        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(listingRepository.countByLandlordId(10L)).thenReturn(1L);
        when(bookingRepository.countByListingLandlordIdAndStatusIn(eq(10L), any())).thenReturn(0L);
        when(bookingRepository.countByListingLandlordIdAndStatus(10L, BookingStatus.PENDING)).thenReturn(1L);
        when(paymentRepository.sumAmountByLandlordIdAndStatus(10L, PaymentStatus.COMPLETED))
                .thenReturn(BigDecimal.ZERO);
        when(bookingRepository.findByListingLandlordIdAndStatus(eq(10L), eq(BookingStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        LandlordDashboardResponse response = dashboardService.getLandlordDashboard();

        assertThat(response.getPendingBookings()).hasSize(1);
        LandlordDashboardResponse.PendingBookingSummary summary = response.getPendingBookings().getFirst();
        assertThat(summary.getBookingId()).isEqualTo(42L);
        assertThat(summary.getStudentName()).isEqualTo("Paul Biya");
        assertThat(summary.getListingTitle()).isEqualTo("Chambre Moderne Eyang");
        assertThat(summary.getStartDate()).isEqualTo("2026-05-01");
        assertThat(summary.getCreatedAt()).isEqualTo("2026-03-15T09:45:30");
    }

    @Test
    void getLandlordDashboardShouldMapRecentActivityCorrectly() {
        Landlord landlord = buildLandlord(10L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 17, 16, 20, 0);
        Notification notification = buildNotification(5L, landlord, NotificationType.PAYMENT_RECEIVED,
                "Paiement recu", "Le paiement de 35 000 FCFA a ete recu.", 77L, createdAt);

        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(listingRepository.countByLandlordId(10L)).thenReturn(0L);
        when(bookingRepository.countByListingLandlordIdAndStatusIn(eq(10L), any())).thenReturn(0L);
        when(bookingRepository.countByListingLandlordIdAndStatus(10L, BookingStatus.PENDING)).thenReturn(0L);
        when(paymentRepository.sumAmountByLandlordIdAndStatus(10L, PaymentStatus.COMPLETED))
                .thenReturn(BigDecimal.ZERO);
        when(bookingRepository.findByListingLandlordIdAndStatus(eq(10L), eq(BookingStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));

        LandlordDashboardResponse response = dashboardService.getLandlordDashboard();

        assertThat(response.getRecentActivity()).hasSize(1);
        LandlordDashboardResponse.RecentActivityItem activity = response.getRecentActivity().getFirst();
        assertThat(activity.getType()).isEqualTo("PAYMENT_RECEIVED");
        assertThat(activity.getTitle()).isEqualTo("Paiement recu");
        assertThat(activity.getMessage()).isEqualTo("Le paiement de 35 000 FCFA a ete recu.");
        assertThat(activity.getCreatedAt()).isEqualTo("2026-03-17T16:20:00");
        assertThat(activity.getReferenceId()).isEqualTo(77L);
    }

    @Test
    void getLandlordDashboardShouldReturnEmptyListsWhenNoPendingOrActivity() {
        Landlord landlord = buildLandlord(10L);
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(listingRepository.countByLandlordId(10L)).thenReturn(3L);
        when(bookingRepository.countByListingLandlordIdAndStatusIn(eq(10L), any())).thenReturn(2L);
        when(bookingRepository.countByListingLandlordIdAndStatus(10L, BookingStatus.PENDING)).thenReturn(0L);
        when(paymentRepository.sumAmountByLandlordIdAndStatus(10L, PaymentStatus.COMPLETED))
                .thenReturn(new BigDecimal("70000"));
        when(bookingRepository.findByListingLandlordIdAndStatus(eq(10L), eq(BookingStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        LandlordDashboardResponse response = dashboardService.getLandlordDashboard();

        assertThat(response.getTotalListings()).isEqualTo(3L);
        assertThat(response.getActiveBookings()).isEqualTo(2L);
        assertThat(response.getPendingRequests()).isZero();
        assertThat(response.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("70000"));
        assertThat(response.getPendingBookings()).isEmpty();
        assertThat(response.getRecentActivity()).isEmpty();
    }

    @Test
    void getLandlordDashboardShouldCountOnlyConfirmedAndCompletedAsActive() {
        Landlord landlord = buildLandlord(10L);
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        stubEmptyDashboard(10L);

        dashboardService.getLandlordDashboard();

        verify(bookingRepository).countByListingLandlordIdAndStatusIn(10L,
                List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));
    }
}
