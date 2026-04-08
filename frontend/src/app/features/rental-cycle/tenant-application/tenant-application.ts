import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';
import { RentalCycleService, TenantApplicationResponse } from '../../../core/services/rental-cycle';

@Component({
  selector: 'app-tenant-application',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    ToastModule,
    ProgressSpinnerModule,
  ],
  providers: [MessageService],
  changeDetection: ChangeDetectionStrategy.OnPush,
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
  protected uploading = signal(false);
  protected currentStep = signal<'form' | 'documents' | 'submitted'>('form');

  protected applicationForm: FormGroup = this.fb.group({
    guarantors: this.fb.array([this.createGuarantorForm()]),
  });

  protected readonly documentTypes = [
    { value: 'ID_CARD', label: 'Carte d\'identité nationale', icon: 'pi-id-card', required: true },
    { value: 'STUDENT_CARD', label: 'Carte étudiant', icon: 'pi-graduation-cap', required: true },
    { value: 'PROOF_OF_ADDRESS', label: 'Justificatif de domicile', icon: 'pi-home', required: false },
    { value: 'GUARANTOR_ID_CARD', label: 'CNI du garant', icon: 'pi-users', required: false },
  ];

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('bookingId'));
    this.bookingId.set(id);
    this.loadExistingApplication(id);
  }

  private loadExistingApplication(bookingId: number): void {
    this.loading.set(true);
    this.rentalCycleService.getApplication(bookingId).subscribe({
      next: (app) => {
        this.application.set(app);
        this.loading.set(false);
        
        if (app.status === 'INCOMPLETE') {
          this.currentStep.set('documents');
        } else if (app.status === 'SUBMITTED' || app.status === 'UNDER_REVIEW' || app.status === 'APPROVED' || app.status === 'REJECTED') {
          this.currentStep.set('submitted');
        }
      },
      error: () => {
        this.loading.set(false);
        this.currentStep.set('form');
      },
    });
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
    if (this.guarantors.length > 1) this.guarantors.removeAt(index);
  }

  protected onSubmitApplication(): void {
    if (this.applicationForm.invalid) {
      this.applicationForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Formulaire incomplet',
        detail: 'Veuillez remplir tous les champs obligatoires',
      });
      return;
    }
    
    this.submitting.set(true);
    this.rentalCycleService.createApplication(this.bookingId(), this.applicationForm.value).subscribe({
      next: (response) => {
        this.application.set(response);
        this.submitting.set(false);
        this.currentStep.set('documents');
        this.messageService.add({
          severity: 'success',
          summary: 'Dossier créé',
          detail: 'Vous pouvez maintenant uploader vos documents',
        });
      },
      error: () => {
        this.submitting.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Impossible de créer le dossier',
        });
      },
    });
  }

  protected onFileSelect(event: Event, type: string): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.uploadDocument(input.files[0], type);
    }
  }

  private uploadDocument(file: File, type: string): void {
    if (file.size > 10 * 1024 * 1024) {
      this.messageService.add({
        severity: 'error',
        summary: 'Fichier trop volumineux',
        detail: 'La taille maximale est de 10 Mo',
      });
      return;
    }

    this.uploading.set(true);
    this.rentalCycleService.uploadDocument(this.bookingId(), file, type).subscribe({
      next: (doc) => {
        const app = this.application();
        if (app) {
          this.application.set({
            ...app,
            documents: [...app.documents, doc],
          });
        }
        this.uploading.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Document ajouté',
          detail: 'Votre document a été uploadé avec succès',
        });
      },
      error: () => {
        this.uploading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Échec de l\'envoi',
          detail: 'Veuillez réessayer',
        });
      },
    });
  }

  protected onSubmitForValidation(): void {
    const app = this.application();
    if (!app) return;

    const hasIdCard = app.documents.some(d => d.type === 'ID_CARD');
    const hasStudentCard = app.documents.some(d => d.type === 'STUDENT_CARD');

    if (!hasIdCard || !hasStudentCard) {
      this.messageService.add({
        severity: 'error',
        summary: 'Documents manquants',
        detail: 'Vous devez uploader au minimum votre CNI et votre carte étudiant',
      });
      return;
    }

    this.submitting.set(true);
    this.rentalCycleService.submitApplication(this.bookingId()).subscribe({
      next: (response) => {
        this.application.set(response);
        this.submitting.set(false);
        this.currentStep.set('submitted');
        this.messageService.add({
          severity: 'success',
          summary: 'Dossier soumis',
          detail: 'Votre dossier est en cours de validation',
        });
      },
      error: () => {
        this.submitting.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Impossible de soumettre le dossier',
        });
      },
    });
  }

  protected hasDocument(type: string): boolean {
    return this.application()?.documents.some(d => d.type === type) || false;
  }

  protected getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      INCOMPLETE: 'Incomplet',
      SUBMITTED: 'Soumis',
      UNDER_REVIEW: 'En cours de validation',
      APPROVED: 'Approuvé',
      REJECTED: 'Refusé',
    };
    return labels[status] || status;
  }

  protected getStatusClass(status: string): string {
    const classes: Record<string, string> = {
      INCOMPLETE: 'status-incomplete',
      SUBMITTED: 'status-submitted',
      UNDER_REVIEW: 'status-review',
      APPROVED: 'status-approved',
      REJECTED: 'status-rejected',
    };
    return classes[status] || '';
  }

  protected goBack(): void {
    this.router.navigate(['/bookings']);
  }
}
