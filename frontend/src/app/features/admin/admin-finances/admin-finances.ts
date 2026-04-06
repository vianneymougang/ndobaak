import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { AdminService } from '../../../core/services/admin';
import { AdminTransaction, GlobalStats } from '../../../core/models/admin.models';
import { AdminEmptyState } from '../shared/admin-empty-state/admin-empty-state';
import { AdminErrorState } from '../shared/admin-error-state/admin-error-state';
import { FinanceChartHeader } from './finance-chart-header/finance-chart-header';
import { RelativeTimePipe } from '../../../shared/pipes/relative-time-pipe';

@Component({
  selector: 'app-admin-finances',
  imports: [
    DatePipe,
    FormsModule,
    TableModule,
    TagModule,
    ButtonModule,
    SelectModule,
    PaginatorModule,
    SkeletonModule,
    AdminEmptyState,
    AdminErrorState,
    FinanceChartHeader,
    RelativeTimePipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-finances.html',
  styleUrl: './admin-finances.scss',
})
export class AdminFinances implements OnInit {
  private adminService = inject(AdminService);
  private destroyRef = inject(DestroyRef);

  protected stats = signal<GlobalStats | null>(null);
  protected loadingStats = signal(true);
  protected errorStats = signal(false);

  protected anomalies = signal<AdminTransaction[]>([]);
  protected loadingAnomalies = signal(true);
  protected errorAnomalies = signal(false);

  protected transactions = signal<AdminTransaction[]>([]);
  protected totalTransactions = signal(0);
  protected loadingTransactions = signal(true);
  protected errorTransactions = signal(false);

  protected statusFilter = signal<string | null>(null);
  protected typeFilter = signal<string | null>(null);
  protected page = signal(0);
  protected pageSize = 20;

  protected statusOptions = [
    { label: 'Tous les statuts', value: null },
    { label: 'Complétés', value: 'COMPLETED' },
    { label: 'En attente', value: 'PENDING' },
    { label: 'Échoués', value: 'FAILED' },
  ];

  protected typeOptions = [
    { label: 'Tous les types', value: null },
    { label: 'Réservation', value: 'BOOKING' },
    { label: 'Abonnement', value: 'SUBSCRIPTION' },
  ];

  ngOnInit(): void {
    this.loadStats();
    this.loadAnomalies();
    this.loadTransactions();
  }

  protected onStatusFilterChange(status: string | null): void {
    this.statusFilter.set(status);
    this.page.set(0);
    this.loadTransactions();
  }

  protected onTypeFilterChange(type: string | null): void {
    this.typeFilter.set(type);
    this.page.set(0);
    this.loadTransactions();
  }

  protected onPageChange(event: PaginatorState): void {
    this.page.set(event.page ?? 0);
    this.loadTransactions();
  }

  protected retryAnomalies(): void {
    this.loadAnomalies();
  }

  protected retryTransactions(): void {
    this.loadTransactions();
  }

  protected formatXAF(amount: number): string {
    return new Intl.NumberFormat('fr-FR').format(amount) + ' XAF';
  }

  protected formatOperator(op?: string): string {
    if (!op) return '—';
    if (op.includes('MTN')) return 'MTN MoMo';
    if (op.includes('ORANGE')) return 'Orange Money';
    return op;
  }

  protected statusSeverity(status: string): 'success' | 'danger' | 'warn' | 'info' {
    if (status === 'COMPLETED') return 'success';
    if (status === 'FAILED') return 'danger';
    if (status === 'PENDING') return 'warn';
    return 'info';
  }

  protected statusLabel(status: string): string {
    if (status === 'COMPLETED') return 'Complété';
    if (status === 'FAILED') return 'Échoué';
    if (status === 'PENDING') return 'En attente';
    if (status === 'PROCESSING') return 'En cours';
    return status;
  }

  protected typeSeverity(type: string): 'info' | 'warn' {
    return type === 'BOOKING' ? 'info' : 'warn';
  }

  protected typeLabel(type: string): string {
    return type === 'BOOKING' ? 'Réservation' : 'Abonnement';
  }

  protected retryStats(): void {
    this.loadStats();
  }

  private loadStats(): void {
    this.loadingStats.set(true);
    this.errorStats.set(false);
    this.adminService.getGlobalStats().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.stats.set(data);
        this.loadingStats.set(false);
      },
      error: () => {
        this.errorStats.set(true);
        this.loadingStats.set(false);
      },
    });
  }

  private loadAnomalies(): void {
    this.loadingAnomalies.set(true);
    this.errorAnomalies.set(false);
    this.adminService.getAnomalies().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.anomalies.set(data);
        this.loadingAnomalies.set(false);
      },
      error: () => {
        this.errorAnomalies.set(true);
        this.loadingAnomalies.set(false);
      },
    });
  }

  private loadTransactions(): void {
    this.loadingTransactions.set(true);
    this.errorTransactions.set(false);
    const status = this.statusFilter() ?? undefined;
    const type = this.typeFilter() ?? undefined;

    this.adminService.getTransactions(status, type, this.page(), this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.transactions.set(page.content);
          this.totalTransactions.set(page.totalElements);
          this.loadingTransactions.set(false);
        },
        error: () => {
          this.errorTransactions.set(true);
          this.loadingTransactions.set(false);
        },
      });
  }
}
