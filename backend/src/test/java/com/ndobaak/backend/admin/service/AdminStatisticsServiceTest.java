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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatisticsServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ListingRepository listingRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private AdminStatisticsService adminStatisticsService;

    @Test
    void getGlobalStatsShouldReturnCorrectUserCounts() {
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.count(any(Specification.class))).thenReturn(20L, 30L);
        when(listingRepository.count()).thenReturn(0L);
        when(listingRepository.findByStatusOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(bookingRepository.count()).thenReturn(0L);
        when(bookingRepository.countByStatus(any())).thenReturn(0L);
        when(paymentRepository.sumAmountByStatus(any())).thenReturn(BigDecimal.ZERO);
        when(subscriptionRepository.countByStatus(any())).thenReturn(0L);
        when(userRepository.countByRegistrationDateAfter(any())).thenReturn(0L);
        when(bookingRepository.countByCreatedAtAfter(any())).thenReturn(0L);
        when(bookingRepository.countMonthlyBookings(any())).thenReturn(List.of());
        when(paymentRepository.sumMonthlyRevenueByStatus(any(), any())).thenReturn(List.of());

        GlobalStatsResponse result = adminStatisticsService.getGlobalStats();

        assertThat(result.getTotalUsers()).isEqualTo(50L);
    }

    @Test
    void getGlobalStatsShouldReturnListingCounts() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.count(any(Specification.class))).thenReturn(0L);
        when(listingRepository.count()).thenReturn(15L);
        when(listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.PUBLISHED)).thenReturn(List.of(
                com.ndobaak.backend.listing.entity.Listing.builder().build(),
                com.ndobaak.backend.listing.entity.Listing.builder().build()
        ));
        when(listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.PENDING_REVIEW)).thenReturn(List.of(
                com.ndobaak.backend.listing.entity.Listing.builder().build()
        ));
        when(bookingRepository.count()).thenReturn(0L);
        when(bookingRepository.countByStatus(any())).thenReturn(0L);
        when(paymentRepository.sumAmountByStatus(any())).thenReturn(BigDecimal.ZERO);
        when(subscriptionRepository.countByStatus(any())).thenReturn(0L);
        when(userRepository.countByRegistrationDateAfter(any())).thenReturn(0L);
        when(bookingRepository.countByCreatedAtAfter(any())).thenReturn(0L);
        when(bookingRepository.countMonthlyBookings(any())).thenReturn(List.of());
        when(paymentRepository.sumMonthlyRevenueByStatus(any(), any())).thenReturn(List.of());

        GlobalStatsResponse result = adminStatisticsService.getGlobalStats();

        assertThat(result.getTotalListings()).isEqualTo(15L);
        assertThat(result.getPublishedListings()).isEqualTo(2L);
        assertThat(result.getPendingListings()).isEqualTo(1L);
    }

    @Test
    void getGlobalStatsShouldReturnRevenueFromRepository() {
        BigDecimal expectedRevenue = BigDecimal.valueOf(250_000);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.count(any(Specification.class))).thenReturn(0L);
        when(listingRepository.count()).thenReturn(0L);
        when(listingRepository.findByStatusOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(bookingRepository.count()).thenReturn(0L);
        when(bookingRepository.countByStatus(any())).thenReturn(0L);
        when(paymentRepository.sumAmountByStatus(PaymentStatus.COMPLETED)).thenReturn(expectedRevenue);
        when(subscriptionRepository.countByStatus(any())).thenReturn(0L);
        when(userRepository.countByRegistrationDateAfter(any())).thenReturn(0L);
        when(bookingRepository.countByCreatedAtAfter(any())).thenReturn(0L);
        when(bookingRepository.countMonthlyBookings(any())).thenReturn(List.of());
        when(paymentRepository.sumMonthlyRevenueByStatus(any(), any())).thenReturn(List.of());

        GlobalStatsResponse result = adminStatisticsService.getGlobalStats();

        assertThat(result.getTotalRevenue()).isEqualByComparingTo(expectedRevenue);
    }

    @Test
    void getGlobalStatsShouldReturnZeroRevenueWhenRepositoryReturnsNull() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.count(any(Specification.class))).thenReturn(0L);
        when(listingRepository.count()).thenReturn(0L);
        when(listingRepository.findByStatusOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(bookingRepository.count()).thenReturn(0L);
        when(bookingRepository.countByStatus(any())).thenReturn(0L);
        when(paymentRepository.sumAmountByStatus(PaymentStatus.COMPLETED)).thenReturn(null);
        when(subscriptionRepository.countByStatus(any())).thenReturn(0L);
        when(userRepository.countByRegistrationDateAfter(any())).thenReturn(0L);
        when(bookingRepository.countByCreatedAtAfter(any())).thenReturn(0L);
        when(bookingRepository.countMonthlyBookings(any())).thenReturn(List.of());
        when(paymentRepository.sumMonthlyRevenueByStatus(any(), any())).thenReturn(List.of());

        GlobalStatsResponse result = adminStatisticsService.getGlobalStats();

        assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getGlobalStatsShouldReturnActiveSubscriptionCount() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.count(any(Specification.class))).thenReturn(0L);
        when(listingRepository.count()).thenReturn(0L);
        when(listingRepository.findByStatusOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(bookingRepository.count()).thenReturn(0L);
        when(bookingRepository.countByStatus(any())).thenReturn(0L);
        when(paymentRepository.sumAmountByStatus(any())).thenReturn(BigDecimal.ZERO);
        when(subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE)).thenReturn(7L);
        when(userRepository.countByRegistrationDateAfter(any())).thenReturn(0L);
        when(bookingRepository.countByCreatedAtAfter(any())).thenReturn(0L);
        when(bookingRepository.countMonthlyBookings(any())).thenReturn(List.of());
        when(paymentRepository.sumMonthlyRevenueByStatus(any(), any())).thenReturn(List.of());

        GlobalStatsResponse result = adminStatisticsService.getGlobalStats();

        assertThat(result.getActiveSubscriptions()).isEqualTo(7L);
    }

    @Test
    void getGlobalStatsShouldReturnMonthlyBookingChartData() {
        List<Object[]> rows = List.of(
                new Object[]{"2026-01", 10L},
                new Object[]{"2026-02", 14L}
        );
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.count(any(Specification.class))).thenReturn(0L);
        when(listingRepository.count()).thenReturn(0L);
        when(listingRepository.findByStatusOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(bookingRepository.count()).thenReturn(0L);
        when(bookingRepository.countByStatus(any())).thenReturn(0L);
        when(paymentRepository.sumAmountByStatus(any())).thenReturn(BigDecimal.ZERO);
        when(subscriptionRepository.countByStatus(any())).thenReturn(0L);
        when(userRepository.countByRegistrationDateAfter(any())).thenReturn(0L);
        when(bookingRepository.countByCreatedAtAfter(any())).thenReturn(0L);
        when(bookingRepository.countMonthlyBookings(any())).thenReturn(rows);
        when(paymentRepository.sumMonthlyRevenueByStatus(any(), any())).thenReturn(List.of());

        GlobalStatsResponse result = adminStatisticsService.getGlobalStats();

        assertThat(result.getMonthlyBookings()).hasSize(2);
        assertThat(result.getMonthlyBookings().get(0).getMonth()).isEqualTo("2026-01");
        assertThat(result.getMonthlyBookings().get(0).getValue()).isEqualTo(10L);
    }

    @Test
    void getGlobalStatsShouldReturnMonthlyRevenueChartData() {
        List<Object[]> rows = List.of(
                new Object[]{"2026-01", BigDecimal.valueOf(100_000)},
                new Object[]{"2026-02", BigDecimal.valueOf(200_000)}
        );
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.count(any(Specification.class))).thenReturn(0L);
        when(listingRepository.count()).thenReturn(0L);
        when(listingRepository.findByStatusOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(bookingRepository.count()).thenReturn(0L);
        when(bookingRepository.countByStatus(any())).thenReturn(0L);
        when(paymentRepository.sumAmountByStatus(any())).thenReturn(BigDecimal.ZERO);
        when(subscriptionRepository.countByStatus(any())).thenReturn(0L);
        when(userRepository.countByRegistrationDateAfter(any())).thenReturn(0L);
        when(bookingRepository.countByCreatedAtAfter(any())).thenReturn(0L);
        when(bookingRepository.countMonthlyBookings(any())).thenReturn(List.of());
        when(paymentRepository.sumMonthlyRevenueByStatus(eq(PaymentStatus.COMPLETED), any())).thenReturn(rows);

        GlobalStatsResponse result = adminStatisticsService.getGlobalStats();

        assertThat(result.getMonthlyRevenue()).hasSize(2);
        assertThat(result.getMonthlyRevenue().get(1).getMonth()).isEqualTo("2026-02");
        assertThat((BigDecimal) result.getMonthlyRevenue().get(1).getValue())
                .isEqualByComparingTo(BigDecimal.valueOf(200_000));
    }

    @Test
    void getGlobalStatsShouldReturnNewUsersAndBookingsThisMonth() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.count(any(Specification.class))).thenReturn(0L);
        when(listingRepository.count()).thenReturn(0L);
        when(listingRepository.findByStatusOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(bookingRepository.count()).thenReturn(0L);
        when(bookingRepository.countByStatus(any())).thenReturn(0L);
        when(paymentRepository.sumAmountByStatus(any())).thenReturn(BigDecimal.ZERO);
        when(subscriptionRepository.countByStatus(any())).thenReturn(0L);
        when(userRepository.countByRegistrationDateAfter(any())).thenReturn(5L);
        when(bookingRepository.countByCreatedAtAfter(any())).thenReturn(3L);
        when(bookingRepository.countMonthlyBookings(any())).thenReturn(List.of());
        when(paymentRepository.sumMonthlyRevenueByStatus(any(), any())).thenReturn(List.of());

        GlobalStatsResponse result = adminStatisticsService.getGlobalStats();

        assertThat(result.getNewUsersThisMonth()).isEqualTo(5L);
        assertThat(result.getNewBookingsThisMonth()).isEqualTo(3L);
    }
}
