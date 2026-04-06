import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { AdminReview } from '../../../../core/models/admin.models';
import { AdminEmptyState } from '../../shared/admin-empty-state/admin-empty-state';
import { AdminErrorState } from '../../shared/admin-error-state/admin-error-state';
import { RelativeTimePipe } from '../../../../shared/pipes/relative-time-pipe';

@Component({
  selector: 'app-moderation-reviews-tab',
  imports: [ButtonModule, TagModule, SkeletonModule, AdminEmptyState, AdminErrorState, RelativeTimePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './moderation-reviews-tab.html',
  styleUrl: './moderation-reviews-tab.scss',
})
export class ModerationReviewsTab {
  reviews = input<AdminReview[]>([]);
  loading = input(false);
  error = input(false);
  actionLoading = input<Record<string, boolean>>({});

  approve = output<AdminReview>();
  reject = output<AdminReview>();
  retry = output<void>();

  protected isLoading(key: string): boolean {
    return this.actionLoading()[key] ?? false;
  }

  protected getStarArray(rating: number): number[] {
    return Array.from({ length: 5 }, (_, i) => (i < rating ? 1 : 0));
  }
}
