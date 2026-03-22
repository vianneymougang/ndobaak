import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth';
import type { UserProfile } from '../models/auth.models';

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  phone: string;
}

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private apiUrl = environment.apiUrl;

  updateProfile(data: UpdateProfileRequest): Observable<UserProfile> {
    return this.http
      .put<UserProfile>(`${this.apiUrl}/auth/profile`, data)
      .pipe(switchMap(() => this.authService.loadCurrentUser()));
  }

  uploadAvatar(file: File): Observable<UserProfile> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<{ url: string }>(`${this.apiUrl}/auth/profile/avatar`, formData)
      .pipe(switchMap(() => this.authService.loadCurrentUser()));
  }
}
