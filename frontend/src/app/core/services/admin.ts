import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminPage,
  AdminReview,
  AdminTransaction,
  AdminUserDetail,
  AdminUserListItem,
  DeactivateUserRequest,
  GlobalStats,
  RejectionMotifRequest,
  ResolveReportRequest,
  ReviewReport,
} from '../models/admin.models';
import { ListingResponse } from '../models/listing.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getUsers(
    role?: string,
    status?: string,
    search?: string,
    page = 0,
    size = 20,
  ): Observable<AdminPage<AdminUserListItem>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (role) params = params.set('role', role);
    if (status) params = params.set('status', status);
    if (search) params = params.set('search', search);
    return this.http.get<AdminPage<AdminUserListItem>>(`${this.apiUrl}/admin/users`, { params });
  }

  getUserDetail(id: number): Observable<AdminUserDetail> {
    return this.http.get<AdminUserDetail>(`${this.apiUrl}/admin/users/${id}`);
  }

  deactivateUser(id: number, request: DeactivateUserRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/admin/users/${id}/deactivate`, request);
  }

  reactivateUser(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/admin/users/${id}/reactivate`, null);
  }

  getListings(
    status?: string,
    search?: string,
    page = 0,
    size = 20,
  ): Observable<AdminPage<ListingResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (search) params = params.set('search', search);
    return this.http.get<AdminPage<ListingResponse>>(`${this.apiUrl}/admin/listings`, { params });
  }

  getPendingListings(): Observable<ListingResponse[]> {
    return this.http.get<ListingResponse[]>(`${this.apiUrl}/admin/listings/pending`);
  }

  validateListing(id: number): Observable<ListingResponse> {
    return this.http.put<ListingResponse>(`${this.apiUrl}/admin/listings/${id}/validate`, null);
  }

  rejectListing(id: number, request: RejectionMotifRequest): Observable<ListingResponse> {
    return this.http.put<ListingResponse>(`${this.apiUrl}/admin/listings/${id}/reject`, request);
  }

  getReviews(
    queue: 'pending' | 'flagged' | 'all',
    page = 0,
    size = 20,
  ): Observable<AdminPage<AdminReview>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<AdminPage<AdminReview>>(`${this.apiUrl}/admin/reviews/${queue}`, {
      params,
    });
  }

  approveReview(id: number): Observable<AdminReview> {
    return this.http.put<AdminReview>(`${this.apiUrl}/admin/reviews/${id}/approve`, null);
  }

  rejectReview(id: number): Observable<AdminReview> {
    return this.http.put<AdminReview>(`${this.apiUrl}/admin/reviews/${id}/reject`, null);
  }

  getReviewReports(reviewId: number): Observable<AdminPage<ReviewReport>> {
    return this.http.get<AdminPage<ReviewReport>>(
      `${this.apiUrl}/admin/reviews/${reviewId}/reports`,
    );
  }

  resolveReport(reportId: number, request: ResolveReportRequest): Observable<ReviewReport> {
    return this.http.put<ReviewReport>(
      `${this.apiUrl}/admin/reports/${reportId}/resolve`,
      request,
    );
  }

  getGlobalStats(): Observable<GlobalStats> {
    return this.http.get<GlobalStats>(`${this.apiUrl}/admin/statistics`);
  }

  getTransactions(
    status?: string,
    type?: string,
    page = 0,
    size = 20,
  ): Observable<AdminPage<AdminTransaction>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (type) params = params.set('type', type);
    return this.http.get<AdminPage<AdminTransaction>>(`${this.apiUrl}/admin/transactions`, {
      params,
    });
  }

  getAnomalies(): Observable<AdminTransaction[]> {
    return this.http.get<AdminTransaction[]>(`${this.apiUrl}/admin/transactions/anomalies`);
  }

  getListingDetail(id: number): Observable<ListingResponse> {
    return this.http.get<ListingResponse>(`${this.apiUrl}/listings/${id}`);
  }
}
