import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  computed,
  signal,
  inject,
  effect,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { DrawerModule } from 'primeng/drawer';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { GalleriaModule } from 'primeng/galleria';
import { SkeletonModule } from 'primeng/skeleton';
import { ChipModule } from 'primeng/chip';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { FloatLabelModule } from 'primeng/floatlabel';
import { TextareaModule } from 'primeng/textarea';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AdminErrorState } from '../admin-error-state/admin-error-state';
import { AdminService } from '../../../../core/services/admin';
import { ListingResponse, ListingStatus } from '../../../../core/models/listing.models';
import { RejectionMotif, REJECTION_MOTIF_LABELS } from '../../../../core/models/admin.models';

@Component({
  selector: 'app-listing-detail-drawer',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    DrawerModule,
    ButtonModule,
    TagModule,
    GalleriaModule,
    SkeletonModule,
    ChipModule,
    DialogModule,
    SelectModule,
    FloatLabelModule,
    TextareaModule,
    AdminErrorState,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './listing-detail-drawer.html',
  styleUrl: './listing-detail-drawer.scss',
})
export class ListingDetailDrawer {
  private adminService = inject(AdminService);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  listingId = input<number | null>(null);
  showActions = input(true);

  validated = output<number>();
  rejected = output<number>();
  closed = output<void>();

  protected visible = computed(() => this.listingId() !== null);
  protected listing = signal<ListingResponse | null>(null);
  protected loading = signal(false);
  protected error = signal(false);
  protected actionLoading = signal(false);
  protected rejectDialogVisible = signal(false);
  protected activePhotoIndex = signal(0);

  protected isPending = computed(() => this.listing()?.status === 'PENDING_REVIEW');

  protected statusSeverity = computed<'success' | 'warn' | 'danger' | 'info'>(() => {
    const status = this.listing()?.status;
    if (status === 'PUBLISHED') return 'success';
    if (status === 'PENDING_REVIEW') return 'warn';
    if (status === 'REJECTED') return 'danger';
    return 'info';
  });

  protected statusLabel = computed(() => {
    const status = this.listing()?.status;
    if (status === 'PUBLISHED') return 'Publiée';
    if (status === 'PENDING_REVIEW') return 'En attente';
    if (status === 'REJECTED') return 'Refusée';
    if (status === 'DRAFT') return 'Brouillon';
    if (status === 'ARCHIVED') return 'Archivée';
    return status ?? '';
  });

  protected roomTypeLabel = computed(() => {
    const type = this.listing()?.roomType;
    if (type === 'STUDIO') return 'Studio';
    if (type === 'SINGLE_ROOM') return 'Chambre simple';
    if (type === 'DOUBLE_ROOM') return 'Chambre double';
    if (type === 'APARTMENT') return 'Appartement';
    return type ?? '';
  });

  protected motifOptions = Object.entries(REJECTION_MOTIF_LABELS).map(([value, label]) => ({
    label,
    value: value as RejectionMotif,
  }));

  protected rejectForm = this.fb.group({
    motif: [null as RejectionMotif | null, Validators.required],
    additionalReason: [''],
  });

  constructor() {
    effect(() => {
      const id = this.listingId();
      if (id !== null) {
        this.loadListing(id);
      } else {
        this.listing.set(null);
      }
    });
  }

  protected onHide(): void {
    this.closed.emit();
  }

  protected formatXAF(amount: number): string {
    return new Intl.NumberFormat('fr-FR').format(amount) + ' XAF';
  }

  protected validate(): void {
    const id = this.listing()?.id;
    if (!id) return;
    this.actionLoading.set(true);
    this.adminService.validateListing(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (updated) => {
        this.listing.set(updated);
        this.actionLoading.set(false);
        this.validated.emit(id);
      },
      error: () => this.actionLoading.set(false),
    });
  }

  protected openRejectDialog(): void {
    this.rejectForm.reset();
    this.rejectDialogVisible.set(true);
  }

  protected submitReject(): void {
    if (this.rejectForm.invalid) {
      this.rejectForm.markAllAsTouched();
      return;
    }
    const id = this.listing()?.id;
    if (!id) return;
    this.actionLoading.set(true);
    this.adminService
      .rejectListing(id, {
        motif: this.rejectForm.value.motif!,
        additionalReason: this.rejectForm.value.additionalReason ?? undefined,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.listing.set(updated);
          this.rejectDialogVisible.set(false);
          this.actionLoading.set(false);
          this.rejected.emit(id);
        },
        error: () => this.actionLoading.set(false),
      });
  }

  protected retryLoad(): void {
    const id = this.listingId();
    if (id !== null) this.loadListing(id);
  }

  private loadListing(id: number): void {
    this.loading.set(true);
    this.error.set(false);
    this.adminService.getListingDetail(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.listing.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
