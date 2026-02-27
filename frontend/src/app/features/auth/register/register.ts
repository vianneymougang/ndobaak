import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageModule } from 'primeng/message';
import { FloatLabelModule } from 'primeng/floatlabel';
import { AuthService } from '../../../core/services/auth';

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
    InputNumberModule,
    MessageModule,
    FloatLabelModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  protected roleOptions = [
    { label: 'Etudiant', value: 'STUDENT' },
    { label: 'Proprietaire', value: 'LANDLORD' },
  ];

  protected selectedRole = signal<string>('STUDENT');
  protected loading = signal(false);
  protected errorMessage = signal<string | null>(null);

  protected form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.minLength(9)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    matricule: [''],
    fieldOfStudy: [''],
    yearOfStudy: [null as number | null],
    idCardNumber: [''],
    taxId: [''],
  });

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
    const formValue = this.form.getRawValue();

    if (this.selectedRole() === 'STUDENT') {
      this.authService
        .registerStudent({
          firstName: formValue.firstName,
          lastName: formValue.lastName,
          email: formValue.email,
          phone: formValue.phone,
          password: formValue.password,
          matricule: formValue.matricule || undefined,
          fieldOfStudy: formValue.fieldOfStudy || undefined,
          yearOfStudy: formValue.yearOfStudy || undefined,
        })
        .subscribe({
          next: () => this.onSuccess(),
          error: (err) => this.onError(err),
        });
    } else {
      this.authService
        .registerLandlord({
          firstName: formValue.firstName,
          lastName: formValue.lastName,
          email: formValue.email,
          phone: formValue.phone,
          password: formValue.password,
          idCardNumber: formValue.idCardNumber || undefined,
          taxId: formValue.taxId || undefined,
        })
        .subscribe({
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

  private onError(err: unknown): void {
    this.loading.set(false);
    const error = err as { error?: { message?: string } };
    this.errorMessage.set(error?.error?.message ?? "Erreur lors de l'inscription");
  }
}
