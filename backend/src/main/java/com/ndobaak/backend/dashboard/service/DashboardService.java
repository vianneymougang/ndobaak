package com.ndobaak.backend.dashboard.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.dashboard.dto.LandlordDashboardResponse;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.notification.entity.Notification;
import com.ndobaak.backend.notification.repository.NotificationRepository;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DashboardService {

    private final AuthService authService;
    private final ListingRepository listingRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public DashboardService(AuthService authService,
                            ListingRepository listingRepository,
                            BookingRepository bookingRepository,
                            PaymentRepository paymentRepository,
                            NotificationRepository notificationRepository) {
        this.authService = authService;
        this.listingRepository = listingRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public LandlordDashboardResponse getLandlordDashboard() {
        User landlord = authService.getAuthenticatedUser();
        Long landlordId = landlord.getId();

        long totalListings = listingRepository.countByLandlordId(landlordId);
        long activeBookings = bookingRepository.countByListingLandlordIdAndStatusIn(landlordId,
                List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));
        long pendingRequests = bookingRepository.countByListingLandlordIdAndStatus(landlordId, BookingStatus.PENDING);
        BigDecimal totalRevenue = paymentRepository.sumAmountByLandlordIdAndStatus(landlordId, PaymentStatus.COMPLETED);

        PageRequest pendingPageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        List<LandlordDashboardResponse.PendingBookingSummary> pendingBookings =
                bookingRepository.findByListingLandlordIdAndStatus(landlordId, BookingStatus.PENDING, pendingPageable)
                        .getContent()
                        .stream()
                        .map(this::toPendingBookingSummary)
                        .toList();

        PageRequest activityPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        List<LandlordDashboardResponse.RecentActivityItem> recentActivity =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(landlordId, activityPageable)
                        .getContent()
                        .stream()
                        .map(this::toRecentActivityItem)
                        .toList();

        return LandlordDashboardResponse.builder()
                .totalListings(totalListings)
                .activeBookings(activeBookings)
                .pendingRequests(pendingRequests)
                .totalRevenue(totalRevenue)
                .pendingBookings(pendingBookings)
                .recentActivity(recentActivity)
                .build();
    }

    private LandlordDashboardResponse.PendingBookingSummary toPendingBookingSummary(Booking booking) {
        return LandlordDashboardResponse.PendingBookingSummary.builder()
                .bookingId(booking.getId())
                .studentName(booking.getStudent().getFirstName() + " " + booking.getStudent().getLastName())
                .listingTitle(booking.getListing().getTitle())
                .startDate(booking.getStartDate().format(DATE_FMT))
                .createdAt(booking.getCreatedAt().format(DATETIME_FMT))
                .build();
    }

    private LandlordDashboardResponse.RecentActivityItem toRecentActivityItem(Notification notification) {
        return LandlordDashboardResponse.RecentActivityItem.builder()
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .createdAt(notification.getCreatedAt().format(DATETIME_FMT))
                .referenceId(notification.getReferenceId())
                .build();
    }
}
