import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateListingRequest,
  ListingResponse,
  ListingSearchResponse,
  SearchPageResponse,
} from '../models/listing.models';

@Injectable({
  providedIn: 'root',
})
export class ListingService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  createListing(request: CreateListingRequest): Observable<ListingResponse> {
    return this.http.post<ListingResponse>(`${this.apiUrl}/listings`, request);
  }

  updateListing(id: number, request: CreateListingRequest): Observable<ListingResponse> {
    return this.http.put<ListingResponse>(`${this.apiUrl}/listings/${id}`, request);
  }

  uploadPhoto(listingId: number, file: File): Observable<ListingResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ListingResponse>(`${this.apiUrl}/listings/${listingId}/photos`, formData);
  }

  deletePhoto(listingId: number, photoId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/listings/${listingId}/photos/${photoId}`);
  }

  submitForReview(id: number): Observable<ListingResponse> {
    return this.http.put<ListingResponse>(`${this.apiUrl}/listings/${id}/submit`, {});
  }

  getMyListings(): Observable<ListingResponse[]> {
    return this.http.get<ListingResponse[]>(`${this.apiUrl}/listings/my`);
  }

  getListingById(id: number): Observable<ListingResponse> {
    return this.http.get<ListingResponse>(`${this.apiUrl}/listings/${id}`);
  }

  search(filters: Record<string, unknown>): Observable<SearchPageResponse<ListingSearchResponse>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== null && value !== undefined && value !== '') {
        if (Array.isArray(value)) {
          params = params.set(key, value.join(','));
        } else {
          params = params.set(key, String(value));
        }
      }
    }
    return this.http.get<SearchPageResponse<ListingSearchResponse>>(
      `${this.apiUrl}/listings/search`,
      { params }
    );
  }
}
