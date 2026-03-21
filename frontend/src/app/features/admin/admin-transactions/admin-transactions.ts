import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { SelectModule } from 'primeng/select';
import { TooltipModule } from 'primeng/tooltip';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { PaginatorModule } from 'primeng/paginator';
import { AdminService } from '../../../core/services/admin';
import { AdminTransaction, AdminPage } from '../../../core/models/admin.models';

@Component({
  selector: 'app-admin-transactions',
  imports: [
    DatePipe, DecimalPipe,
    TableModule, TagModule, ButtonModule, SkeletonModule,
    SelectModule, IconFieldModule, InputIconModule, PaginatorModule, TooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-transactions.html',
  styleUrl: './admin-transactions.scss',
})
export class AdminTransactions implements OnInit {
  private adminService = inject(AdminService);

  protected page = signal<AdminPage<AdminTransaction> | null>(null);
  protected loading = signal(true);
  protected currentPage = signal(0);
  protected statusFilter = signal('');
  protected typeFilter = signal('');
  protected readonly skeletonRows = Array(10).fill({});

  protected statusOptions = [
    { label: 'Tous les statuts', value: '' },
    { label: 'Réussi', value: 'SUCCESS' },
    { label: 'En attente', value: 'PENDING' },
    { label: 'Échoué', value: 'FAILED' },
  ];

  protected typeOptions = [
    { label: 'Tous les types', value: '' },
    { label: 'Réservation', value: 'BOOKING' },
    { label: 'Abonnement', value: 'SUBSCRIPTION' },
  ];

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.adminService
      .getTransactions(
        this.statusFilter() || undefined,
        this.typeFilter() || undefined,
        this.currentPage(),
      )
      .subscribe({
        next: (data) => { this.page.set(data); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
  }

  protected onStatusChange(value: string): void {
    this.statusFilter.set(value);
    this.currentPage.set(0);
    this.load();
  }

  protected onTypeChange(value: string): void {
    this.typeFilter.set(value);
    this.currentPage.set(0);
    this.load();
  }

  protected onPageChange(event: { page?: number }): void {
    this.currentPage.set(event.page ?? 0);
    this.load();
  }

  protected statusSeverity(status: string): 'success' | 'warn' | 'danger' | 'secondary' {
    if (status === 'SUCCESS') return 'success';
    if (status === 'PENDING') return 'warn';
    if (status === 'FAILED') return 'danger';
    return 'secondary';
  }
}
