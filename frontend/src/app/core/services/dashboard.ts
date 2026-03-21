import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LandlordDashboard, MonthlyRevenue, DailyViews } from '../models/dashboard.models';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getLandlordDashboard(from?: string, to?: string): Observable<LandlordDashboard> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<LandlordDashboard>(`${this.apiUrl}/dashboard/landlord`, { params });
  }

  getRevenueChart(months: number): Observable<MonthlyRevenue[]> {
    const params = new HttpParams().set('months', String(months));
    return this.http.get<MonthlyRevenue[]>(`${this.apiUrl}/dashboard/landlord/revenue-chart`, { params });
  }

  getViewsChart(days: number): Observable<DailyViews[]> {
    const params = new HttpParams().set('days', String(days));
    return this.http.get<DailyViews[]>(`${this.apiUrl}/dashboard/landlord/views-chart`, { params });
  }
}
