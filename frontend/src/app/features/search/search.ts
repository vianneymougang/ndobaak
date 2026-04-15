import {
  Component,
  ChangeDetectionStrategy,
  DestroyRef,
  inject,
  signal,
  OnInit,
  effect,
  ElementRef,
  viewChild,
  PLATFORM_ID,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { PaginatorModule } from 'primeng/paginator';
import { DrawerModule } from 'primeng/drawer';
import { PopoverModule } from 'primeng/popover';
import { SliderModule } from 'primeng/slider';
import { SelectButtonModule } from 'primeng/selectbutton';
import { ListingService } from '../../core/services/listing';
import { GeolocationService } from '../../core/services/geolocation';
import { ListingSearchResponse, NearbyListingResponse } from '../../core/models/listing.models';
import { ListingCard } from '../../shared/components/listing-card/listing-card';
import { environment } from '../../../environments/environment';
import type * as LeafletNS from 'leaflet';

const MAX_PRICE = 300_000;

const CAMPUS_LAT = 3.8844536562668015;
const CAMPUS_LNG = 11.391160302004234;

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
    SelectButtonModule,
    ListingCard,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './search.html',
  styleUrl: './search.scss',
})
export class Search implements OnInit {
  private route = inject(ActivatedRoute);
  private listingService = inject(ListingService);
  private geolocationService = inject(GeolocationService);
  private destroyRef = inject(DestroyRef);

  protected mapContainer = viewChild<ElementRef>('mapContainer');
  private L: typeof LeafletNS | null = null;
  private map: LeafletNS.Map | null = null;
  private markers: LeafletNS.Marker[] = [];
  private userMarker: LeafletNS.CircleMarker | null = null;
  private radiusCircle: LeafletNS.Circle | null = null;

