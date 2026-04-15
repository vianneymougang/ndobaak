import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { DividerModule } from 'primeng/divider';
import { AccordionModule } from 'primeng/accordion';
import { GalleriaModule } from 'primeng/galleria';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../../core/services/auth';
import {
  PropertyInspectionResponse,
  InspectionItemResponse,
  RentalCycleService,
} from '../../../core/services/rental-cycle';

@Component({
  selector: 'app-inspection-viewer',
  templateUrl: './inspection-viewer.html',
  styleUrl: './inspection-viewer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    CardModule,
    TagModule,
    ButtonModule,
    SkeletonModule,
    MessageModule,
    ToastModule,
    DividerModule,
    AccordionModule,
    GalleriaModule,
  ],
  providers: [MessageService],
})
export class InspectionViewer implements OnInit {
  private route = inject(ActivatedRoute);
  private rentalCycleService = inject(RentalCycleService);
  private messageService = inject(MessageService);
  protected authService = inject(AuthService);

  protected bookingId = signal<number>(0);
  protected inspections = signal<PropertyInspectionResponse[]>([]);
  protected loading = signal(true);
  protected signing = signal(false);

  protected readonly userRole = this.authService.userRole;

  protected readonly statusLabels: Record<string, string> = {
    IN_PROGRESS: 'En cours',
    COMPLETED: 'Terminé',
    LANDLORD_SIGNED: 'Signé par le propriétaire',
    FULLY_SIGNED: 'Signé par les deux parties',
  };

  protected readonly statusSeverity: Record<string, 'success' | 'info' | 'warn' | 'danger'> = {
    IN_PROGRESS: 'warn',
    COMPLETED: 'info',
    LANDLORD_SIGNED: 'info',
    FULLY_SIGNED: 'success',
  };

  protected readonly conditionLabels: Record<string, string> = {
    GOOD: 'Bon état',
    NORMAL_WEAR: 'Usure normale',
    DAMAGED: 'Endommagé',
    MISSING: 'Manquant',
  };

  protected readonly conditionSeverity: Record<string, 'success' | 'info' | 'warn' | 'danger'> = {
    GOOD: 'success',
    NORMAL_WEAR: 'info',
    DAMAGED: 'danger',
    MISSING: 'danger',
  };

  protected readonly typeLabels: Record<string, string> = {
    MOVE_IN: "État des lieux d'entrée",
    MOVE_OUT: "État des lieux de sortie",
  };

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('bookingId'));
    this.bookingId.set(id);
    this.loadInspections();
  }

  protected loadInspections(): void {
    this.loading.set(true);
    this.rentalCycleService.getInspections(this.bookingId()).subscribe({
      next: (data) => {
        this.inspections.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: "Impossible de charger les états des lieux. Vérifiez votre connexion et réessayez.",
        });
        this.loading.set(false);
      },
    });
  }

  protected canSign(inspection: PropertyInspectionResponse): boolean {
    const role = this.userRole();
    if (inspection.status === 'FULLY_SIGNED') return false;
    if (role === 'LANDLORD') return !inspection.landlordSigned;
    if (role === 'STUDENT') return inspection.landlordSigned && !inspection.tenantSigned;
    return false;
  }

  protected signInspection(inspection: PropertyInspectionResponse): void {
    this.signing.set(true);
    this.rentalCycleService.signInspection(this.bookingId(), inspection.id).subscribe({
      next: (updated) => {
        this.inspections.update((list) =>
          list.map((i) => (i.id === updated.id ? updated : i)),
        );
        this.messageService.add({
          severity: 'success',
          summary: 'Signé',
          detail: "Votre signature a été enregistrée avec succès.",
        });
        this.signing.set(false);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: err?.error?.message || "Une erreur est survenue lors de la signature.",
        });
        this.signing.set(false);
      },
    });
  }

  protected getPhotosForItem(
    inspection: PropertyInspectionResponse,
    item: InspectionItemResponse,
  ) {
    return inspection.photos
      .filter((p) => p.itemId === item.id)
      .map((p) => ({
        itemImageSrc: p.storageUrl,
        thumbnailImageSrc: p.storageUrl,
        alt: p.caption || '',
      }));
  }

  protected getGeneralPhotos(inspection: PropertyInspectionResponse) {
    return inspection.photos
      .filter((p) => !p.itemId)
      .map((p) => ({
        itemImageSrc: p.storageUrl,
        thumbnailImageSrc: p.storageUrl,
        alt: p.caption || '',
      }));
  }
}
