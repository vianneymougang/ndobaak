package com.ndobaak.backend.admin.service;

import com.ndobaak.backend.admin.dto.GlobalStatsResponse;
import com.ndobaak.backend.admin.dto.MonthlyStatPoint;
import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.SubscriptionStatus;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import com.ndobaak.backend.subscription.repository.SubscriptionRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminStatisticsService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;

    public AdminStatisticsService(UserRepository userRepository,
                                   ListingRepository listingRepository,
                                   BookingRepository bookingRepository,
                                   PaymentRepository paymentRepository,
                                   SubscriptionRepository subscriptionRepository) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public GlobalStatsResponse getGlobalStats() {
        long totalUsers = userRepository.count();
        long totalLandlords = userRepository.count(hasUserType(Landlord.class));
        long totalStudents = userRepository.count(hasUserType(Student.class));

        long totalListings = listingRepository.count();
        long publishedListings = listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.PUBLISHED).size();
        long pendingListings = listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.PENDING_REVIEW).size();

        long totalBookings = bookingRepository.count();
        long confirmedBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);

        BigDecimal totalRevenue = paymentRepository.sumAmountByStatus(PaymentStatus.COMPLETED);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        long activeSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long newUsersThisMonth = userRepository.countByRegistrationDateAfter(startOfMonth);
        long newBookingsThisMonth = bookingRepository.countByCreatedAtAfter(startOfMonth);

        LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12);
        List<MonthlyStatPoint> monthlyUsers = buildMonthlyUserStats(twelveMonthsAgo);
        List<MonthlyStatPoint> monthlyBookings = buildMonthlyBookingStats(twelveMonthsAgo);
        List<MonthlyStatPoint> monthlyRevenue = buildMonthlyRevenueStats(twelveMonthsAgo);

        return GlobalStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalLandlords(totalLandlords)
                .totalStudents(totalStudents)
                .totalListings(totalListings)
                .publishedListings(publishedListings)
                .pendingListings(pendingListings)
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .totalRevenue(totalRevenue)
                .activeSubscriptions(activeSubscriptions)
                .newUsersThisMonth(newUsersThisMonth)
                .newBookingsThisMonth(newBookingsThisMonth)
                .monthlyUsers(monthlyUsers)
                .monthlyBookings(monthlyBookings)
                .monthlyRevenue(monthlyRevenue)
                .build();
    }

    private List<MonthlyStatPoint> buildMonthlyBookingStats(LocalDateTime since) {
        List<Object[]> rows = bookingRepository.countMonthlyBookings(since);
        List<MonthlyStatPoint> points = new ArrayList<>();
        for (Object[] row : rows) {
            points.add(MonthlyStatPoint.builder()
                    .month((String) row[0])
                    .value((Long) row[1])
                    .build());
        }
        return points;
    }

    private List<MonthlyStatPoint> buildMonthlyRevenueStats(LocalDateTime since) {
        List<Object[]> rows = paymentRepository.sumMonthlyRevenueByStatus(PaymentStatus.COMPLETED, since);
        List<MonthlyStatPoint> points = new ArrayList<>();
        for (Object[] row : rows) {
            points.add(MonthlyStatPoint.builder()
                    .month((String) row[0])
                    .value((BigDecimal) row[1])
                    .build());
        }
        return points;
    }

    private List<MonthlyStatPoint> buildMonthlyUserStats(LocalDateTime since) {
        return List.of();
    }

    private Specification<com.ndobaak.backend.auth.entity.User> hasUserType(Class<?> type) {
        return (root, query, cb) -> cb.equal(root.type(), type);
    }
}
