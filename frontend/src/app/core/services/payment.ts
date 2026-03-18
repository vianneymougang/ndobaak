import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaginatedResponse } from '../models/booking.models';
import {
  InitiatePaymentRequest,
  PayInstallmentRequest,
  PaymentResponse,
  PaymentScheduleResponse,
  ReceiptResponse,
} from '../models/payment.models';

@Injectable({
  providedIn: 'root',
})
export class PaymentService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  initiatePayment(request: InitiatePaymentRequest): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(`${this.apiUrl}/payments/initiate`, request);
  }

  getPaymentsByBooking(bookingId: number): Observable<PaymentResponse[]> {
    return this.http.get<PaymentResponse[]>(`${this.apiUrl}/payments/booking/${bookingId}`);
  }

  getPaymentHistory(page = 0, size = 20): Observable<PaginatedResponse<PaymentResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PaginatedResponse<PaymentResponse>>(`${this.apiUrl}/payments/history`, {
      params,
    });
  }

  getReceipt(paymentId: number): Observable<ReceiptResponse> {
    return this.http.get<ReceiptResponse>(`${this.apiUrl}/payments/${paymentId}/receipt`);
  }

  payInstallment(installmentId: number, request: PayInstallmentRequest): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(
      `${this.apiUrl}/payments/installments/${installmentId}/pay`,
      request,
    );
  }

  getPaymentSchedule(bookingId: number): Observable<PaymentScheduleResponse> {
    return this.http.get<PaymentScheduleResponse>(
      `${this.apiUrl}/payments/schedule/${bookingId}`,
    );
  }
}
