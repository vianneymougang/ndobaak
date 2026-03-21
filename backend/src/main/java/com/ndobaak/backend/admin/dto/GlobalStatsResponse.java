package com.ndobaak.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalStatsResponse {
    private long totalUsers;
    private long totalLandlords;
    private long totalStudents;
    private long totalListings;
    private long publishedListings;
    private long pendingListings;
    private long totalBookings;
    private long confirmedBookings;
    private BigDecimal totalRevenue;
    private long activeSubscriptions;
    private long newUsersThisMonth;
    private long newBookingsThisMonth;
    private List<MonthlyStatPoint> monthlyUsers;
    private List<MonthlyStatPoint> monthlyBookings;
    private List<MonthlyStatPoint> monthlyRevenue;
}
