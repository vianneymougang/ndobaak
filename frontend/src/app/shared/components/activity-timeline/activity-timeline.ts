import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { RelativeTimePipe } from '../../pipes/relative-time-pipe';

export interface ActivityItem {
  type: string;
  title: string;
  message: string;
  createdAt: string;
  referenceId: number;
}

@Component({
  selector: 'app-activity-timeline',
  imports: [SkeletonModule, NgmMotionDirective, RelativeTimePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './activity-timeline.html',
  styleUrl: './activity-timeline.scss',
})
export class ActivityTimeline {
  items = input<ActivityItem[]>([]);
  loading = input(false);
  error = input(false);
  emptyMessage = input('Aucune activité récente');

  itemClicked = output<ActivityItem>();
  retry = output<void>();

  protected getIcon(type: string): string {
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

  protected getIconClass(type: string): string {
    if (type.startsWith('PAYMENT')) return 'timeline-icon--gold';
    if (type.startsWith('BOOKING_CONFIRMED') || type.startsWith('LISTING_APPROVED')) return 'timeline-icon--success';
    if (type.startsWith('BOOKING_CANCELLED') || type.startsWith('LISTING_REJECTED') || type.startsWith('PAYMENT_FAILED')) return 'timeline-icon--error';
    if (type.startsWith('REVIEW')) return 'timeline-icon--star';
    return 'timeline-icon--default';
  }

  protected staggerDelay(index: number): number {
    return index * 0.06;
  }
}
