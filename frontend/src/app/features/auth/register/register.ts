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
import { InputGroupModule } from 'primeng/inputgroup';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
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

export interface Country {
  flag: string;
  name: string;
  dialCode: string;
}

export const COUNTRIES: Country[] = [
  { flag: '🇨🇲', name: 'Cameroun', dialCode: '+237' },
  { flag: '🇳🇬', name: 'Nigeria', dialCode: '+234' },
  { flag: '🇨🇮', name: "Côte d'Ivoire", dialCode: '+225' },
  { flag: '🇸🇳', name: 'Sénégal', dialCode: '+221' },
  { flag: '🇬🇳', name: 'Guinée', dialCode: '+224' },
  { flag: '🇬🇦', name: 'Gabon', dialCode: '+241' },
  { flag: '🇨🇬', name: 'Congo', dialCode: '+242' },
  { flag: '🇨🇩', name: 'RD Congo', dialCode: '+243' },
  { flag: '🇹🇩', name: 'Tchad', dialCode: '+235' },
  { flag: '🇬🇭', name: 'Ghana', dialCode: '+233' },
  { flag: '🇫🇷', name: 'France', dialCode: '+33' },
  { flag: '🇧🇪', name: 'Belgique', dialCode: '+32' },
  { flag: '🇨🇦', name: 'Canada', dialCode: '+1' },
  { flag: '🇺🇸', name: 'États-Unis', dialCode: '+1' },
  { flag: '🇬🇧', name: 'Royaume-Uni', dialCode: '+44' },
];

@Component({
  selector: 'app-register',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    InputTextModule,
    InputGroupModule,
    SelectModule,
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

  protected countries = COUNTRIES;
  protected selectedCountry = signal<Country>(COUNTRIES[0]);

  protected selectedRole = signal<string>('STUDENT');
  protected loading = signal(false);
  protected errorMessage = signal<string | null>(null);

  protected form = this.fb.nonNullable.group(
    {
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      localPhone: ['', [Validators.required, Validators.pattern(/^\d{7,12}$/)]],
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

  protected onCountryChange(country: Country): void {
    this.selectedCountry.set(country);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    const { firstName, lastName, email, localPhone, password } = this.form.getRawValue();
    const phone = this.selectedCountry().dialCode + localPhone;

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
