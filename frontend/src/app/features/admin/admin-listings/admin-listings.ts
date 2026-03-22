import { Component, ChangeDetectionStrategy, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { InputTextModule } from 'primeng/inputtext';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { FloatLabelModule } from 'primeng/floatlabel';
import { TextareaModule } from 'primeng/textarea';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { AdminService } from '../../../core/services/admin';
import { ListingResponse } from '../../../core/models/listing.models';
import { RejectionMotif, REJECTION_MOTIF_LABELS } from '../../../core/models/admin.models';

@Component({
  selector: 'app-admin-listings',
  imports: [
    DatePipe, DecimalPipe, RouterLink, ReactiveFormsModule,
    TableModule, TagModule, ButtonModule, SkeletonModule,
    InputTextModule, IconFieldModule, InputIconModule,
    DialogModule, SelectModule, FloatLabelModule, TextareaModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-listings.html',
  styleUrl: './admin-listings.scss',
})
export class AdminListings implements OnInit {
  private adminService = inject(AdminService);
  private messageService = inject(MessageService);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  protected listings = signal<ListingResponse[]>([]);
  protected loading = signal(true);
  protected rejectDialogVisible = signal(false);
  protected selectedListingId = signal<number | null>(null);
  protected actionLoading = signal(false);
  protected readonly skeletonRows = Array(8).fill({});

  protected motifOptions = Object.entries(REJECTION_MOTIF_LABELS).map(([value, label]) => ({
    label,
    value: value as RejectionMotif,
  }));

  protected rejectForm = this.fb.group({
    motif: [null as RejectionMotif | null, Validators.required],
    additionalReason: [''],
  });

  ngOnInit(): void {
    this.loadListings();
  }

  protected loadListings(): void {
    this.loading.set(true);
    this.adminService.getPendingListings().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => { this.listings.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected validate(listingId: number): void {
    this.actionLoading.set(true);
    this.adminService.validateListing(listingId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Annonce approuvée', life: 3000 });
        this.loadListings();
        this.actionLoading.set(false);
      },
      error: () => this.actionLoading.set(false),
    });
  }

  protected openRejectDialog(listingId: number): void {
    this.selectedListingId.set(listingId);
    this.rejectForm.reset();
    this.rejectDialogVisible.set(true);
  }

  protected submitReject(): void {
    if (this.rejectForm.invalid) { this.rejectForm.markAllAsTouched(); return; }
    const id = this.selectedListingId();
    if (!id) return;
    this.actionLoading.set(true);
    this.adminService.rejectListing(id, {
      motif: this.rejectForm.value.motif!,
      additionalReason: this.rejectForm.value.additionalReason ?? undefined,
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.messageService.add({ severity: 'info', summary: 'Annonce refusée', life: 3000 });
        this.rejectDialogVisible.set(false);
        this.loadListings();
        this.actionLoading.set(false);
      },
      error: () => this.actionLoading.set(false),
    });
  }
}
