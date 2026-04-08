import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RentalCycleService, LandlordDocumentResponse } from '../../../core/services/rental-cycle';
import { AuthService } from '../../../core/services/auth';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-landlord-documents',
  standalone: true,
  imports: [CommonModule, ToastModule, ButtonModule, ProgressSpinnerModule],
  providers: [MessageService],
  templateUrl: './landlord-documents.html',
  styleUrl: './landlord-documents.scss',
})
export class LandlordDocuments implements OnInit {
  private rentalCycleService = inject(RentalCycleService);
  private authService = inject(AuthService);
  private messageService = inject(MessageService);

  documents = signal<LandlordDocumentResponse[]>([]);
  loading = signal(false);
  uploading = signal(false);
  dragActive = signal(false);

  readonly documentTypes = [
    { value: 'ID_CARD', label: 'Carte d\'identité nationale', icon: 'pi-id-card' },
    { value: 'BUILDING_PERMIT', label: 'Permis de construire', icon: 'pi-building' },
    { value: 'LAND_TITLE', label: 'Titre foncier', icon: 'pi-file-check' },
    { value: 'OTHER', label: 'Autre document', icon: 'pi-file' },
  ];

  ngOnInit() {
    this.loadDocuments();
  }

  loadDocuments() {
    const user = this.authService.user();
    if (!user) return;

    this.loading.set(true);
    this.rentalCycleService.getLandlordDocuments(user.id).subscribe({
      next: (docs) => {
        this.documents.set(docs);
        this.loading.set(false);
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Impossible de charger vos documents',
        });
        this.loading.set(false);
      },
    });
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.dragActive.set(true);
  }

  onDragLeave() {
    this.dragActive.set(false);
  }

  onDrop(event: DragEvent, type: string) {
    event.preventDefault();
    this.dragActive.set(false);

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.uploadFile(files[0], type);
    }
  }

  onFileSelect(event: Event, type: string) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.uploadFile(input.files[0], type);
    }
  }

  uploadFile(file: File, type: string) {
    const user = this.authService.user();
    if (!user) return;

    if (file.size > 10 * 1024 * 1024) {
      this.messageService.add({
        severity: 'error',
        summary: 'Fichier trop volumineux',
        detail: 'La taille maximale est de 10 Mo',
      });
      return;
    }

    this.uploading.set(true);
    this.rentalCycleService.uploadLandlordDocument(user.id, file, type).subscribe({
      next: (doc) => {
        this.documents.update((docs) => [...docs, doc]);
        this.messageService.add({
          severity: 'success',
          summary: 'Document envoyé',
          detail: 'Votre document est en cours de validation',
        });
        this.uploading.set(false);
      },
      error: (err) => {
        console.error('Upload error:', err);
        const errorMessage = err.error?.message || err.message || 'Veuillez réessayer';
        this.messageService.add({
          severity: 'error',
          summary: 'Échec de l\'envoi',
          detail: errorMessage,
          life: 6000,
        });
        this.uploading.set(false);
      },
    });
  }

  getDocumentsByType(type: string): LandlordDocumentResponse[] {
    return this.documents().filter((doc) => doc.type === type);
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En attente',
      APPROVED: 'Validé',
      REJECTED: 'Refusé',
    };
    return labels[status] || status;
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = {
      PENDING: 'status-pending',
      APPROVED: 'status-approved',
      REJECTED: 'status-rejected',
    };
    return classes[status] || '';
  }
}
