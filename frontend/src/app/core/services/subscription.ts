import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Subscription, SubscribeRequest, RenewRequest } from '../models/subscription.models';

@Injectable({
  providedIn: 'root',
})
export class SubscriptionService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  subscribe(request: SubscribeRequest): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.apiUrl}/subscriptions`, request);
  }

  getMySubscription(): Observable<Subscription | null> {
    return this.http.get<Subscription>(`${this.apiUrl}/subscriptions/my`).pipe(
      catchError((err) => {
        if (err.status === 404) return of(null);
        throw err;
      }),
    );
  }

  renew(request: RenewRequest): Observable<Subscription> {
    return this.http.put<Subscription>(`${this.apiUrl}/subscriptions/renew`, request);
  }

  cancelAutoRenew(): Observable<Subscription> {
    return this.http.put<Subscription>(`${this.apiUrl}/subscriptions/cancel-auto-renew`, {});
  }

  cancelPendingSubscription(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/subscriptions/pending`);
  }
}
