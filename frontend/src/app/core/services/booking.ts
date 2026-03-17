import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BookingResponse,
  BookingStatus,
  CreateBookingRequest,
  PaginatedResponse,
} from '../models/booking.models';

@Injectable({
  providedIn: 'root',
})
export class BookingService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  createBooking(request: CreateBookingRequest): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(`${this.apiUrl}/bookings`, request);
  }

  getBookingById(id: number): Observable<BookingResponse> {
    return this.http.get<BookingResponse>(`${this.apiUrl}/bookings/${id}`);
  }

  getMyBookings(
    status?: BookingStatus,
    page = 0,
    size = 10,
  ): Observable<PaginatedResponse<BookingResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<PaginatedResponse<BookingResponse>>(`${this.apiUrl}/bookings/my`, {
      params,
    });
  }

  getLandlordBookings(
    status?: BookingStatus,
    page = 0,
    size = 10,
  ): Observable<PaginatedResponse<BookingResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<PaginatedResponse<BookingResponse>>(`${this.apiUrl}/bookings/landlord`, {
      params,
    });
  }

  confirmBooking(id: number): Observable<BookingResponse> {
    return this.http.put<BookingResponse>(`${this.apiUrl}/bookings/${id}/confirm`, {});
  }

  rejectBooking(id: number, reason?: string): Observable<BookingResponse> {
    return this.http.put<BookingResponse>(`${this.apiUrl}/bookings/${id}/reject`, { reason });
  }

  cancelBooking(id: number): Observable<BookingResponse> {
    return this.http.put<BookingResponse>(`${this.apiUrl}/bookings/${id}/cancel`, {});
  }
}
