export interface MonthlyRevenue {
  month: string;
  revenue: number;
  bookingCount: number;
}

export interface DailyViews {
  date: string;
  viewCount: number;
}

export interface BookingsByStatus {
  pending: number;
  confirmed: number;
  completed: number;
  cancelled: number;
}

export interface PendingBookingSummary {
  bookingId: number;
  studentName: string;
  listingTitle: string;
  startDate: string;
  createdAt: string;
}

export interface RecentActivityItem {
  type: string;
  title: string;
  message: string;
  createdAt: string;
  referenceId: number;
}

export interface LandlordDashboard {
  totalListings: number;
  activeListings: number;
  totalBookings: number;
  confirmedBookings: number;
  pendingBookings: number;
  completedBookings: number;
  totalRevenue: number;
  previousPeriodRevenue: number;
  revenueChangePercent: number;
  totalViews: number;
  occupancyRate: number;
  bookingsByStatus: BookingsByStatus;
  subscription: { status: string; endDate: string; daysRemaining: number } | null;
  recentPendingBookings: PendingBookingSummary[];
  recentActivities: RecentActivityItem[];
}
