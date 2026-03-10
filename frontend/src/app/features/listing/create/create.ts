import { Component, ChangeDetectionStrategy, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { StepperModule } from 'primeng/stepper';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { InputNumberModule } from 'primeng/inputnumber';
import { DatePickerModule } from 'primeng/datepicker';
import { CheckboxModule } from 'primeng/checkbox';
import { FloatLabelModule } from 'primeng/floatlabel';
import { TagModule } from 'primeng/tag';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ProgressBarModule } from 'primeng/progressbar';
import { MessageService } from 'primeng/api';
import { concat, EMPTY } from 'rxjs';
import { finalize, tap } from 'rxjs/operators';
import { ListingService } from '../../../core/services/listing';
import { ListingResponse, RoomType, CreateListingRequest, PhotoResponse } from '../../../core/models/listing.models';
import { AMENITY_CONFIG } from '../../../core/models/amenity.config';
import * as L from 'leaflet';

const CAMPUS_LAT = 3.8480;
const CAMPUS_LNG = 11.5021;

interface AmenityOption {
  name: string;
  label: string;
  icon: string;
  selected: boolean;
}

interface PendingPhoto {
  localUrl: string;
  file: File;
  uploading: boolean;
}

@Component({
  selector: 'app-create',
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    StepperModule,
    InputTextModule,
    TextareaModule,
    SelectModule,
    InputNumberModule,
    DatePickerModule,
    CheckboxModule,
    FloatLabelModule,
    TagModule,
    MessageModule,
    ProgressSpinnerModule,
    ProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './create.html',
  styleUrl: './create.scss',
})
export class Create implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private listingService = inject(ListingService);
  private messageService = inject(MessageService);
  private router = inject(Router);

  protected activeStep = signal(1);
  protected saving = signal(false);
  protected submitting = signal(false);
  protected listing = signal<ListingResponse | null>(null);
  protected uploadingPhotos = signal(false);
  protected uploadProgress = signal(0);
  protected uploadTotal = signal(0);
  protected photos = signal<PhotoResponse[]>([]);
  protected pendingPhotos = signal<PendingPhoto[]>([]);
  protected map: L.Map | null = null;
  private marker: L.Marker | null = null;

  protected roomTypeOptions = [
    { label: 'Studio', value: 'STUDIO' as RoomType },
    { label: 'Chambre simple', value: 'SINGLE_ROOM' as RoomType },
    { label: 'Chambre double', value: 'DOUBLE_ROOM' as RoomType },
    { label: 'Appartement', value: 'APARTMENT' as RoomType },
  ];

  protected amenities = signal<AmenityOption[]>(
    Object.entries(AMENITY_CONFIG).map(([name, config]) => ({
      name,
      label: config.label,
      icon: config.icon,
      selected: false,
    }))
  );

  protected infoForm = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(10)]],
    description: ['', [Validators.required, Validators.minLength(50)]],
    roomType: [null as RoomType | null, Validators.required],
    monthlyPrice: [null as number | null, [Validators.required, Validators.min(1)]],
    availableRooms: [1, [Validators.required, Validators.min(1)]],
    availableDate: [null as Date | null],
  });

  protected locationForm = this.fb.group({
    address: ['', Validators.required],
    neighborhood: [''],
    city: ['Yaounde'],
    latitude: [CAMPUS_LAT, Validators.required],
    longitude: [CAMPUS_LNG, Validators.required],
  });

  protected descriptionLength = computed(() => {
    const desc = this.infoForm.get('description')?.value;
    return desc ? desc.length : 0;
  });

  protected titleLength = computed(() => {
    const title = this.infoForm.get('title')?.value;
    return title ? title.length : 0;
  });

  protected selectedAmenityNames = computed(() =>
    this.amenities()
      .filter((a) => a.selected)
      .map((a) => a.name)
  );

  protected selectedAmenityLabels = computed(() =>
    this.amenities()
      .filter((a) => a.selected)
      .map((a) => a.label)
  );

  protected photoCount = computed(() => this.photos().length);
  protected totalPhotoCount = computed(() => this.photos().length + this.pendingPhotos().length);
  protected canSubmit = computed(() => this.photoCount() >= 3 && this.photoCount() <= 10);

  protected distanceToCampus = computed(() => {
    const lat = this.locationForm.get('latitude')?.value ?? CAMPUS_LAT;
    const lng = this.locationForm.get('longitude')?.value ?? CAMPUS_LNG;
    return this.haversineDistance(lat, lng, CAMPUS_LAT, CAMPUS_LNG);
  });

  ngOnInit(): void {
    this.infoForm.get('description')?.valueChanges.subscribe(() => {});
    this.infoForm.get('title')?.valueChanges.subscribe(() => {});
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
    this.pendingPhotos().forEach((p) => URL.revokeObjectURL(p.localUrl));
  }

  protected initMap(): void {
    if (this.map) return;

    setTimeout(() => {
      const mapContainer = document.getElementById('location-map');
      if (!mapContainer) return;

      this.map = L.map('location-map').setView([CAMPUS_LAT, CAMPUS_LNG], 15);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap',
      }).addTo(this.map);

      const campusIcon = L.divIcon({
        className: 'campus-marker',
        html: '<i class="pi pi-building" style="font-size:1.5rem;color:#6366f1"></i>',
        iconSize: [30, 30],
        iconAnchor: [15, 15],
      });

      L.marker([CAMPUS_LAT, CAMPUS_LNG], { icon: campusIcon })
        .addTo(this.map)
        .bindPopup('IUSJC Campus');

      this.marker = L.marker([CAMPUS_LAT, CAMPUS_LNG], { draggable: true }).addTo(this.map);

      this.marker.on('dragend', () => {
        const pos = this.marker!.getLatLng();
        this.locationForm.patchValue({ latitude: pos.lat, longitude: pos.lng });
      });

      this.map.on('click', (e: L.LeafletMouseEvent) => {
        this.marker!.setLatLng(e.latlng);
        this.locationForm.patchValue({ latitude: e.latlng.lat, longitude: e.latlng.lng });
      });

      this.map.invalidateSize();
    }, 100);
  }

  protected toggleAmenity(index: number): void {
    this.amenities.update((list) =>
      list.map((a, i) => (i === index ? { ...a, selected: !a.selected } : a))
    );
  }

  protected onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const currentListing = this.listing();
    if (!currentListing) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'Veuillez d\'abord sauvegarder le brouillon avant d\'ajouter des photos.',
      });
      return;
    }

    const files = Array.from(input.files);
    const remaining = 10 - this.totalPhotoCount();
    const filesToUpload = files.slice(0, remaining);

    if (filesToUpload.length === 0) {
      input.value = '';
      return;
    }

    const newPending: PendingPhoto[] = filesToUpload.map((file) => ({
      localUrl: URL.createObjectURL(file),
      file,
      uploading: false,
    }));

    this.pendingPhotos.update((list) => [...list, ...newPending]);
    this.uploadingPhotos.set(true);
    this.uploadProgress.set(0);
    this.uploadTotal.set(filesToUpload.length);

    const uploads = filesToUpload.map((file, index) =>
      this.listingService.uploadPhoto(currentListing.id, file).pipe(
        tap((updated) => {
          this.listing.set(updated);
          this.photos.set(updated.photos);
          this.uploadProgress.set(index + 1);

          const pendingUrl = newPending[index].localUrl;
          URL.revokeObjectURL(pendingUrl);
          this.pendingPhotos.update((list) => list.filter((p) => p.localUrl !== pendingUrl));
        })
      )
    );

    concat(...uploads)
      .pipe(
        finalize(() => {
          this.uploadingPhotos.set(false);
          this.uploadProgress.set(0);
          this.uploadTotal.set(0);
        })
      )
      .subscribe({
        error: (err) => {
          this.pendingPhotos.update((list) => {
            list.forEach((p) => URL.revokeObjectURL(p.localUrl));
            return [];
          });
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Echec de l\'upload d\'une ou plusieurs photos.',
          });
        },
      });

    input.value = '';
  }

  protected removePhoto(photo: PhotoResponse): void {
    const currentListing = this.listing();
    if (!currentListing) return;

    this.listingService.deletePhoto(currentListing.id, photo.id).subscribe({
      next: () => {
        this.photos.update((list) => list.filter((p) => p.id !== photo.id));
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Echec de la suppression de la photo.',
        });
      },
    });
  }

  protected saveDraft(): void {
    this.saving.set(true);
    const request = this.buildRequest();
    const currentListing = this.listing();

    const save$ = currentListing
      ? this.listingService.updateListing(currentListing.id, request)
      : this.listingService.createListing(request);

    save$.subscribe({
      next: (saved) => {
        this.listing.set(saved);
        this.photos.set(saved.photos);
        this.saving.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Sauvegarde',
          detail: 'Brouillon sauvegarde avec succes.',
        });
      },
      error: () => {
        this.saving.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Echec de la sauvegarde.',
        });
      },
    });
  }

  protected submitForReview(): void {
    const currentListing = this.listing();
    if (!currentListing) return;

    this.submitting.set(true);

    const request = this.buildRequest();
    this.listingService.updateListing(currentListing.id, request).subscribe({
      next: (updated) => {
        this.listingService.submitForReview(updated.id).subscribe({
          next: () => {
            this.submitting.set(false);
            this.messageService.add({
              severity: 'success',
              summary: 'Soumise',
              detail: 'Votre annonce a ete soumise pour validation.',
            });
            this.router.navigate(['/my-listings']);
          },
          error: () => {
            this.submitting.set(false);
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Echec de la soumission. Verifiez les champs requis.',
            });
          },
        });
      },
      error: () => {
        this.submitting.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Echec de la sauvegarde avant soumission.',
        });
      },
    });
  }

  protected formatPrice(price: number | null): string {
    if (!price) return '—';
    return new Intl.NumberFormat('fr-FR').format(price) + ' FCFA';
  }

  protected roomTypeLabel(value: RoomType | null): string {
    const found = this.roomTypeOptions.find((o) => o.value === value);
    return found ? found.label : '—';
  }

  private buildRequest(): CreateListingRequest {
    const info = this.infoForm.value;
    const loc = this.locationForm.value;

    return {
      title: info.title ?? undefined,
      description: info.description ?? undefined,
      roomType: info.roomType ?? undefined,
      monthlyPrice: info.monthlyPrice ?? undefined,
      availableRooms: info.availableRooms ?? undefined,
      availableDate: info.availableDate ? this.formatDate(info.availableDate) : undefined,
      location: loc.address
        ? {
            address: loc.address!,
            latitude: loc.latitude!,
            longitude: loc.longitude!,
            neighborhood: loc.neighborhood ?? undefined,
            city: loc.city ?? undefined,
          }
        : undefined,
      amenityNames: this.selectedAmenityNames(),
    };
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  private haversineDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371;
    const dLat = this.toRad(lat2 - lat1);
    const dLon = this.toRad(lon2 - lon1);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.toRad(lat1)) * Math.cos(this.toRad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return Math.round(R * c * 100) / 100;
  }

  private toRad(deg: number): number {
    return (deg * Math.PI) / 180;
  }
}
