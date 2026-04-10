import { ChangeDetectionStrategy, Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { FloatLabelModule } from 'primeng/floatlabel';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { AuthService } from '../../../core/services/auth';

function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('newPassword')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordsMismatch: true } : null;
}

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink, PasswordModule, ButtonModule, FloatLabelModule, MessageModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.scss',
})
export class ResetPassword implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);

  protected loading = signal(false);
  protected success = signal(false);
  protected errorMessage = signal<string | null>(null);
  protected token = signal<string | null>(null);

  protected form = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatchValidator }
  );

  ngOnInit(): void {
    const t = this.route.snapshot.queryParamMap.get('token');
    if (!t) {
      this.router.navigate(['/login']);
      return;
    }
    this.token.set(t);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const t = this.token();
    if (!t) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    const { newPassword } = this.form.getRawValue();
    this.authService.resetPassword(t, newPassword).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading.set(false);
        this.errorMessage.set(
          err?.error?.message ?? 'Ce lien est invalide ou a expiré. Veuillez en demander un nouveau.'
        );
      },
    });
  }

  protected get passwordsMismatch(): boolean {
    return this.form.hasError('passwordsMismatch') && !!this.form.get('confirmPassword')?.touched;
  }
}
