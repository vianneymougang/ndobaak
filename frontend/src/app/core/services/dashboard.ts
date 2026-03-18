import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

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

export interface LandlordDashboardResponse {
  totalListings: number;
  activeBookings: number;
  pendingRequests: number;
  totalRevenue: number;
  pendingBookings: PendingBookingSummary[];
  recentActivity: RecentActivityItem[];
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getLandlordDashboard(): Observable<LandlordDashboardResponse> {
    return this.http.get<LandlordDashboardResponse>(`${this.apiUrl}/dashboard/landlord`);
  }
}
