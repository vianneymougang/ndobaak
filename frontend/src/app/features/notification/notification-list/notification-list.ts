import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  effect,
  DestroyRef,
} from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PaginatorModule } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';
import { NotificationService } from '../../../core/services/notification';
import { NotificationResponse } from '../../../core/models/notification-models';

@Component({
  selector: 'app-notification-list',
  imports: [DatePipe, PaginatorModule, SkeletonModule, ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './notification-list.html',
  styleUrl: './notification-list.scss',
})
export class NotificationList {
  private notificationService = inject(NotificationService);
  private destroyRef = inject(DestroyRef);
  private router = inject(Router);

  protected notifications = signal<NotificationResponse[]>([]);
  protected loading = signal(true);
  protected page = signal(0);
  protected totalElements = signal(0);
  protected pageSize = 20;

  constructor() {
    effect(() => {
      const p = this.page();
      this.loadNotifications(p);
    });
  }

  private loadNotifications(page: number): void {
    this.loading.set(true);
    this.notificationService
      .getNotifications(page, this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.notifications.set(result.content);
          this.totalElements.set(result.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  protected onPageChange(event: { page?: number }): void {
    this.page.set(event.page ?? 0);
  }

  protected markAsRead(notification: NotificationResponse): void {
    if (notification.read) {
      this.navigateToReference(notification);
      return;
    }
    this.notificationService
      .markAsRead(notification.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.notifications.update((list) =>
          list.map((n) => (n.id === notification.id ? { ...n, read: true } : n)),
        );
        this.navigateToReference(notification);
      });
  }

  protected markAllAsRead(): void {
    this.notificationService
      .markAllAsRead()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.notifications.update((list) => list.map((n) => ({ ...n, read: true })));
      });
  }

  private navigateToReference(notification: NotificationResponse): void {
    const route = this.getRoute(notification);
    if (route) {
      this.router.navigateByUrl(route);
    }
  }

  protected getRoute(notification: NotificationResponse): string | null {
    if (!notification.referenceId || !notification.referenceType) return null;
    const routes: Record<string, string> = {
      BOOKING: '/my-bookings',
      LISTING: `/listings/${notification.referenceId}`,
      PAYMENT: '/transactions',
      REVIEW: `/listings/${notification.referenceId}`,
    };
    return routes[notification.referenceType] ?? null;
  }

  protected getIcon(type: string): string {
    const icons: Record<string, string> = {
      BOOKING_CREATED: 'pi-calendar-plus',
      BOOKING_CONFIRMED: 'pi-check-circle',
      BOOKING_CANCELLED: 'pi-times-circle',
      PAYMENT_RECEIVED: 'pi-wallet',
      PAYMENT_FAILED: 'pi-exclamation-triangle',
      PAYMENT_REMINDER: 'pi-clock',
      REVIEW_POSTED: 'pi-star',
      LISTING_APPROVED: 'pi-verified',
      LISTING_REJECTED: 'pi-ban',
    };
    return icons[type] ?? 'pi-bell';
  }
}
