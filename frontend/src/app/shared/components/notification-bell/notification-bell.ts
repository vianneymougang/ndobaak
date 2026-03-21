import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  OnDestroy,
  DestroyRef,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timer, switchMap, catchError, of } from 'rxjs';
import { OverlayBadgeModule } from 'primeng/overlaybadge';
import { PopoverModule } from 'primeng/popover';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { NotificationService } from '../../../core/services/notification';
import { NotificationResponse } from '../../../core/models/notification-models';

@Component({
  selector: 'app-notification-bell',
  imports: [
    RouterLink,
    DatePipe,
    OverlayBadgeModule,
    PopoverModule,
    ButtonModule,
    SkeletonModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './notification-bell.html',
  styleUrl: './notification-bell.scss',
})
export class NotificationBell implements OnInit, OnDestroy {
  private notificationService = inject(NotificationService);
  private destroyRef = inject(DestroyRef);

  protected unreadCount = signal(0);
  protected notifications = signal<NotificationResponse[]>([]);
  protected loading = signal(false);

  ngOnInit(): void {
    this.pollUnreadCount();
  }

  ngOnDestroy(): void {}

  private pollUnreadCount(): void {
    timer(0, 30_000)
      .pipe(
        switchMap(() => this.notificationService.getUnreadCount().pipe(catchError(() => of(0)))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((count) => this.unreadCount.set(count));
  }

  protected onPopoverShow(): void {
    this.loading.set(true);
    this.notificationService
      .getNotifications(0, 5)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.notifications.set(page.content);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  protected markAsRead(notification: NotificationResponse, event: Event): void {
    event.stopPropagation();
    if (notification.read) return;
    this.notificationService
      .markAsRead(notification.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.notifications.update((list) =>
          list.map((n) => (n.id === notification.id ? { ...n, read: true } : n)),
        );
        this.unreadCount.update((c) => Math.max(0, c - 1));
      });
  }

  protected markAllAsRead(): void {
    this.notificationService
      .markAllAsRead()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.notifications.update((list) => list.map((n) => ({ ...n, read: true })));
        this.unreadCount.set(0);
      });
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

  protected getRoute(notification: NotificationResponse): string | null {
    if (!notification.referenceId || !notification.referenceType) return null;
    const routes: Record<string, string> = {
      BOOKING: `/my-bookings`,
      LISTING: `/listings/${notification.referenceId}`,
      PAYMENT: `/transactions`,
      REVIEW: `/listings/${notification.referenceId}`,
    };
    return routes[notification.referenceType] ?? null;
  }
}
