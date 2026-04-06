import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { FileUploadModule } from 'primeng/fileupload';
import { MessageService } from 'primeng/api';
import { RentalCycleService, TenantApplicationResponse } from '../../../core/services/rental-cycle';

@Component({
  selector: 'app-tenant-application',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    CardModule,
    InputTextModule,
    FileUploadModule,
  ],
  templateUrl: './tenant-application.html',
  styleUrl: './tenant-application.scss',
})
export class TenantApplication implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private rentalCycleService = inject(RentalCycleService);
  private messageService = inject(MessageService);

  protected bookingId = signal<number>(0);
  protected application = signal<TenantApplicationResponse | null>(null);
  protected loading = signal(false);
  protected submitting = signal(false);

  protected applicationForm: FormGroup;

  constructor() {
    this.applicationForm = this.fb.group({
      guarantors: this.fb.array([this.createGuarantorForm()]),
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('bookingId');
    if (id) {
      this.bookingId.set(+id);
    }
  }

  private createGuarantorForm(): FormGroup {
    return this.fb.group({
      lastName: ['', Validators.required],
      firstName: ['', Validators.required],
      idCardNumber: ['', Validators.required],
      phone: ['', [Validators.required, Validators.pattern(/^\+237[26][0-9]{8}$/)]],
      address: ['', Validators.required],
      relationshipToTenant: ['', Validators.required],
    });
  }

  get guarantors(): FormArray {
    return this.applicationForm.get('guarantors') as FormArray;
  }

  protected addGuarantor(): void {
    this.guarantors.push(this.createGuarantorForm());
  }

  protected removeGuarantor(index: number): void {
    if (this.guarantors.length > 1) {
      this.guarantors.removeAt(index);
    }
  }

  protected onSubmitApplication(): void {
    if (this.applicationForm.invalid) {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs requis',
      });
      return;
    }

    this.submitting.set(true);
    this.rentalCycleService.createApplication(this.bookingId(), this.applicationForm.value).subscribe({
      next: (response) => {
        this.application.set(response);
        this.submitting.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Dossier créé avec succès',
        });
      },
      error: () => {
        this.submitting.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors de la création du dossier',
        });
      },
    });
  }

  protected onUploadDocument(event: any, type: string): void {
    const file = event.files[0];
    if (!file) return;

    this.rentalCycleService.uploadDocument(this.bookingId(), file, type).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Document uploadé avec succès',
        });
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: "Erreur lors de l'upload du document",
        });
      },
    });
  }

  protected onSubmitForValidation(): void {
    this.submitting.set(true);
    this.rentalCycleService.submitApplication(this.bookingId()).subscribe({
      next: (response) => {
        this.application.set(response);
        this.submitting.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Dossier soumis pour validation',
        });
      },
      error: () => {
        this.submitting.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors de la soumission',
        });
      },
    });
  }
}
