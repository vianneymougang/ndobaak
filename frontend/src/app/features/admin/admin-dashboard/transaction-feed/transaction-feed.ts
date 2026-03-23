import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

import { AdminTransaction } from '../../../../core/models/admin.models';
import { SectionHeader } from '../../../../shared/components/section-header/section-header';
import { RelativeTimePipe } from '../../../../shared/pipes/relative-time-pipe';

@Component({
  selector: 'app-transaction-feed',
  imports: [RouterLink, SkeletonModule, ButtonModule, TagModule, SectionHeader, RelativeTimePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './transaction-feed.html',
  styleUrl: './transaction-feed.scss',
})
export class TransactionFeed {
  transactions = input<AdminTransaction[]>([]);
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

  protected statusSeverity(status: string): 'success' | 'warn' | 'danger' | 'info' {
    switch (status) {
      case 'COMPLETED':
        return 'success';
      case 'PENDING':
      case 'PROCESSING':
        return 'warn';
      case 'FAILED':
        return 'danger';
      default:
        return 'info';
    }
  }

  protected statusLabel(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'Terminé';
      case 'PENDING':
        return 'En attente';
      case 'PROCESSING':
        return 'En cours';
      case 'FAILED':
        return 'Échoué';
      default:
        return status;
    }
  }

  protected typeLabel(type: string): string {
    return type === 'SUBSCRIPTION' ? 'Abonnement' : 'Réservation';
  }
}
