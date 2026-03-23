import {
  Component,
  ChangeDetectionStrategy,
  DestroyRef,
  inject,
  signal,
  computed,
  effect,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TabsModule } from 'primeng/tabs';
import { PaginatorModule } from 'primeng/paginator';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { DialogModule } from 'primeng/dialog';
import { MessageService } from 'primeng/api';
import { BookingCard } from '../../../shared/components/booking-card/booking-card';
import { BookingService } from '../../../core/services/booking';
import { BookingResponse, BookingStatus } from '../../../core/models/booking.models';

@Component({
  selector: 'app-my-bookings',
  imports: [
    RouterLink,
    TabsModule,
    PaginatorModule,
    ButtonModule,
    SkeletonModule,
    DialogModule,
    BookingCard,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './my-bookings.html',
  styleUrl: './my-bookings.scss',
})
export class MyBookings {
  private bookingService = inject(BookingService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);

  protected activeTab = signal(0);
  protected bookings = signal<BookingResponse[]>([]);
  protected loading = signal(true);
  protected page = signal(0);
  protected totalElements = signal(0);
  protected pageSize = 10;

  protected cancelDialogVisible = signal(false);
  protected cancellingId = signal<number | null>(null);
  protected cancelling = signal(false);

  readonly skeletonItems = Array(3).fill(0);

  private readonly tabStatuses: (BookingStatus | undefined)[] = [
    undefined,
    'PENDING',
    'CONFIRMED',
    'CANCELLED',
  ];

  private readonly emptyMessages = [
    'Vous n\'avez pas encore de réservation. Trouvez votre prochain logement !',
    'Aucune réservation en attente.',
    'Aucune réservation confirmée pour le moment.',
    'Aucune réservation annulée.',
  ];

  protected emptyMessage = computed(() => this.emptyMessages[this.activeTab()]);
  protected showSearchCta = computed(() => this.activeTab() === 0);

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
    this.bookingService.getMyBookings(status, page, this.pageSize).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
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
          detail: 'Impossible de charger vos réservations.',
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

  protected openCancelDialog(id: number): void {
    this.cancellingId.set(id);
    this.cancelDialogVisible.set(true);
  }

  protected confirmCancel(): void {
    const id = this.cancellingId();
    if (!id) return;
    this.cancelling.set(true);
    this.bookingService.cancelBooking(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.cancelling.set(false);
        this.cancelDialogVisible.set(false);
        this.cancellingId.set(null);
        this.messageService.add({
          severity: 'success',
          summary: 'Réservation annulée',
          detail: 'Votre réservation a été annulée.',
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
