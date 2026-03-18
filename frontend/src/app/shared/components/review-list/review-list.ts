import {
  Component,
  ChangeDetectionStrategy,
  input,
  signal,
  computed,
  inject,
  OnInit,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { PaginatorModule } from 'primeng/paginator';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { GalleriaModule } from 'primeng/galleria';
import { FormsModule } from '@angular/forms';
import { StarRating } from '../star-rating/star-rating';
import { ReviewService } from '../../../core/services/review';
import { ReviewResponse, RatingStatsResponse } from '../../../core/models/review.models';

interface SortOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-review-list',
  imports: [
    DatePipe,
    AvatarModule,
    ButtonModule,
    PaginatorModule,
    SelectModule,
    SkeletonModule,
    GalleriaModule,
    FormsModule,
    StarRating,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './review-list.html',
  styleUrl: './review-list.scss',
})
export class ReviewList implements OnInit {
  listingId = input.required<number>();

  private reviewService = inject(ReviewService);

  protected reviews = signal<ReviewResponse[]>([]);
  protected ratingStats = signal<RatingStatsResponse | null>(null);
  protected loading = signal(true);
  protected totalElements = signal(0);
  protected page = signal(0);
  protected pageSize = 10;
  protected selectedSort = signal('createdAt,desc');
  protected expandedReviews = signal<Set<number>>(new Set());
  protected galleriaVisible = signal(false);
  protected galleriaImages = signal<{ source: string }[]>([]);
  protected galleriaActiveIndex = signal(0);

  protected sortOptions: SortOption[] = [
    { label: 'Plus récents', value: 'createdAt,desc' },
    { label: 'Meilleures notes', value: 'rating,desc' },
    { label: 'Notes les plus basses', value: 'rating,asc' },
  ];

  protected distributionBars = computed(() => {
    const stats = this.ratingStats();
    if (!stats || stats.totalReviews === 0) return [];

    return [5, 4, 3, 2, 1].map((star) => ({
      star,
      count: stats.distribution[star] ?? 0,
      percentage: Math.round(((stats.distribution[star] ?? 0) / stats.totalReviews) * 100),
    }));
  });

  ngOnInit(): void {
    this.loadRatingStats();
    this.loadReviews();
  }

  protected onPageChange(event: { page?: number }): void {
    this.page.set(event.page ?? 0);
    this.loadReviews();
  }

  protected onSortChange(): void {
    this.page.set(0);
    this.loadReviews();
  }

  protected toggleExpanded(reviewId: number): void {
    this.expandedReviews.update((set) => {
      const next = new Set(set);
      if (next.has(reviewId)) {
        next.delete(reviewId);
      } else {
        next.add(reviewId);
      }
      return next;
    });
  }

  protected isExpanded(reviewId: number): boolean {
    return this.expandedReviews().has(reviewId);
  }

  protected openGalleria(photos: string[], index: number): void {
    this.galleriaImages.set(photos.map((url) => ({ source: url })));
    this.galleriaActiveIndex.set(index);
    this.galleriaVisible.set(true);
  }

  protected getInitials(name: string): string {
    return name
      .split(' ')
      .map((part) => part.charAt(0))
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  private loadRatingStats(): void {
    this.reviewService.getRatingStats(this.listingId()).subscribe({
      next: (stats) => this.ratingStats.set(stats),
    });
  }

  private loadReviews(): void {
    this.loading.set(true);
    this.reviewService
      .getReviewsByListing(this.listingId(), this.page(), this.pageSize, this.selectedSort())
      .subscribe({
        next: (response) => {
          this.reviews.set(response.content);
          this.totalElements.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}
