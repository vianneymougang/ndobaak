package com.ndobaak.backend.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LandlordDashboardResponse {

    private long totalListings;
    private long activeListings;
    private long totalBookings;
    private long pendingBookings;
    private BigDecimal totalRevenue;
    private long totalViews;
    private double occupancyRate;
    private long confirmedBookings;
    private long completedBookings;
    private BigDecimal previousPeriodRevenue;
    private double revenueChangePercent;
    private List<PendingBookingSummary> recentPendingBookings;
    private List<RecentActivityItem> recentActivities;
    private List<MonthlyRevenueDTO> revenueChart;
    private List<DailyViewsDTO> viewsChart;
    private BookingsByStatusDTO bookingsByStatus;

    @Getter
    @Setter
    @Builder
    public static class PendingBookingSummary {
        private Long bookingId;
        private String studentName;
        private String listingTitle;
        private String startDate;
        private String createdAt;
    }

    @Getter
    @Setter
    @Builder
    public static class RecentActivityItem {
        private String type;
        private String title;
        private String message;
        private String createdAt;
        private Long referenceId;
    }
}
