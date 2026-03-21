import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  OnInit,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { SkeletonModule } from 'primeng/skeleton';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { MessageService } from 'primeng/api';
import { PaymentService } from '../../../core/services/payment';
import { ReceiptResponse } from '../../../core/models/payment.models';

@Component({
  selector: 'app-payment-receipt',
  imports: [RouterLink, ButtonModule, DividerModule, SkeletonModule, NgmMotionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './payment-receipt.html',
  styleUrl: './payment-receipt.scss',
})
export class PaymentReceipt implements OnInit {
  private route = inject(ActivatedRoute);
  private paymentService = inject(PaymentService);
  private messageService = inject(MessageService);

  protected receipt = signal<ReceiptResponse | null>(null);
  protected loading = signal(true);

  protected formattedPaymentDate = computed(() => {
    const r = this.receipt();
    return r
      ? new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long', timeStyle: 'short' }).format(
          new Date(r.paymentDate),
        )
      : '';
  });

  protected formattedBookingDates = computed(() => {
    const r = this.receipt();
    if (!r) return '';
    const fmt = new Intl.DateTimeFormat('fr-FR');
    return `${fmt.format(new Date(r.bookingStartDate))} → ${fmt.format(new Date(r.bookingEndDate))}`;
  });

  protected formattedRent = computed(() => {
    const r = this.receipt();
    return r ? new Intl.NumberFormat('fr-FR').format(r.rentAmount) : '—';
  });

  protected formattedCommission = computed(() => {
    const r = this.receipt();
    return r ? new Intl.NumberFormat('fr-FR').format(r.commission) : '—';
  });

  protected formattedTotal = computed(() => {
    const r = this.receipt();
    return r ? new Intl.NumberFormat('fr-FR').format(r.totalCharged) : '—';
  });

  protected operatorLabel = computed(() => {
    const r = this.receipt();
    if (!r) return '';
    return r.operator === 'MTN_MOMO' ? 'MTN Mobile Money' : 'Orange Money';
  });

  protected paymentTypeLabel = computed(() => {
    const r = this.receipt();
    if (!r) return '';
    return r.paymentType === 'INTEGRAL' ? 'Paiement intégral' : 'Paiement échelonné';
  });

  ngOnInit(): void {
    const paymentId = Number(this.route.snapshot.paramMap.get('paymentId'));
    this.paymentService.getReceipt(paymentId).subscribe({
      next: (receipt) => {
        this.receipt.set(receipt);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Impossible de charger le reçu.',
        });
      },
    });
  }

  protected print(): void {
    window.print();
  }
}
