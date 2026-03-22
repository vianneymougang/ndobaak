import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Observable, of, Subject } from 'rxjs';

export interface UserPosition {
  latitude: number;
  longitude: number;
}

@Injectable({
  providedIn: 'root',
})
export class GeolocationService {
  private static readonly SESSION_KEY = 'ndb_user_position';
  private static readonly MAX_AGE_MS = 300_000;
  private static readonly TIMEOUT_MS = 10_000;

  private platformId = inject(PLATFORM_ID);

  readonly position = signal<UserPosition | null>(this.loadFromSession());
  readonly denied = signal(false);
  readonly loading = signal(false);

  requestPosition(): Observable<UserPosition | null> {
    if (!isPlatformBrowser(this.platformId)) {
      return of(null);
    }

    const cached = this.loadFromSession();
    if (cached) {
      this.position.set(cached);
      return of(cached);
    }

    if (!('geolocation' in navigator)) {
      this.denied.set(true);
      return of(null);
    }

    this.loading.set(true);
    const result$ = new Subject<UserPosition | null>();

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const userPos: UserPosition = {
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
        };
        this.position.set(userPos);
        this.denied.set(false);
        this.loading.set(false);
        this.saveToSession(userPos);
        result$.next(userPos);
        result$.complete();
      },
      () => {
        this.position.set(null);
        this.denied.set(true);
        this.loading.set(false);
        result$.next(null);
        result$.complete();
      },
      {
        enableHighAccuracy: false,
        maximumAge: GeolocationService.MAX_AGE_MS,
        timeout: GeolocationService.TIMEOUT_MS,
      }
    );

    return result$.asObservable();
  }

  private loadFromSession(): UserPosition | null {
    try {
      const raw = sessionStorage.getItem(GeolocationService.SESSION_KEY);
      if (!raw) return null;
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed.latitude === 'number' && typeof parsed.longitude === 'number') {
        return parsed as UserPosition;
      }
    } catch {
      // ignore
    }
    return null;
  }

  private saveToSession(pos: UserPosition): void {
    try {
      sessionStorage.setItem(GeolocationService.SESSION_KEY, JSON.stringify(pos));
    } catch {
      // ignore
    }
  }
}
