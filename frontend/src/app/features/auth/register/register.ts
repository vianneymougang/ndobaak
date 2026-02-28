import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';
import { FloatLabelModule } from 'primeng/floatlabel';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { SelectButtonModule } from 'primeng/selectbutton';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth';

function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');
  if (password && confirmPassword && password.value !== confirmPassword.value) {
    return { passwordMismatch: true };
  }
  return null;
}

@Component({
  selector: 'app-register',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    SelectButtonModule,
    MessageModule,
    FloatLabelModule,
    AutoFocusModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  protected roleOptions = [
    { label: 'Etudiant', value: 'STUDENT' },
    { label: 'Proprietaire', value: 'LANDLORD' },
  ];

  protected selectedRole = signal<string>('STUDENT');
  protected loading = signal(false);
  protected errorMessage = signal<string | null>(null);

  protected form = this.fb.nonNullable.group(
    {
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.minLength(9)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatchValidator },
  );

  ngOnInit(): void {
    const oauthError = this.route.snapshot.queryParamMap.get('oauthError');
    if (oauthError) {
      this.errorMessage.set('La connexion avec Google a échoué. Veuillez réessayer.');
    }
  }

  protected onRoleChange(role: string): void {
    this.selectedRole.set(role);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    const { firstName, lastName, email, phone, password } = this.form.getRawValue();

    if (this.selectedRole() === 'STUDENT') {
      this.authService.registerStudent({ firstName, lastName, email, phone, password }).subscribe({
        next: () => this.onSuccess(),
        error: (err) => this.onError(err),
      });
    } else {
      this.authService.registerLandlord({ firstName, lastName, email, phone, password }).subscribe({
        next: () => this.onSuccess(),
        error: (err) => this.onError(err),
      });
    }
  }

  private onSuccess(): void {
    this.loading.set(false);
    this.authService.loadCurrentUser().subscribe();
    this.router.navigate(['/']);
  }

  protected onGoogleLogin(): void {
    window.location.href = `${environment.apiUrl}/oauth2/authorization/google?from=register`;
  }

  private onError(err: unknown): void {
    this.loading.set(false);
    const error = err as { error?: { message?: string } };
    this.errorMessage.set(error?.error?.message ?? "Erreur lors de l'inscription");
  }
}
