package com.ndobaak.backend.dashboard.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.dashboard.dto.BookingsByStatusDTO;
import com.ndobaak.backend.dashboard.dto.DailyViewsDTO;
import com.ndobaak.backend.dashboard.dto.LandlordDashboardResponse;
import com.ndobaak.backend.dashboard.dto.MonthlyRevenueDTO;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.service.ListingViewService;
import com.ndobaak.backend.notification.entity.Notification;
import com.ndobaak.backend.notification.repository.NotificationRepository;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import com.ndobaak.backend.subscription.dto.SubscriptionMapper;
import com.ndobaak.backend.subscription.dto.SubscriptionResponse;
import com.ndobaak.backend.subscription.repository.SubscriptionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final AuthService authService;
    private final ListingRepository listingRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final ListingViewService listingViewService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public DashboardService(AuthService authService,
                            ListingRepository listingRepository,
                            BookingRepository bookingRepository,
                            PaymentRepository paymentRepository,
                            NotificationRepository notificationRepository,
                            ListingViewService listingViewService,
                            SubscriptionRepository subscriptionRepository,
                            SubscriptionMapper subscriptionMapper) {
        this.authService = authService;
        this.listingRepository = listingRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
        this.listingViewService = listingViewService;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionMapper = subscriptionMapper;
    }

    @Transactional(readOnly = true)
    public LandlordDashboardResponse getLandlordDashboard(LocalDate from, LocalDate to) {
        User landlord = authService.getAuthenticatedUser();
        Long landlordId = landlord.getId();

        long totalListings = listingRepository.countByLandlordId(landlordId);
        long confirmedBookings = bookingRepository.countByListingLandlordIdAndStatus(landlordId, BookingStatus.CONFIRMED);
        long completedBookings = bookingRepository.countByListingLandlordIdAndStatus(landlordId, BookingStatus.COMPLETED);
        long activeBookings = confirmedBookings + completedBookings;
        long pendingRequests = bookingRepository.countByListingLandlordIdAndStatus(landlordId, BookingStatus.PENDING);
        long cancelledBookings = bookingRepository.countByListingLandlordIdAndStatus(landlordId, BookingStatus.CANCELLED);

        BigDecimal totalRevenue = paymentRepository.sumAmountByLandlordIdAndStatus(landlordId, PaymentStatus.COMPLETED);

        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevFrom = from.minusDays(periodDays);
        LocalDate prevTo = from.minusDays(1);
        BigDecimal previousRevenue = paymentRepository.sumAmountByLandlordIdAndStatusAndPeriod(
                landlordId, PaymentStatus.COMPLETED,
                prevFrom.atStartOfDay(), prevTo.atTime(23, 59, 59));
        BigDecimal currentRevenue = paymentRepository.sumAmountByLandlordIdAndStatusAndPeriod(
                landlordId, PaymentStatus.COMPLETED,
                from.atStartOfDay(), to.atTime(23, 59, 59));

        double revenueChangePercent = 0.0;
        if (previousRevenue != null && previousRevenue.compareTo(BigDecimal.ZERO) > 0) {
            revenueChangePercent = (currentRevenue != null ? currentRevenue : BigDecimal.ZERO)
                    .subtract(previousRevenue)
                    .divide(previousRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        long totalAvailableRooms = listingRepository.sumAvailableRoomsByLandlordIdAndStatus(landlordId, ListingStatus.PUBLISHED);
        double occupancyRate = 0.0;
        if (totalAvailableRooms > 0) {
            occupancyRate = Math.min(
                    (double) (confirmedBookings + completedBookings) / totalAvailableRooms * 100,
                    100.0);
        }

        Map<Long, Long> viewsMap = listingViewService.getViewsForLandlordListings(landlordId, from, to);
        long totalViews = viewsMap.values().stream().mapToLong(Long::longValue).sum();

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

        List<MonthlyRevenueDTO> revenueChart = buildRevenueChart(landlordId, 12);
        List<DailyViewsDTO> viewsChart = listingViewService.getDailyViewsForLandlord(landlordId, from, to);
        BookingsByStatusDTO bookingsByStatus = new BookingsByStatusDTO(
                pendingRequests, confirmedBookings, completedBookings, cancelledBookings);

        return LandlordDashboardResponse.builder()
                .totalListings(totalListings)
                .activeBookings(activeBookings)
                .pendingRequests(pendingRequests)
                .totalRevenue(totalRevenue)
                .totalViews(totalViews)
                .occupancyRate(occupancyRate)
                .confirmedBookings(confirmedBookings)
                .completedBookings(completedBookings)
                .previousPeriodRevenue(previousRevenue)
                .revenueChangePercent(revenueChangePercent)
                .pendingBookings(pendingBookings)
                .recentActivity(recentActivity)
                .revenueChart(revenueChart)
                .viewsChart(viewsChart)
                .bookingsByStatus(bookingsByStatus)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MonthlyRevenueDTO> getRevenueChart(int months) {
        User landlord = authService.getAuthenticatedUser();
        return buildRevenueChart(landlord.getId(), months);
    }

    @Transactional(readOnly = true)
    public List<DailyViewsDTO> getViewsChart(int days) {
        User landlord = authService.getAuthenticatedUser();
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days - 1);
        return listingViewService.getDailyViewsForLandlord(landlord.getId(), from, to);
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionResponse> getSubscriptionStatus(Long landlordId) {
        return subscriptionRepository.findByLandlordId(landlordId)
                .map(subscriptionMapper::toResponse);
    }

    private List<MonthlyRevenueDTO> buildRevenueChart(Long landlordId, int months) {
        LocalDate startOfFirstMonth = LocalDate.now().minusMonths(months - 1).withDayOfMonth(1);
        List<Object[]> data = paymentRepository.findMonthlyRevenue(landlordId, startOfFirstMonth.atStartOfDay());
        Map<String, Object[]> dataMap = data.stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> row));
        List<MonthlyRevenueDTO> result = new ArrayList<>();
        LocalDate cursor = startOfFirstMonth;
        for (int i = 0; i < months; i++) {
            String month = cursor.format(MONTH_FMT);
            Object[] row = dataMap.get(month);
            if (row != null) {
                result.add(new MonthlyRevenueDTO(month, new BigDecimal(row[1].toString()), ((Number) row[2]).intValue()));
            } else {
                result.add(new MonthlyRevenueDTO(month, BigDecimal.ZERO, 0));
            }
            cursor = cursor.plusMonths(1);
        }
        return result;
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
