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
  createdAt: string;
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
  startDate?: string;
  endDate?: string;
  securityDeposit?: number;
  specialClauses?: string;
}

export interface LandlordDocumentResponse {
  id: number;
  landlordId: number;
  type: string;
  fileName: string;
  storageUrl: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  validatedById?: number;
  validatedAt?: string;
  rejectionComment?: string;
  createdAt: string;
}

export type LeaseStatus =
  | 'DRAFT'
  | 'AWAITING_LANDLORD_SIGNATURE'
  | 'AWAITING_TENANT_SIGNATURE'
  | 'FULLY_SIGNED'
  | 'TAX_REGISTERED'
  | 'TERMINATED';

export interface LeaseContractResponse {
  id: number;
  bookingId: number;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  securityDeposit: number;
  agencyFee: number;
  specialClauses?: string;
  pdfUrl?: string;
  status: LeaseStatus;
  landlordSignedAt?: string;
  tenantSignedAt?: string;
  taxRegistrationDate?: string;
  taxReference?: string;
  generatedById: number;
  createdAt: string;
}

export type InspectionType = 'MOVE_IN' | 'MOVE_OUT';
export type InspectionStatus = 'IN_PROGRESS' | 'COMPLETED' | 'LANDLORD_SIGNED' | 'FULLY_SIGNED';
export type ElementCondition = 'GOOD' | 'NORMAL_WEAR' | 'DAMAGED' | 'MISSING';

export interface InspectionRequest {
  type: InspectionType;
  conductedAt: string;
  observations?: string;
}

export interface InspectionItemRequest {
  location: string;
  element: string;
  condition: ElementCondition;
  observations?: string;
}

export interface KeyHandoverRequest {
  handoverDate: string;
  observations?: string;
}

export interface InspectionItemResponse {
  id: number;
  location: string;
  element: string;
  condition: ElementCondition;
  observations?: string;
  createdAt: string;
}

export interface InspectionPhotoResponse {
  id: number;
  inspectionId: number;
  itemId?: number;
  storageUrl: string;
  caption?: string;
  createdAt: string;
}

export interface PropertyInspectionResponse {
  id: number;
  bookingId: number;
  type: InspectionType;
  conductedAt: string;
  agentId: number;
  status: InspectionStatus;
  landlordSigned: boolean;
  tenantSigned: boolean;
  landlordSignedAt?: string;
  tenantSignedAt?: string;
  observations?: string;
  items: InspectionItemResponse[];
  photos: InspectionPhotoResponse[];
  createdAt: string;
}

export interface KeyHandoverResponse {
  id: number;
  bookingId: number;
  moveInInspectionId: number;
  handoverDate: string;
  landlordConfirmed: boolean;
  tenantConfirmed: boolean;
  landlordConfirmedAt?: string;
  tenantConfirmedAt?: string;
  transferInitiated: boolean;
  transferDate?: string;
  observations?: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class RentalCycleService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  // ========== Landlord Documents ==========
  getLandlordDocuments(landlordId: number): Observable<LandlordDocumentResponse[]> {
    return this.http.get<LandlordDocumentResponse[]>(
      `${this.apiUrl}/landlords/${landlordId}/documents`,
    );
  }

