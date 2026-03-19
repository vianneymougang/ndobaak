import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  OnInit,
  DestroyRef,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { MessageService } from 'primeng/api';
import {
  DashboardService,
  LandlordDashboardResponse,
} from '../../../core/services/dashboard';
import { BookingService } from '../../../core/services/booking';
import { SubscriptionService } from '../../../core/services/subscription';
import { Subscription } from '../../../core/models/subscription.models';

@Component({
  selector: 'app-landlord-dashboard',
  imports: [RouterLink, CurrencyPipe, DatePipe, CardModule, ButtonModule, SkeletonModule, TagModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './landlord-dashboard.html',
  styleUrl: './landlord-dashboard.scss',
})
export class LandlordDashboard implements OnInit {
  private dashboardService = inject(DashboardService);
  private bookingService = inject(BookingService);
  private subscriptionService = inject(SubscriptionService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);

  protected dashboard = signal<LandlordDashboardResponse | null>(null);
  protected subscription = signal<Subscription | null | undefined>(undefined);
  protected loading = signal(true);
  protected subscriptionLoading = signal(true);

  protected subscriptionExpiringSoon = computed(() => {
    const sub = this.subscription();
    return sub?.status === 'ACTIVE' && (sub.daysRemaining ?? 0) <= 30;
  });

  ngOnInit(): void {
    this.loadDashboard();
    this.loadSubscription();
  }

  private loadSubscription(): void {
    this.subscriptionLoading.set(true);
    this.subscriptionService
      .getMySubscription()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (sub) => {
          this.subscription.set(sub);
          this.subscriptionLoading.set(false);
        },
        error: () => {
          this.subscription.set(null);
          this.subscriptionLoading.set(false);
        },
      });
  }

  private loadDashboard(): void {
    this.loading.set(true);
    this.dashboardService
      .getLandlordDashboard()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.dashboard.set(data);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger le tableau de bord. Réessayez.',
          });
        },
      });
  }

  protected confirmBooking(bookingId: number): void {
    this.bookingService
      .confirmBooking(bookingId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Réservation confirmée',
            detail: 'La réservation a été confirmée avec succès.',
          });
          this.loadDashboard();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de confirmer la réservation.',
          });
        },
      });
  }

  protected rejectBooking(bookingId: number): void {
    this.bookingService
      .rejectBooking(bookingId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'warn',
            summary: 'Réservation refusée',
            detail: 'La demande a été refusée.',
          });
          this.loadDashboard();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de refuser la réservation.',
          });
        },
      });
  }

  protected getActivityIcon(type: string): string {
    const icons: Record<string, string> = {
      BOOKING_CREATED: 'pi-calendar-plus',
      BOOKING_CONFIRMED: 'pi-check-circle',
      BOOKING_CANCELLED: 'pi-times-circle',
      PAYMENT_RECEIVED: 'pi-wallet',
      PAYMENT_FAILED: 'pi-exclamation-triangle',
      REVIEW_POSTED: 'pi-star',
      LISTING_APPROVED: 'pi-verified',
      LISTING_REJECTED: 'pi-ban',
    };
    return icons[type] ?? 'pi-bell';
  }
}
