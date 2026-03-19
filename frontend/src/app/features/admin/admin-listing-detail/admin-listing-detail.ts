import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
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
  selector: 'app-admin-listing-detail',
  imports: [
    DatePipe, DecimalPipe, RouterLink, ReactiveFormsModule,
    ButtonModule, TagModule, SkeletonModule,
    DialogModule, SelectModule, FloatLabelModule, TextareaModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-listing-detail.html',
  styleUrl: './admin-listing-detail.scss',
})
export class AdminListingDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private adminService = inject(AdminService);
  private messageService = inject(MessageService);
  private fb = inject(FormBuilder);

  protected listing = signal<ListingResponse | null>(null);
  protected loading = signal(true);
  protected actionLoading = signal(false);
  protected rejectDialogVisible = signal(false);

  protected motifOptions = Object.entries(REJECTION_MOTIF_LABELS).map(([value, label]) => ({
    label,
    value: value as RejectionMotif,
  }));

  protected rejectForm = this.fb.group({
    motif: [null as RejectionMotif | null, Validators.required],
    additionalReason: [''],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.adminService.getListingDetail(id).subscribe({
      next: (data) => { this.listing.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected validate(): void {
    const id = this.listing()?.id;
    if (!id) return;
    this.actionLoading.set(true);
    this.adminService.validateListing(id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Annonce approuvée', life: 3000 });
        this.actionLoading.set(false);
      },
      error: () => this.actionLoading.set(false),
    });
  }

  protected openRejectDialog(): void {
    this.rejectForm.reset();
    this.rejectDialogVisible.set(true);
  }

  protected submitReject(): void {
    if (this.rejectForm.invalid) { this.rejectForm.markAllAsTouched(); return; }
    const id = this.listing()?.id;
    if (!id) return;
    this.actionLoading.set(true);
    this.adminService.rejectListing(id, {
      motif: this.rejectForm.value.motif!,
      additionalReason: this.rejectForm.value.additionalReason ?? undefined,
    }).subscribe({
      next: () => {
        this.messageService.add({ severity: 'info', summary: 'Annonce refusée', life: 3000 });
        this.rejectDialogVisible.set(false);
        this.actionLoading.set(false);
      },
      error: () => this.actionLoading.set(false),
    });
  }
}
