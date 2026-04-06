import { ChangeDetectionStrategy, Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';
import { FloatLabelModule } from 'primeng/floatlabel';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-login',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    FloatLabelModule,
    AutoFocusModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);

  protected activeTab = signal<string>('email');
  protected loading = signal(false);
  protected errorMessage = signal<string | null>(null);

  protected emailForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected phoneForm = this.fb.nonNullable.group({
    phone: ['', [Validators.required, Validators.minLength(9)]],
    password: ['', [Validators.required]],
  });

  ngOnInit(): void {
    const oauthError = this.route.snapshot.queryParamMap.get('oauthError');
    if (oauthError) {
      this.errorMessage.set('La connexion avec Google a échoué. Veuillez réessayer.');
    }
  }

  protected onTabChange(tab: string | null): void {
    if (tab) {
      this.activeTab.set(tab);
      this.errorMessage.set(null);
    }
  }

  protected onEmailLogin(): void {
    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      this.scrollToFirstError();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    const { email, password } = this.emailForm.getRawValue();

    this.authService.login({ email, password }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.onSuccess(),
      error: (err) => this.onError(err),
    });
  }

  protected onPhoneLogin(): void {
    if (this.phoneForm.invalid) {
      this.phoneForm.markAllAsTouched();
      this.scrollToFirstError();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    const { phone, password } = this.phoneForm.getRawValue();

    this.authService.login({ phone, password }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.onSuccess(),
      error: (err) => this.onError(err),
    });
  }

  private onSuccess(): void {
    this.loading.set(false);
    const role = this.authService.userRole();
    if (role === 'ADMIN') {
      this.router.navigate(['/admin']);
    } else if (role === 'LANDLORD') {
      this.router.navigate(['/landlord']);
    } else {
      this.router.navigate(['/']);
    }
  }

  protected onGoogleLogin(): void {
    window.location.href = `${environment.apiUrl}/oauth2/authorization/google?from=login`;
  }

  private scrollToFirstError(): void {
    setTimeout(() => {
      document.querySelector('.ng-invalid:not(form)')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 50);
  }

  private onError(err: unknown): void {
    this.loading.set(false);
    const error = err as { error?: { message?: string } };
    this.errorMessage.set(error?.error?.message ?? 'Email/telephone ou mot de passe incorrect');
  }
}
