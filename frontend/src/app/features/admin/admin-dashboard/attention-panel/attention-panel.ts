import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

import { AdminReview, AdminTransaction } from '../../../../core/models/admin.models';
import { ListingResponse } from '../../../../core/models/listing.models';
import { SectionHeader } from '../../../../shared/components/section-header/section-header';
import { RelativeTimePipe } from '../../../../shared/pipes/relative-time-pipe';

@Component({
  selector: 'app-attention-panel',
  imports: [RouterLink, SkeletonModule, ButtonModule, TagModule, SectionHeader, RelativeTimePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './attention-panel.html',
  styleUrl: './attention-panel.scss',
})
export class AttentionPanel {
  pendingListings = input<(ListingResponse & { waitingDays?: number })[]>([]);
  pendingReviews = input<AdminReview[]>([]);
  anomalies = input<AdminTransaction[]>([]);
  loadingListings = input(false);
  loadingReviews = input(false);
  loadingAnomalies = input(false);
  errorListings = input(false);
  errorReviews = input(false);
  errorAnomalies = input(false);
  actionLoading = input<Record<string, boolean>>({});

  validateListing = output<ListingResponse>();
  rejectListing = output<ListingResponse>();
  approveReview = output<AdminReview>();
  rejectReview = output<AdminReview>();
  retryListings = output<void>();
  retryReviews = output<void>();
  retryAnomalies = output<void>();

  protected isLoading(key: string): boolean {
    return this.actionLoading()[key] ?? false;
  }

  protected formatXAF(amount: number): string {
    return new Intl.NumberFormat('fr-FR').format(amount) + ' XAF';
  }

  protected formatOperator(op?: string): string {
    if (!op) return '';
    if (op.includes('MTN')) return 'MTN MoMo';
    if (op.includes('ORANGE')) return 'Orange Money';
    return op;
  }

  protected getStarArray(rating: number): number[] {
    return Array.from({ length: 5 }, (_, i) => (i < rating ? 1 : 0));
  }

  protected hasAnyContent(): boolean {
    return (
      !this.loadingListings() &&
      !this.loadingReviews() &&
      !this.loadingAnomalies() &&
      !this.errorListings() &&
      !this.errorReviews() &&
      !this.errorAnomalies() &&
      this.pendingListings().length === 0 &&
      this.pendingReviews().length === 0 &&
      this.anomalies().length === 0
    );
  }
}
