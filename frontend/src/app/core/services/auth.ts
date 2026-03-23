import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, switchMap } from 'rxjs';
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

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => !!this.currentUser());
  readonly userRole = computed(() => this.currentUser()?.role ?? null);

  registerStudent(request: RegisterStudentRequest): Observable<UserProfile> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/auth/register/student`, request, { withCredentials: true })
      .pipe(switchMap(() => this.loadCurrentUser()));
  }

  registerLandlord(request: RegisterLandlordRequest): Observable<UserProfile> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/auth/register/landlord`, request, { withCredentials: true })
      .pipe(switchMap(() => this.loadCurrentUser()));
  }

  login(request: LoginRequest): Observable<UserProfile> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/auth/login`, request, { withCredentials: true })
      .pipe(switchMap(() => this.loadCurrentUser()));
  }

  loadCurrentUser(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/auth/me`, { withCredentials: true }).pipe(
      tap((user) => this.currentUser.set(user)),
      catchError((err) => {
        this.currentUser.set(null);
        throw err;
      }),
    );
  }

  handleOAuthCallback(): Observable<UserProfile> {
    return this.loadCurrentUser();
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/auth/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/auth/reset-password`, { token, newPassword });
  }

  logout(): void {
    this.http.post(`${this.apiUrl}/auth/logout`, {}, { withCredentials: true }).subscribe({
      complete: () => {
        this.currentUser.set(null);
        this.router.navigate(['/']);
      },
      error: () => {
        this.currentUser.set(null);
        this.router.navigate(['/']);
      },
    });
  }
}
