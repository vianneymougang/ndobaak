import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  effect,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TabsModule } from 'primeng/tabs';
import { PaginatorModule } from 'primeng/paginator';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { DialogModule } from 'primeng/dialog';
import { TextareaModule } from 'primeng/textarea';
import { FloatLabelModule } from 'primeng/floatlabel';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { MessageService } from 'primeng/api';
import { BookingCard } from '../../../shared/components/booking-card/booking-card';
import { BookingService } from '../../../core/services/booking';
import { BookingResponse, BookingStatus } from '../../../core/models/booking.models';

@Component({
  selector: 'app-landlord-bookings',
  imports: [
    FormsModule,
    TabsModule,
    PaginatorModule,
    ButtonModule,
    SkeletonModule,
    DialogModule,
    TextareaModule,
    FloatLabelModule,
    NgmMotionDirective,
    BookingCard,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './landlord-bookings.html',
  styleUrl: './landlord-bookings.scss',
})
export class LandlordBookings {
  private bookingService = inject(BookingService);
  private messageService = inject(MessageService);

  protected activeTab = signal(1);
  protected bookings = signal<BookingResponse[]>([]);
  protected loading = signal(true);
  protected page = signal(0);
  protected totalElements = signal(0);
  protected pageSize = 10;

  protected rejectDialogVisible = signal(false);
  protected rejectingId = signal<number | null>(null);
  protected rejectReason = signal('');
  protected rejecting = signal(false);

  protected cancelDialogVisible = signal(false);
  protected cancellingId = signal<number | null>(null);
  protected cancelling = signal(false);

  protected confirming = signal(false);

  readonly skeletonItems = Array(3).fill(0);

  private readonly tabStatuses: (BookingStatus | undefined)[] = [
    undefined,
    'PENDING',
    'CONFIRMED',
    'CANCELLED',
  ];

  protected emptyMessage = computed(() => {
    const messages = [
      'Aucune demande de réservation.',
      'Aucune demande en attente.',
      'Aucune réservation confirmée.',
      'Aucune réservation annulée.',
    ];
    return messages[this.activeTab()];
  });

  constructor() {
    effect(() => {
      const tab = this.activeTab();
      const p = this.page();
      this.performLoad(tab, p);
    });
  }

  private performLoad(tab: number, page: number): void {
    const status = this.tabStatuses[tab];
    this.loading.set(true);
    this.bookings.set([]);
    this.bookingService.getLandlordBookings(status, page, this.pageSize).subscribe({
      next: (res) => {
        this.bookings.set(res.content);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Impossible de charger les réservations.',
        });
      },
    });
  }

  protected onTabChange(value: string | number | undefined): void {
    this.page.set(0);
    this.activeTab.set(typeof value === 'number' ? value : Number(value ?? 0));
  }

  protected onPageChange(event: { page?: number }): void {
    this.page.set(event.page ?? 0);
  }

  protected staggerDelay(i: number): number {
    return Math.min(i, 10) * 0.06;
  }

  protected confirmBooking(id: number): void {
    this.confirming.set(true);
    this.bookingService.confirmBooking(id).subscribe({
      next: () => {
        this.confirming.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Réservation confirmée',
          detail: 'L\'étudiant a été notifié.',
        });
        this.performLoad(this.activeTab(), this.page());
      },
      error: (err) => {
        this.confirming.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: err?.error?.message ?? 'Impossible de confirmer cette réservation.',
        });
      },
    });
  }

  protected openRejectDialog(id: number): void {
    this.rejectingId.set(id);
    this.rejectReason.set('');
    this.rejectDialogVisible.set(true);
  }

  protected doReject(): void {
    const id = this.rejectingId();
    if (!id) return;
    this.rejecting.set(true);
    const reason = this.rejectReason().trim() || undefined;
    this.bookingService.rejectBooking(id, reason).subscribe({
      next: () => {
        this.rejecting.set(false);
        this.rejectDialogVisible.set(false);
        this.rejectingId.set(null);
        this.messageService.add({
          severity: 'success',
          summary: 'Demande refusée',
          detail: 'L\'étudiant a été informé du refus.',
        });
        this.performLoad(this.activeTab(), this.page());
      },
      error: (err) => {
        this.rejecting.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: err?.error?.message ?? 'Impossible de refuser cette réservation.',
        });
      },
    });
  }

  protected openCancelDialog(id: number): void {
    this.cancellingId.set(id);
    this.cancelDialogVisible.set(true);
  }

  protected doCancel(): void {
    const id = this.cancellingId();
    if (!id) return;
    this.cancelling.set(true);
    this.bookingService.cancelBooking(id).subscribe({
      next: () => {
        this.cancelling.set(false);
        this.cancelDialogVisible.set(false);
        this.cancellingId.set(null);
        this.messageService.add({
          severity: 'success',
          summary: 'Réservation annulée',
          detail: 'La réservation a été annulée.',
        });
        this.performLoad(this.activeTab(), this.page());
      },
      error: (err) => {
        this.cancelling.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: err?.error?.message ?? 'Impossible d\'annuler cette réservation.',
        });
      },
    });
  }
}
