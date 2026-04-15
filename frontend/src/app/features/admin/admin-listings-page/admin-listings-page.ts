import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { InputTextModule } from 'primeng/inputtext';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { AdminService } from '../../../core/services/admin';
import { ListingResponse, ListingStatus } from '../../../core/models/listing.models';
import { GlobalStats } from '../../../core/models/admin.models';
import { AdminEmptyState } from '../shared/admin-empty-state/admin-empty-state';
import { AdminErrorState } from '../shared/admin-error-state/admin-error-state';
import { ListingDetailDrawer } from '../shared/listing-detail-drawer/listing-detail-drawer';

@Component({
  selector: 'app-admin-listings-page',
  imports: [
    DatePipe,
    FormsModule,
    TableModule,
    TagModule,
    ButtonModule,
    SelectButtonModule,
    InputTextModule,
    IconFieldModule,
    InputIconModule,
    PaginatorModule,
    SkeletonModule,
    AdminEmptyState,
    AdminErrorState,
    ListingDetailDrawer,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-listings-page.html',
  styleUrl: './admin-listings-page.scss',
})
export class AdminListingsPage implements OnInit {
  private adminService = inject(AdminService);
  private destroyRef = inject(DestroyRef);

  protected stats = signal<GlobalStats | null>(null);
  protected listings = signal<ListingResponse[]>([]);
  protected totalElements = signal(0);
  protected loading = signal(true);
  protected error = signal(false);

  protected search = signal('');
  protected statusFilter = signal<string>('ALL');
  protected page = signal(0);
  protected pageSize = 20;

  protected selectedListingId = signal<number | null>(null);

  protected statusOptions = [
    { label: 'Toutes', value: 'ALL' },
    { label: 'Publiées', value: 'PUBLISHED' },
    { label: 'En attente', value: 'PENDING_REVIEW' },
    { label: 'Refusées', value: 'REJECTED' },
  ];

  ngOnInit(): void {
    this.loadStats();
    this.loadListings();
  }

  protected onSearch(value: string): void {
    this.search.set(value);
    this.page.set(0);
    this.loadListings();
  }

  protected onStatusChange(status: string): void {
    this.statusFilter.set(status);
    this.page.set(0);
    this.loadListings();
  }

  protected onPageChange(event: PaginatorState): void {
    this.page.set(event.page ?? 0);
    this.loadListings();
  }

  protected openDetail(listing: ListingResponse): void {
    this.selectedListingId.set(listing.id);
  }

  protected onDrawerClosed(): void {
    this.selectedListingId.set(null);
  }

  protected onListingChanged(): void {
    this.loadListings();
    this.loadStats();
  }

  protected statusSeverity(status: string): 'success' | 'warn' | 'danger' | 'info' {
    if (status === 'PUBLISHED') return 'success';
    if (status === 'PENDING_REVIEW') return 'warn';
    if (status === 'REJECTED') return 'danger';
    return 'info';
  }

  protected statusLabel(status: string): string {
    if (status === 'PUBLISHED') return 'Publiée';
    if (status === 'PENDING_REVIEW') return 'En attente';
    if (status === 'REJECTED') return 'Refusée';
    if (status === 'DRAFT') return 'Brouillon';
    if (status === 'ARCHIVED') return 'Archivée';
    return status;
  }

  protected formatXAF(amount: number): string {
    return new Intl.NumberFormat('fr-FR').format(amount) + ' XAF';
  }

  protected retryLoad(): void {
    this.loadStats();
    this.loadListings();
  }

  private loadStats(): void {
    this.adminService.getGlobalStats().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.stats.set(data);
      },
    });
  }

  private loadListings(): void {
    this.loading.set(true);
    this.error.set(false);
    const status = this.statusFilter() === 'ALL' ? undefined : this.statusFilter();
    const search = this.search() || undefined;

    this.adminService.getListings(status, search, this.page(), this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.listings.set(page.content);
          this.totalElements.set(page.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.error.set(true);
          this.loading.set(false);
        },
      });
  }
}