  uploadLandlordDocument(
    landlordId: number,
    file: File,
    type: string,
  ): Observable<LandlordDocumentResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);

    return this.http.post<LandlordDocumentResponse>(
      `${this.apiUrl}/landlords/${landlordId}/documents`,
      formData,
    );
  }

  validateLandlordDocument(
    landlordId: number,
    docId: number,
    decision: ValidationDecisionRequest,
  ): Observable<LandlordDocumentResponse> {
    return this.http.patch<LandlordDocumentResponse>(
      `${this.apiUrl}/admin/landlords/${landlordId}/documents/${docId}/validate`,
      decision,
    );
  }

  // ========== Tenant Application ==========
  getApplication(bookingId: number): Observable<TenantApplicationResponse> {
    return this.http.get<TenantApplicationResponse>(
      `${this.apiUrl}/bookings/${bookingId}/application`,
    );
  }

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
      `${this.apiUrl}/admin/bookings/${bookingId}/application/validate`,
      decision,
    );
  }

  // ========== Lease Contract ==========
  getLease(bookingId: number): Observable<LeaseContractResponse> {
    return this.http.get<LeaseContractResponse>(
      `${this.apiUrl}/bookings/${bookingId}/lease`,
    );
  }

  getLeasePdfUrl(bookingId: number): string {
    return `${this.apiUrl}/bookings/${bookingId}/lease/pdf`;
  }

  signLease(bookingId: number): Observable<LeaseContractResponse> {
    return this.http.post<LeaseContractResponse>(
      `${this.apiUrl}/bookings/${bookingId}/lease/sign`,
      {},
    );
  }

  initiatePayment(bookingId: number): Observable<{ authorizationUrl: string; reference: string }> {
    return this.http.post<{ authorizationUrl: string; reference: string }>(
      `${this.apiUrl}/bookings/${bookingId}/payment/initiate`,
      {},
    );
  }

  // ========== Property Inspection ==========
  getInspections(bookingId: number): Observable<PropertyInspectionResponse[]> {
    return this.http.get<PropertyInspectionResponse[]>(
      `${this.apiUrl}/bookings/${bookingId}/inspection`,
    );
  }

  getInspection(bookingId: number, inspId: number): Observable<PropertyInspectionResponse> {
    return this.http.get<PropertyInspectionResponse>(
      `${this.apiUrl}/bookings/${bookingId}/inspection/${inspId}`,
    );
  }

  createInspection(bookingId: number, request: InspectionRequest): Observable<PropertyInspectionResponse> {
    return this.http.post<PropertyInspectionResponse>(
      `${this.apiUrl}/bookings/${bookingId}/inspection`,
      request,
    );
  }

  addInspectionItem(
    bookingId: number,
    inspId: number,
    request: InspectionItemRequest,
  ): Observable<InspectionItemResponse> {
    return this.http.post<InspectionItemResponse>(
      `${this.apiUrl}/bookings/${bookingId}/inspection/${inspId}/items`,
      request,
    );
  }

  uploadInspectionPhoto(
    bookingId: number,
    inspId: number,
    file: File,
    itemId?: number,
  ): Observable<InspectionPhotoResponse> {
    const formData = new FormData();
    formData.append('file', file);
    const url = itemId
      ? `${this.apiUrl}/bookings/${bookingId}/inspection/${inspId}/photos?itemId=${itemId}`
      : `${this.apiUrl}/bookings/${bookingId}/inspection/${inspId}/photos`;
    return this.http.post<InspectionPhotoResponse>(url, formData);
  }

  signInspection(bookingId: number, inspId: number): Observable<PropertyInspectionResponse> {
    return this.http.post<PropertyInspectionResponse>(
      `${this.apiUrl}/bookings/${bookingId}/inspection/${inspId}/sign`,
      {},
    );
  }

  // ========== Key Handover ==========
  getKeyHandover(bookingId: number): Observable<KeyHandoverResponse> {
    return this.http.get<KeyHandoverResponse>(
      `${this.apiUrl}/bookings/${bookingId}/key-handover`,
    );
  }

  createKeyHandover(bookingId: number, request: KeyHandoverRequest): Observable<KeyHandoverResponse> {
    return this.http.post<KeyHandoverResponse>(
      `${this.apiUrl}/bookings/${bookingId}/key-handover`,
      request,
    );
  }

  confirmKeyHandover(bookingId: number): Observable<KeyHandoverResponse> {
    return this.http.post<KeyHandoverResponse>(
      `${this.apiUrl}/bookings/${bookingId}/key-handover/confirm`,
      {},
    );
  }

  retryTransfer(bookingId: number): Observable<KeyHandoverResponse> {
    return this.http.post<KeyHandoverResponse>(
      `${this.apiUrl}/admin/bookings/${bookingId}/retry-transfer`,
      {},
    );
  }
}
