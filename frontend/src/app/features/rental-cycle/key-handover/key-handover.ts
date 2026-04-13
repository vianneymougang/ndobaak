import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  Signal,
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
import { TimelineModule } from 'primeng/timeline';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../../core/services/auth';
import { KeyHandoverResponse, RentalCycleService } from '../../../core/services/rental-cycle';

interface TimelineEvent {
  label: string;
  date?: string;
  done: boolean;
  icon: string;
}

@Component({
  selector: 'app-key-handover',
  templateUrl: './key-handover.html',
  styleUrl: './key-handover.scss',
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
    TimelineModule,
  ],
  providers: [MessageService],
})
export class KeyHandover implements OnInit {
  private route = inject(ActivatedRoute);
  private rentalCycleService = inject(RentalCycleService);
  private messageService = inject(MessageService);
  protected authService = inject(AuthService);

  protected bookingId = signal<number>(0);
  protected handover = signal<KeyHandoverResponse | null>(null);
  protected loading = signal(true);
  protected confirming = signal(false);
  protected notFound = signal(false);

  protected readonly userRole = this.authService.userRole;

  protected canConfirm = computed(() => {
    const h = this.handover();
    const role = this.userRole();
    if (!h) return false;
    if (role === 'LANDLORD') return !h.landlordConfirmed;
    if (role === 'STUDENT') return !h.tenantConfirmed;
    return false;
  });

  protected timelineEvents = computed<TimelineEvent[]>(() => {
    const h = this.handover();
    if (!h) return [];
    return [
      {
        label: 'Remise des clés planifiée',
        date: h.handoverDate,
        done: true,
        icon: 'pi pi-calendar',
      },
      {
        label: 'Confirmation propriétaire',
        date: h.landlordConfirmedAt,
        done: h.landlordConfirmed,
        icon: 'pi pi-home',
      },
      {
        label: 'Confirmation locataire',
        date: h.tenantConfirmedAt,
        done: h.tenantConfirmed,
        icon: 'pi pi-user',
      },
      {
        label: 'Virement effectué',
        date: h.transferDate,
        done: h.transferInitiated,
        icon: 'pi pi-credit-card',
      },
    ];
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('bookingId'));
    this.bookingId.set(id);
    this.loadHandover();
  }

  protected loadHandover(): void {
    this.loading.set(true);
    this.rentalCycleService.getKeyHandover(this.bookingId()).subscribe({
      next: (data) => {
        this.handover.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        if (err?.status === 404) {
          this.notFound.set(true);
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: "Impossible de charger la remise des clés. Vérifiez votre connexion et réessayez.",
          });
        }
        this.loading.set(false);
      },
    });
  }

  protected confirmHandover(): void {
    this.confirming.set(true);
    this.rentalCycleService.confirmKeyHandover(this.bookingId()).subscribe({
      next: (updated) => {
        this.handover.set(updated);
        this.messageService.add({
          severity: 'success',
          summary: 'Confirmé',
          detail: "Votre confirmation a été enregistrée avec succès.",
        });
        this.confirming.set(false);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: err?.error?.message || "Une erreur est survenue lors de la confirmation.",
        });
        this.confirming.set(false);
      },
    });
  }
}
