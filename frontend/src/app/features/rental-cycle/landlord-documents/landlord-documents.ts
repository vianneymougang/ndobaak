import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RentalCycleService, LandlordDocumentResponse } from '../../../core/services/rental-cycle';
import { AuthService } from '../../../core/services/auth';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';

@Component({
  selector: 'app-landlord-documents',
  standalone: true,
  imports: [CommonModule, ToastModule, ButtonModule, SkeletonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
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
  uploadingType = signal<string | null>(null);
  dragActiveType = signal<string | null>(null);

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

  onDragOver(event: DragEvent, type: string) {
    event.preventDefault();
    this.dragActiveType.set(type);
  }

  onDragLeave() {
    this.dragActiveType.set(null);
  }

  onDrop(event: DragEvent, type: string) {
    event.preventDefault();
    this.dragActiveType.set(null);

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.uploadFile(files[0], type);
    }
  }

  onFileSelect(event: Event, type: string) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.uploadFile(input.files[0], type);
      input.value = '';
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

    this.uploadingType.set(type);
    this.rentalCycleService.uploadLandlordDocument(user.id, file, type).subscribe({
      next: (doc) => {
        this.documents.update((docs) => {
          const others = docs.filter(d => d.id !== doc.id);
          return [...others, doc];
        });
        this.messageService.add({
          severity: 'success',
          summary: 'Document envoyé',
          detail: 'Votre document est en cours de validation par notre équipe',
          life: 5000,
        });
        this.uploadingType.set(null);
      },
      error: (err) => {
        const detail = err.error?.message ?? 'Une erreur est survenue. Veuillez réessayer.';
        this.messageService.add({
          severity: 'error',
          summary: 'Échec de l\'envoi',
          detail,
          life: 6000,
        });
        this.uploadingType.set(null);
      },
    });
  }

  openDocument(url: string) {
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  getLatestDocumentByType(type: string): LandlordDocumentResponse | null {
    const docs = this.documents().filter(d => d.type === type);
    if (docs.length === 0) return null;
    return docs.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())[0];
  }

  canReupload(type: string): boolean {
    const doc = this.getLatestDocumentByType(type);
    return doc === null || doc.status === 'REJECTED';
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En attente de validation',
      APPROVED: 'Validé',
      REJECTED: 'Refusé',
    };
    return labels[status] ?? status;
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = {
      PENDING: 'status-pending',
      APPROVED: 'status-approved',
      REJECTED: 'status-rejected',
    };
    return classes[status] ?? '';
  }

  getFileIcon(fileName: string): string {
    const ext = fileName.split('.').pop()?.toLowerCase();
    if (ext === 'pdf') return 'pi-file-pdf';
    if (['jpg', 'jpeg', 'png', 'webp'].includes(ext ?? '')) return 'pi-image';
    return 'pi-file';
  }
}
