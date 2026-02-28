import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  RegisterStudentRequest,
  RegisterLandlordRequest,
  UserProfile,
} from '../models/auth.models';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = environment.apiUrl;

  private currentUser = signal<UserProfile | null>(null);
  private tokenSignal = signal<string | null>(this.getStoredToken());

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => !!this.tokenSignal());
  readonly userRole = computed(() => this.currentUser()?.role ?? null);

  get token(): string | null {
    return this.tokenSignal();
  }

  registerStudent(request: RegisterStudentRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/auth/register/student`, request)
      .pipe(tap((response) => this.handleAuthResponse(response)));
  }

  registerLandlord(request: RegisterLandlordRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/auth/register/landlord`, request)
      .pipe(tap((response) => this.handleAuthResponse(response)));
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/auth/login`, request)
      .pipe(tap((response) => this.handleAuthResponse(response)));
  }

  loadCurrentUser(): Observable<UserProfile> {
    return this.http
      .get<UserProfile>(`${this.apiUrl}/auth/me`)
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  handleOAuthCallback(token: string, refreshToken: string): Observable<UserProfile> {
    localStorage.setItem('access_token', token);
    localStorage.setItem('refresh_token', refreshToken);
    this.tokenSignal.set(token);
    return this.loadCurrentUser();
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/auth/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/auth/reset-password`, { token, newPassword });
  }

  logout(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    this.tokenSignal.set(null);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  private handleAuthResponse(response: AuthResponse): void {
    localStorage.setItem('access_token', response.accessToken);
    localStorage.setItem('refresh_token', response.refreshToken);
    this.tokenSignal.set(response.accessToken);
  }

  private getStoredToken(): string | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem('access_token');
  }
}
