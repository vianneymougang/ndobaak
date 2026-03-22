import { Component, ChangeDetectionStrategy, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TabsModule } from 'primeng/tabs';
import { MessageService } from 'primeng/api';
import { AdminService } from '../../../core/services/admin';
import { AdminReview } from '../../../core/models/admin.models';

type ReviewQueue = 'pending' | 'flagged' | 'all';

@Component({
  selector: 'app-admin-reviews',
  imports: [DatePipe, TableModule, TagModule, ButtonModule, SkeletonModule, TabsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-reviews.html',
  styleUrl: './admin-reviews.scss',
})
export class AdminReviews implements OnInit {
  private adminService = inject(AdminService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);

  protected reviews = signal<AdminReview[]>([]);
  protected loading = signal(true);
  protected actionLoading = signal(false);
  protected activeQueue = signal<ReviewQueue>('pending');
  protected readonly skeletonRows = Array(8).fill({});

  ngOnInit(): void {
    this.load('pending');
  }

  protected onQueueChange(value: string | number | undefined): void {
    const q = (value as ReviewQueue) ?? 'pending';
    this.activeQueue.set(q);
    this.load(q);
  }

  private load(queue: ReviewQueue): void {
    this.loading.set(true);
    this.adminService.getReviews(queue).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => { this.reviews.set(data.content); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected approve(id: number): void {
    this.actionLoading.set(true);
    this.adminService.approveReview(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Avis approuvé', life: 3000 });
        this.load(this.activeQueue());
        this.actionLoading.set(false);
      },
      error: () => this.actionLoading.set(false),
    });
  }

  protected reject(id: number): void {
    this.actionLoading.set(true);
    this.adminService.rejectReview(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.messageService.add({ severity: 'info', summary: 'Avis rejeté', life: 3000 });
        this.load(this.activeQueue());
        this.actionLoading.set(false);
      },
      error: () => this.actionLoading.set(false),
    });
  }

  protected ratingSeverity(rating: number): 'success' | 'warn' | 'danger' {
    return rating >= 4 ? 'success' : rating >= 3 ? 'warn' : 'danger';
  }
}
