import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  AfterViewInit,
  ElementRef,
  viewChild,
  computed,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { GalleriaModule } from 'primeng/galleria';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ChipModule } from 'primeng/chip';
import { AvatarModule } from 'primeng/avatar';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ListingService } from '../../../core/services/listing';
import { ListingResponse } from '../../../core/models/listing.models';
import { amenityLabel, amenityIcon } from '../../../core/models/amenity.config';
import { environment } from '../../../../environments/environment';

declare const L: typeof import('leaflet');

@Component({
  selector: 'app-detail',
  imports: [GalleriaModule, ButtonModule, TagModule, ChipModule, AvatarModule, ProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './detail.html',
  styleUrl: './detail.scss',
})
export class Detail implements OnInit, AfterViewInit {
  private route = inject(ActivatedRoute);
  private listingService = inject(ListingService);

  protected detailMap = viewChild<ElementRef>('detailMap');
  private map: L.Map | null = null;

  protected listing = signal<ListingResponse | null>(null);
  protected loading = signal(true);

  protected roomTypeLabel = computed(() => {
    const labels: Record<string, string> = {
      STUDIO: 'Studio',
      SINGLE_ROOM: 'Chambre simple',
      DOUBLE_ROOM: 'Chambre double',
      APARTMENT: 'Appartement',
    };
    const l = this.listing();
    return l ? labels[l.roomType] ?? l.roomType : '';
  });

  protected formattedPrice = computed(() => {
    const l = this.listing();
    return l ? new Intl.NumberFormat('fr-FR').format(l.monthlyPrice) + ' FCFA/mois' : '';
  });

  protected landlordInitials = computed(() => {
    const l = this.listing()?.landlord;
    if (!l) return '';
    return (l.firstName?.charAt(0) ?? '') + (l.lastName?.charAt(0) ?? '');
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.listingService.getListingById(id).subscribe({
      next: (listing) => {
        this.listing.set(listing);
        this.loading.set(false);
        this.initDetailMap();
      },
      error: () => this.loading.set(false),
    });
  }

  ngAfterViewInit(): void {
    this.initDetailMap();
  }

  protected readonly amenityLabel = amenityLabel;
  protected readonly amenityIcon = amenityIcon;

  private initDetailMap(): void {
    if (typeof L === 'undefined') return;
    const el = this.detailMap()?.nativeElement;
    const loc = this.listing()?.location;
    if (!el || !loc) return;
    if (this.map) return;

    this.map = L.map(el).setView([loc.latitude, loc.longitude], 15);
    L.tileLayer(environment.mapTileUrl, {
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.map);
    L.marker([loc.latitude, loc.longitude]).addTo(this.map);
  }
}
