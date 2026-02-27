import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { TabsModule } from 'primeng/tabs';
import { MessageModule } from 'primeng/message';
import { FloatLabelModule } from 'primeng/floatlabel';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    TabsModule,
    MessageModule,
    FloatLabelModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

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

  protected onTabChange(tab: string | number | undefined): void {
    this.activeTab.set(String(tab ?? 'email'));
    this.errorMessage.set(null);
  }

  protected onEmailLogin(): void {
    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    const { email, password } = this.emailForm.getRawValue();

    this.authService.login({ email, password }).subscribe({
      next: () => this.onSuccess(),
      error: (err) => this.onError(err),
    });
  }

  protected onPhoneLogin(): void {
    if (this.phoneForm.invalid) {
      this.phoneForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    const { phone, password } = this.phoneForm.getRawValue();

    this.authService.login({ phone, password }).subscribe({
      next: () => this.onSuccess(),
      error: (err) => this.onError(err),
    });
  }

  private onSuccess(): void {
    this.loading.set(false);
    this.authService.loadCurrentUser().subscribe();
    this.router.navigate(['/']);
  }

  private onError(err: unknown): void {
    this.loading.set(false);
    const error = err as { error?: { message?: string } };
    this.errorMessage.set(error?.error?.message ?? 'Email/telephone ou mot de passe incorrect');
  }
}
