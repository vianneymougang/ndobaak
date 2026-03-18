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
    private long activeBookings;
    private long pendingRequests;
    private BigDecimal totalRevenue;
    private List<PendingBookingSummary> pendingBookings;
    private List<RecentActivityItem> recentActivity;

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
