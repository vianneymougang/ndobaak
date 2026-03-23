import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  effect,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { PaginatorModule } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { SelectModule } from 'primeng/select';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { MessageService } from 'primeng/api';
import { PaymentService } from '../../../core/services/payment';
import { PaymentResponse, PaymentStatus } from '../../../core/models/payment.models';

@Component({
  selector: 'app-transaction-history',
  imports: [
    RouterLink,
    TagModule,
    ButtonModule,
    PaginatorModule,
    SkeletonModule,
    SelectModule,
    NgmMotionDirective,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './transaction-history.html',
  styleUrl: './transaction-history.scss',
})
export class TransactionHistory {
  private paymentService = inject(PaymentService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);

  protected payments = signal<PaymentResponse[]>([]);
  protected loading = signal(true);
  protected page = signal(0);
  protected totalElements = signal(0);
  protected readonly pageSize = 20;

  readonly skeletonItems = Array(5).fill(0);

  protected readonly statusLabels: Record<PaymentStatus, string> = {
    PENDING: 'En attente',
    PROCESSING: 'En cours',
    COMPLETED: 'Terminé',
    FAILED: 'Échoué',
    REFUNDED: 'Remboursé',
  };

  protected readonly statusSeverities: Record<
    PaymentStatus,
    'warn' | 'info' | 'success' | 'danger' | 'secondary'
  > = {
    PENDING: 'warn',
    PROCESSING: 'info',
    COMPLETED: 'success',
    FAILED: 'danger',
    REFUNDED: 'secondary',
  };

  constructor() {
    effect(() => {
      const p = this.page();
      this.loadPayments(p);
    });
  }

  private loadPayments(page: number): void {
    this.loading.set(true);
    this.paymentService.getPaymentHistory(page, this.pageSize).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => {
        this.payments.set(res.content);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Impossible de charger l\'historique des transactions.',
        });
      },
    });
  }

  protected onPageChange(event: { page?: number }): void {
    this.page.set(event.page ?? 0);
  }

  protected formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-FR').format(amount);
  }

  protected formatDate(dateStr: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    }).format(new Date(dateStr));
  }

  protected operatorLabel(operator: string): string {
    return operator === 'MTN_MOMO' ? 'MTN MoMo' : 'Orange Money';
  }

  protected staggerDelay(i: number): number {
    return Math.min(i, 10) * 0.05;
  }
}
