import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface GuarantorRequest {
  lastName: string;
  firstName: string;
  idCardNumber: string;
  phone: string;
  address: string;
  relationshipToTenant: string;
}

export interface TenantApplicationRequest {
  guarantors: GuarantorRequest[];
}

export interface GuarantorResponse {
  id: number;
  lastName: string;
  firstName: string;
  idCardNumber: string;
  phone: string;
  address: string;
  relationshipToTenant: string;
}

export interface TenantDocumentResponse {
  id: number;
  type: string;
  fileName: string;
  storageUrl: string;
  uploadedAt: string;
}

export interface TenantApplicationResponse {
  id: number;
  bookingId: number;
  status: string;
  createdAt: string;
  decidedAt?: string;
  decisionComment?: string;
  documents: TenantDocumentResponse[];
  guarantors: GuarantorResponse[];
}

export interface ValidationDecisionRequest {
  status: 'APPROVED' | 'REJECTED';
  comment?: string;
}

@Injectable({
  providedIn: 'root',
})
export class RentalCycleService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  createApplication(
    bookingId: number,
    request: TenantApplicationRequest,
  ): Observable<TenantApplicationResponse> {
    return this.http.post<TenantApplicationResponse>(
      `${this.apiUrl}/bookings/${bookingId}/application`,
      request,
    );
  }

  uploadDocument(bookingId: number, file: File, type: string): Observable<TenantDocumentResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);

    return this.http.post<TenantDocumentResponse>(
      `${this.apiUrl}/bookings/${bookingId}/application/documents`,
      formData,
    );
  }

  submitApplication(bookingId: number): Observable<TenantApplicationResponse> {
    return this.http.post<TenantApplicationResponse>(
      `${this.apiUrl}/bookings/${bookingId}/application/submit`,
      {},
    );
  }

  validateApplication(
    bookingId: number,
    decision: ValidationDecisionRequest,
  ): Observable<TenantApplicationResponse> {
    return this.http.patch<TenantApplicationResponse>(
      `${this.apiUrl}/bookings/${bookingId}/application/validate`,
      decision,
    );
  }
}
