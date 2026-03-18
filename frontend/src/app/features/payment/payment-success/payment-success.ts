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
  selector: 'app-payment-success',
  imports: [RouterLink, ButtonModule, DividerModule, SkeletonModule, NgmMotionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './payment-success.html',
  styleUrl: './payment-success.scss',
})
export class PaymentSuccess implements OnInit {
  private route = inject(ActivatedRoute);
  private paymentService = inject(PaymentService);
  private messageService = inject(MessageService);

  protected receipt = signal<ReceiptResponse | null>(null);
  protected loading = signal(true);
  protected paymentId = 0;

  protected formattedAmount = computed(() => {
    const r = this.receipt();
    return r ? new Intl.NumberFormat('fr-FR').format(r.totalCharged) : '—';
  });

  protected formattedDate = computed(() => {
    const r = this.receipt();
    return r
      ? new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long', timeStyle: 'short' }).format(
          new Date(r.paymentDate),
        )
      : '';
  });

  ngOnInit(): void {
    this.paymentId = Number(this.route.snapshot.paramMap.get('paymentId'));
    this.paymentService.getReceipt(this.paymentId).subscribe({
      next: (receipt) => {
        this.receipt.set(receipt);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Impossible de charger les détails du paiement.',
        });
      },
    });
  }
}