  protected results = signal<ListingSearchResponse[]>([]);
  protected totalElements = signal(0);
  protected loading = signal(false);
  protected drawerVisible = signal(false);
  protected locationDenied = this.geolocationService.denied;
  protected userPosition = this.geolocationService.position;
  protected activeRadius = signal<number>(2);
  protected searchThisAreaVisible = signal(false);
  protected readonly radiusOptions = [
    { label: '0.5 km', value: 0.5 },
    { label: '1 km', value: 1 },
    { label: '2 km', value: 2 },
    { label: '5 km', value: 5 },
  ];

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
    this.geolocationService.requestPosition().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
    this.route.queryParams.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.keyword = params['keyword'] ?? '';
      this.roomType = params['roomType'] ?? '';
      this.priceMin = params['priceMin'] ? +params['priceMin'] : null;
      this.priceMax = params['priceMax'] ? +params['priceMax'] : null;
      this.sortBy = params['sortBy'] ?? '';
      this.priceRange = [this.priceMin ?? 0, this.priceMax ?? MAX_PRICE];
      this.doSearch();
    });
  }

  private platformId = inject(PLATFORM_ID);

  private mapEffect = effect(() => {
    const el = this.mapContainer();
    if (el && isPlatformBrowser(this.platformId)) {
      this.initMap();
    }
  });

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
      userLat: CAMPUS_LAT,
      userLng: CAMPUS_LNG,
      maxDistance: this.activeRadius(),
      page: this.page,
      size: this.size,
    };

    this.listingService.search(filters).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => {
        this.results.set(res.content);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
        this.updateMapMarkers(res.content);
      },
      error: () => this.loading.set(false),
    });
  }

  protected onRadiusChange(radius: number): void {
    this.activeRadius.set(radius);
    this.updateRadiusCircle();
    this.doSearch();
  }

  protected searchInCurrentMapArea(): void {
    if (!this.map) return;
    this.searchThisAreaVisible.set(false);
    this.loading.set(true);

    const center = this.map.getCenter();
    const radius = this.activeRadius();

    this.listingService.findNearby(center.lat, center.lng, radius).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (nearby) => {
        const mapped = this.mapNearbyToSearch(nearby);
        this.results.set(mapped);
        this.totalElements.set(mapped.length);
        this.loading.set(false);
        this.updateMapMarkers(mapped);
      },
      error: () => this.loading.set(false),
    });
  }

  private mapNearbyToSearch(items: NearbyListingResponse[]): ListingSearchResponse[] {
    return items.map(n => ({
      id: n.id,
      title: n.title,
      thumbnailUrl: n.thumbnailUrl,
      monthlyPrice: n.monthlyPrice,
      roomType: n.roomType,
      neighborhood: n.neighborhood,
      distanceToCampus: n.distanceToCampus,
      avgRating: n.avgRating,
      reviewCount: n.reviewCount,
      amenities: [],
      verified: n.verified,
      latitude: n.latitude,
      longitude: n.longitude,
      availableRooms: 0,
    }));
  }

  protected onPageChange(event: { page?: number; rows?: number }): void {
    this.page = event.page ?? 0;
    this.size = event.rows ?? 10;
    this.doSearch();
  }

  private formatK(value: number): string {
    return value >= 1000 ? `${Math.round(value / 1000)}k FCFA` : `${value} FCFA`;
  }

  private async initMap(): Promise<void> {
    const el = this.mapContainer()?.nativeElement;
    if (!el) return;

    const { L } = await import('../../core/utils/leaflet-fix');
    this.L = L;

    this.map = L.map(el).setView([CAMPUS_LAT, CAMPUS_LNG], 14);
    if (!environment.production) {
      (window as any).__ndb_map = this.map;
    }
    L.tileLayer(environment.mapTileUrl, {
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.map);

    const campusIcon = L.divIcon({
      className: 'campus-marker',
      html: '<i class="pi pi-building" style="font-size:1.5rem;color:#6366f1"></i>',
      iconSize: [30, 30],
      iconAnchor: [15, 15],
    });
    L.marker([CAMPUS_LAT, CAMPUS_LNG], { icon: campusIcon, zIndexOffset: 1000 })
      .addTo(this.map)
      .bindPopup('IUSJC Campus');

    this.updateUserMarker();
    this.updateRadiusCircle();

    let ignoreFirstMove = true;
    this.map.on('moveend', () => {
      if (ignoreFirstMove) {
        ignoreFirstMove = false;
        return;
      }
      this.searchThisAreaVisible.set(true);
    });
  }

  private updateUserMarker(): void {
    if (!this.map || !this.L) return;
    const L = this.L;

    if (this.userMarker) {
      this.userMarker.remove();
      this.userMarker = null;
    }

    const pos = this.geolocationService.position();
    if (!pos) return;

    this.userMarker = L.circleMarker([pos.latitude, pos.longitude], {
      radius: 8,
      fillColor: '#4A90D9',
      fillOpacity: 1,
      color: '#ffffff',
      weight: 3,
    }).addTo(this.map)
      .bindPopup('Votre position');
  }

  private updateRadiusCircle(): void {
    if (!this.map || !this.L) return;
    const L = this.L;

    if (this.radiusCircle) {
      this.radiusCircle.remove();
      this.radiusCircle = null;
    }

    this.radiusCircle = L.circle([CAMPUS_LAT, CAMPUS_LNG], {
      radius: this.activeRadius() * 1000,
      fillColor: '#4A90D9',
      fillOpacity: 0.08,
      color: '#4A90D9',
      weight: 1.5,
      dashArray: '6 4',
    }).addTo(this.map);
  }

  private updateMapMarkers(listings: ListingSearchResponse[]): void {
    if (!this.map || !this.L) return;
    const L = this.L;

    this.markers.forEach((m) => m.remove());
    this.markers = [];

    listings.forEach((listing) => {
      if (listing.latitude && listing.longitude) {
        const price = new Intl.NumberFormat('fr-FR').format(listing.monthlyPrice);
        const img = listing.thumbnailUrl
          ? `<img src="${listing.thumbnailUrl}" alt="${listing.title}" style="width:100%;height:100px;object-fit:cover;border-radius:6px;margin-bottom:6px">`
          : '';
        const marker = L.marker([listing.latitude, listing.longitude])
          .addTo(this.map!)
          .bindPopup(
            `<div style="min-width:180px;max-width:220px">${img}<strong style="font-size:13px;line-height:1.3;display:block">${listing.title}</strong><span style="color:var(--color-ndb-primary);font-weight:600">${price} FCFA/mois</span></div>`,
            { maxWidth: 240 }
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
