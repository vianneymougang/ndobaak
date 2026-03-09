import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  AfterViewInit,
  ElementRef,
  viewChild,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { PaginatorModule } from 'primeng/paginator';
import { DrawerModule } from 'primeng/drawer';
import { PopoverModule } from 'primeng/popover';
import { SliderModule } from 'primeng/slider';
import { ListingService } from '../../core/services/listing';
import { ListingSearchResponse } from '../../core/models/listing.models';
import { ListingCard } from '../../shared/components/listing-card/listing-card';
import { environment } from '../../../environments/environment';

declare const L: typeof import('leaflet');

const MAX_PRICE = 300_000;

@Component({
  selector: 'app-search',
  imports: [
    FormsModule,
    InputTextModule,
    ButtonModule,
    PaginatorModule,
    DrawerModule,
    PopoverModule,
    SliderModule,
    ListingCard,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './search.html',
  styleUrl: './search.scss',
})
export class Search implements OnInit, AfterViewInit {
  private route = inject(ActivatedRoute);
  private listingService = inject(ListingService);

  protected mapContainer = viewChild<ElementRef>('mapContainer');
  private map: L.Map | null = null;
  private markers: L.Marker[] = [];

  protected results = signal<ListingSearchResponse[]>([]);
  protected totalElements = signal(0);
  protected loading = signal(false);
  protected drawerVisible = signal(false);

  protected keyword = '';
  protected roomType = '';
  protected priceMin: number | null = null;
  protected priceMax: number | null = null;
  protected sortBy = '';
  protected page = 0;
  protected size = 10;
  protected priceRange: number[] = [0, MAX_PRICE];

  protected readonly maxPrice = MAX_PRICE;

  protected readonly roomTypeOptions = [
    { label: 'Tous les types', value: '' },
    { label: 'Studio', value: 'STUDIO' },
    { label: 'Chambre simple', value: 'SINGLE_ROOM' },
    { label: 'Chambre double', value: 'DOUBLE_ROOM' },
    { label: 'Appartement', value: 'APARTMENT' },
  ];

  protected readonly sortOptions = [
    { label: 'Pertinence', value: '' },
    { label: 'Prix croissant', value: 'PRICE_ASC' },
    { label: 'Prix décroissant', value: 'PRICE_DESC' },
    { label: 'Distance', value: 'DISTANCE_ASC' },
    { label: 'Note', value: 'RATING_DESC' },
  ];

  protected get activeRoomTypeLabel(): string {
    return this.roomTypeOptions.find(o => o.value === this.roomType)?.label ?? 'Type';
  }

  protected get activeSortLabel(): string {
    return this.sortOptions.find(o => o.value === this.sortBy)?.label ?? 'Tri';
  }

  protected get activePriceLabel(): string {
    const hasMin = this.priceMin !== null && this.priceMin > 0;
    const hasMax = this.priceMax !== null && this.priceMax < MAX_PRICE;
    if (!hasMin && !hasMax) return 'Prix';
    if (hasMin && hasMax) return `${this.formatK(this.priceMin!)} – ${this.formatK(this.priceMax!)}`;
    if (hasMin) return `≥ ${this.formatK(this.priceMin!)}`;
    return `≤ ${this.formatK(this.priceMax!)}`;
  }

  protected get activeFilterCount(): number {
    let count = 0;
    if (this.roomType) count++;
    if (this.hasPriceFilter) count++;
    if (this.sortBy) count++;
    return count;
  }

  protected get hasPriceFilter(): boolean {
    return (
      (this.priceMin !== null && this.priceMin > 0) ||
      (this.priceMax !== null && this.priceMax < MAX_PRICE)
    );
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.keyword = params['keyword'] ?? '';
      this.roomType = params['roomType'] ?? '';
      this.priceMin = params['priceMin'] ? +params['priceMin'] : null;
      this.priceMax = params['priceMax'] ? +params['priceMax'] : null;
      this.sortBy = params['sortBy'] ?? '';
      this.priceRange = [this.priceMin ?? 0, this.priceMax ?? MAX_PRICE];
      this.doSearch();
    });
  }

  ngAfterViewInit(): void {
    this.initMap();
  }

  protected selectRoomType(value: string): void {
    this.roomType = value;
  }

  protected selectSort(value: string): void {
    this.sortBy = value;
  }

  protected onSliderValueChange(values: number[]): void {
    this.priceRange = values;
    this.priceMin = values[0] === 0 ? null : values[0];
    this.priceMax = values[1] === MAX_PRICE ? null : values[1];
  }

  protected onPriceInputChange(): void {
    const min = typeof this.priceMin === 'number' && !isNaN(this.priceMin) ? this.priceMin : null;
    const max = typeof this.priceMax === 'number' && !isNaN(this.priceMax) ? this.priceMax : null;
    this.priceMin = min;
    this.priceMax = max;
    this.priceRange = [min ?? 0, max ?? MAX_PRICE];
  }

  protected clearPrice(): void {
    this.priceMin = null;
    this.priceMax = null;
    this.priceRange = [0, MAX_PRICE];
  }

  protected clearRoomType(): void {
    this.roomType = '';
  }

  protected clearSort(): void {
    this.sortBy = '';
  }

  protected clearAllFilters(): void {
    this.roomType = '';
    this.sortBy = '';
    this.clearPrice();
  }

  protected doSearch(): void {
    this.drawerVisible.set(false);
    this.loading.set(true);
    const filters: Record<string, unknown> = {
      keyword: this.keyword || null,
      roomType: this.roomType || null,
      priceMin: this.priceMin,
      priceMax: this.priceMax,
      sortBy: this.sortBy || null,
      page: this.page,
      size: this.size,
    };

    this.listingService.search(filters).subscribe({
      next: (res) => {
        this.results.set(res.content);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
        this.updateMapMarkers(res.content);
      },
      error: () => this.loading.set(false),
    });
  }

  protected onPageChange(event: { page?: number; rows?: number }): void {
    this.page = event.page ?? 0;
    this.size = event.rows ?? 10;
    this.doSearch();
  }

  private formatK(value: number): string {
    return value >= 1000 ? `${Math.round(value / 1000)}k FCFA` : `${value} FCFA`;
  }

  private initMap(): void {
    if (typeof L === 'undefined') return;
    const el = this.mapContainer()?.nativeElement;
    if (!el) return;

    this.map = L.map(el).setView([3.848, 11.502], 14);
    L.tileLayer(environment.mapTileUrl, {
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.map);
  }

  private updateMapMarkers(listings: ListingSearchResponse[]): void {
    if (!this.map) return;

    this.markers.forEach((m) => m.remove());
    this.markers = [];

    listings.forEach((listing) => {
      if (listing.latitude && listing.longitude) {
        const marker = L.marker([listing.latitude, listing.longitude])
          .addTo(this.map!)
          .bindPopup(
            `<strong>${listing.title}</strong><br>${new Intl.NumberFormat('fr-FR').format(listing.monthlyPrice)} FCFA/mois`
          );
        this.markers.push(marker);
      }
    });

    if (this.markers.length > 0) {
      const group = L.featureGroup(this.markers);
      this.map.fitBounds(group.getBounds().pad(0.1));
    }
  }
}
