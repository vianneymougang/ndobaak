import { Component, ChangeDetectionStrategy, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';
import { ListingService } from '../../../core/services/listing';
import { ListingResponse, ListingStatus } from '../../../core/models/listing.models';

@Component({
  selector: 'app-my-listings',
  imports: [RouterLink, ButtonModule, TagModule, ProgressSpinnerModule, TooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './my-listings.html',
  styleUrl: './my-listings.scss',
})
export class MyListings implements OnInit {
  private listingService = inject(ListingService);
  private destroyRef = inject(DestroyRef);

  protected listings = signal<ListingResponse[]>([]);
  protected loading = signal(true);

  ngOnInit(): void {
    this.listingService.getMyListings().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (listings) => {
        this.listings.set(listings);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected statusSeverity(status: ListingStatus): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    const map: Record<ListingStatus, 'success' | 'info' | 'warn' | 'danger' | 'secondary'> = {
      DRAFT: 'secondary',
      PENDING_REVIEW: 'warn',
      PUBLISHED: 'success',
      REJECTED: 'danger',
      ARCHIVED: 'info',
    };
    return map[status] ?? 'info';
  }

  protected statusLabel(status: ListingStatus): string {
    const map: Record<ListingStatus, string> = {
      DRAFT: 'Brouillon',
      PENDING_REVIEW: 'En attente',
      PUBLISHED: 'Publiee',
      REJECTED: 'Rejetee',
      ARCHIVED: 'Archivee',
    };
    return map[status] ?? status;
  }

  protected formatPrice(price: number): string {
    return new Intl.NumberFormat('fr-FR').format(price) + ' FCFA';
  }

  protected submitForReview(listing: ListingResponse): void {
    this.listingService.submitForReview(listing.id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((updated) => {
      this.listings.update((list) =>
        list.map((l) => (l.id === updated.id ? updated : l))
      );
    });
  }
}
