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
import { SelectModule } from 'primeng/select';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { PaginatorModule } from 'primeng/paginator';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ListingService } from '../../core/services/listing';
import { ListingSearchResponse } from '../../core/models/listing.models';
import { ListingCard } from '../../shared/components/listing-card/listing-card';
import { environment } from '../../../environments/environment';

declare const L: typeof import('leaflet');

@Component({
  selector: 'app-search',
  imports: [
    FormsModule,
    InputTextModule,
    SelectModule,
    InputNumberModule,
    ButtonModule,
    PaginatorModule,
    ProgressSpinnerModule,
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

  protected keyword = '';
  protected roomType = '';
  protected priceMin: number | null = null;
  protected priceMax: number | null = null;
  protected sortBy = '';
  protected page = 0;
  protected size = 10;

  protected roomTypeOptions = [
    { label: 'Tous les types', value: '' },
    { label: 'Studio', value: 'STUDIO' },
    { label: 'Chambre simple', value: 'SINGLE_ROOM' },
    { label: 'Chambre double', value: 'DOUBLE_ROOM' },
    { label: 'Appartement', value: 'APARTMENT' },
  ];

  protected sortOptions = [
    { label: 'Pertinence', value: '' },
    { label: 'Prix croissant', value: 'PRICE_ASC' },
    { label: 'Prix decroissant', value: 'PRICE_DESC' },
    { label: 'Distance', value: 'DISTANCE_ASC' },
    { label: 'Note', value: 'RATING_DESC' },
  ];

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.keyword = params['keyword'] ?? '';
      this.roomType = params['roomType'] ?? '';
      this.priceMin = params['priceMin'] ? +params['priceMin'] : null;
      this.priceMax = params['priceMax'] ? +params['priceMax'] : null;
      this.sortBy = params['sortBy'] ?? '';
      this.doSearch();
    });
  }

  ngAfterViewInit(): void {
    this.initMap();
  }

  protected doSearch(): void {
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
