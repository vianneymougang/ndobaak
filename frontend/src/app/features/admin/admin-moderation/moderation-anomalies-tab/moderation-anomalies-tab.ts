import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { AdminTransaction } from '../../../../core/models/admin.models';
import { AdminEmptyState } from '../../shared/admin-empty-state/admin-empty-state';
import { AdminErrorState } from '../../shared/admin-error-state/admin-error-state';
import { RelativeTimePipe } from '../../../../shared/pipes/relative-time-pipe';

@Component({
  selector: 'app-moderation-anomalies-tab',
  imports: [RouterLink, TagModule, SkeletonModule, AdminEmptyState, AdminErrorState, RelativeTimePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './moderation-anomalies-tab.html',
  styleUrl: './moderation-anomalies-tab.scss',
})
export class ModerationAnomaliesTab {
  anomalies = input<AdminTransaction[]>([]);
  loading = input(false);
  error = input(false);

  retry = output<void>();

  protected formatXAF(amount: number): string {
    return new Intl.NumberFormat('fr-FR').format(amount) + ' XAF';
  }

  protected formatOperator(op?: string): string {
    if (!op) return '';
    if (op.includes('MTN')) return 'MTN MoMo';
    if (op.includes('ORANGE')) return 'Orange Money';
    return op;
  }

  protected statusSeverity(status: string): 'danger' | 'warn' | 'info' | 'success' {
    if (status === 'FAILED') return 'danger';
    if (status === 'PENDING') return 'warn';
    if (status === 'COMPLETED') return 'success';
    return 'info';
  }
}
