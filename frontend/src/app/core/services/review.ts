import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaginatedResponse } from '../models/booking.models';
import {
  CreateReviewRequest,
  RatingStatsResponse,
  ReviewResponse,
} from '../models/review.models';

@Injectable({
  providedIn: 'root',
})
export class ReviewService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  createReview(request: CreateReviewRequest, photos?: File[]): Observable<ReviewResponse> {
    const formData = new FormData();
    formData.append('review', new Blob([JSON.stringify(request)], { type: 'application/json' }));

    if (photos) {
      photos.forEach((photo) => formData.append('photos', photo));
    }

    return this.http.post<ReviewResponse>(`${this.apiUrl}/reviews`, formData);
  }

  getReviewsByListing(
    listingId: number,
    page = 0,
    size = 10,
    sort = 'createdAt,desc',
  ): Observable<PaginatedResponse<ReviewResponse>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort);
    return this.http.get<PaginatedResponse<ReviewResponse>>(
      `${this.apiUrl}/listings/${listingId}/reviews`,
      { params },
    );
  }

  getMyReviews(page = 0, size = 10): Observable<PaginatedResponse<ReviewResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PaginatedResponse<ReviewResponse>>(
      `${this.apiUrl}/reviews/my`,
      { params },
    );
  }

  deleteReview(reviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/reviews/${reviewId}`);
  }

  getRatingStats(listingId: number): Observable<RatingStatsResponse> {
    return this.http.get<RatingStatsResponse>(
      `${this.apiUrl}/listings/${listingId}/rating-stats`,
    );
  }
}
