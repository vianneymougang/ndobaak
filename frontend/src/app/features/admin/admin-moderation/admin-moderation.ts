import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  OnInit,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { TabsModule } from 'primeng/tabs';
import { BadgeModule } from 'primeng/badge';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageService } from 'primeng/api';
import { AdminService } from '../../../core/services/admin';
import { ListingResponse } from '../../../core/models/listing.models';
import { AdminReview, AdminTransaction } from '../../../core/models/admin.models';
import { ModerationListingsTab } from './moderation-listings-tab/moderation-listings-tab';
import { ModerationReviewsTab } from './moderation-reviews-tab/moderation-reviews-tab';
import { ModerationAnomaliesTab } from './moderation-anomalies-tab/moderation-anomalies-tab';
import { ListingDetailDrawer } from '../shared/listing-detail-drawer/listing-detail-drawer';

@Component({
  selector: 'app-admin-moderation',
  imports: [
    TabsModule,
    BadgeModule,
    SkeletonModule,
    ModerationListingsTab,
    ModerationReviewsTab,
    ModerationAnomaliesTab,
    ListingDetailDrawer,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-moderation.html',
  styleUrl: './admin-moderation.scss',
})
export class AdminModeration implements OnInit {
  private adminService = inject(AdminService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);

  protected pendingListings = signal<(ListingResponse & { waitingDays?: number })[]>([]);
  protected pendingReviews = signal<AdminReview[]>([]);
  protected anomalies = signal<AdminTransaction[]>([]);

  protected loadingListings = signal(true);
  protected loadingReviews = signal(true);
  protected loadingAnomalies = signal(true);
  protected errorListings = signal(false);
  protected errorReviews = signal(false);
  protected errorAnomalies = signal(false);
  protected actionLoading = signal<Record<string, boolean>>({});

  protected selectedListingId = signal<number | null>(null);

  protected totalPending = computed(
    () => this.pendingListings().length + this.pendingReviews().length + this.anomalies().length,
  );

  ngOnInit(): void {
    this.loadAllData();
  }

  protected onValidateListing(listing: ListingResponse): void {
    this.setActionLoading('listing-' + listing.id, true);
    this.adminService.validateListing(listing.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.pendingListings.update(list => list.filter(l => l.id !== listing.id));
        this.setActionLoading('listing-' + listing.id, false);
        this.messageService.add({ severity: 'success', summary: 'Annonce approuvée', life: 3000 });
      },
      error: () => {
        this.setActionLoading('listing-' + listing.id, false);
        this.messageService.add({ severity: 'error', summary: 'Erreur lors de la validation', life: 4000 });
      },
    });
  }

  protected onRejectListing(listing: ListingResponse): void {
    this.selectedListingId.set(listing.id);
  }

  protected onListingValidated(id: number): void {
    this.pendingListings.update(list => list.filter(l => l.id !== id));
    this.selectedListingId.set(null);
    this.messageService.add({ severity: 'success', summary: 'Annonce approuvée', life: 3000 });
  }

  protected onListingRejected(id: number): void {
    this.pendingListings.update(list => list.filter(l => l.id !== id));
    this.selectedListingId.set(null);
    this.messageService.add({ severity: 'info', summary: 'Annonce refusée', life: 3000 });
  }

  protected onViewListingDetail(listing: ListingResponse): void {
    this.selectedListingId.set(listing.id);
  }

  protected onDrawerClosed(): void {
    this.selectedListingId.set(null);
  }

  protected onApproveReview(review: AdminReview): void {
    this.setActionLoading('review-' + review.id, true);
    this.adminService.approveReview(review.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.pendingReviews.update(list => list.filter(r => r.id !== review.id));
        this.setActionLoading('review-' + review.id, false);
        this.messageService.add({ severity: 'success', summary: 'Avis approuvé', life: 3000 });
      },
      error: () => {
        this.setActionLoading('review-' + review.id, false);
        this.messageService.add({ severity: 'error', summary: 'Erreur lors de l\'approbation', life: 4000 });
      },
    });
  }

  protected onRejectReview(review: AdminReview): void {
    this.setActionLoading('review-reject-' + review.id, true);
    this.adminService.rejectReview(review.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.pendingReviews.update(list => list.filter(r => r.id !== review.id));
        this.setActionLoading('review-reject-' + review.id, false);
        this.messageService.add({ severity: 'info', summary: 'Avis rejeté', life: 3000 });
      },
      error: () => {
        this.setActionLoading('review-reject-' + review.id, false);
        this.messageService.add({ severity: 'error', summary: 'Erreur lors du rejet', life: 4000 });
      },
    });
  }

  protected retryListings(): void {
    this.loadListings();
  }

  protected retryReviews(): void {
    this.loadReviews();
  }

  protected retryAnomalies(): void {
    this.loadAnomalies();
  }

  private loadAllData(): void {
    this.loadListings();
    this.loadReviews();
    this.loadAnomalies();
  }

  private loadListings(): void {
    this.loadingListings.set(true);
    this.errorListings.set(false);
    this.adminService.getPendingListings().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        const now = Date.now();
        const enriched = data.map(l => ({
          ...l,
          waitingDays: Math.floor((now - new Date(l.createdAt).getTime()) / 86400000),
        }));
        this.pendingListings.set(enriched);
        this.loadingListings.set(false);
      },
      error: () => {
        this.errorListings.set(true);
        this.loadingListings.set(false);
      },
    });
  }

  private loadReviews(): void {
    this.loadingReviews.set(true);
    this.errorReviews.set(false);
    this.adminService.getReviews('pending', 0, 50).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (page) => {
        this.pendingReviews.set(page.content);
        this.loadingReviews.set(false);
      },
      error: () => {
        this.errorReviews.set(true);
        this.loadingReviews.set(false);
      },
    });
  }

  private loadAnomalies(): void {
    this.loadingAnomalies.set(true);
    this.errorAnomalies.set(false);
    this.adminService.getAnomalies().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.anomalies.set(data);
        this.loadingAnomalies.set(false);
      },
      error: () => {
        this.errorAnomalies.set(true);
        this.loadingAnomalies.set(false);
      },
    });
  }

  private setActionLoading(key: string, value: boolean): void {
    this.actionLoading.update(map => ({ ...map, [key]: value }));
  }
}
