import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { ListingResponse } from '../../../../core/models/listing.models';
import { AdminEmptyState } from '../../shared/admin-empty-state/admin-empty-state';
import { AdminErrorState } from '../../shared/admin-error-state/admin-error-state';
import { RelativeTimePipe } from '../../../../shared/pipes/relative-time-pipe';

@Component({
  selector: 'app-moderation-listings-tab',
  imports: [ButtonModule, TagModule, SkeletonModule, AdminEmptyState, AdminErrorState, RelativeTimePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './moderation-listings-tab.html',
  styleUrl: './moderation-listings-tab.scss',
})
export class ModerationListingsTab {
  listings = input<(ListingResponse & { waitingDays?: number })[]>([]);
  loading = input(false);
  error = input(false);
  actionLoading = input<Record<string, boolean>>({});

  validate = output<ListingResponse>();
  reject = output<ListingResponse>();
  viewDetail = output<ListingResponse>();
  retry = output<void>();

  protected isLoading(key: string): boolean {
    return this.actionLoading()[key] ?? false;
  }

  protected formatXAF(amount: number): string {
    return new Intl.NumberFormat('fr-FR').format(amount) + ' XAF';
  }
}
