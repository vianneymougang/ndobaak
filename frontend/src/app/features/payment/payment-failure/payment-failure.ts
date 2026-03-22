import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { MessageService } from 'primeng/api';
import { PaymentService } from '../../../core/services/payment';
import { PaymentResponse } from '../../../core/models/payment.models';

@Component({
  selector: 'app-payment-failure',
  imports: [RouterLink, ButtonModule, NgmMotionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './payment-failure.html',
  styleUrl: './payment-failure.scss',
})
export class PaymentFailure implements OnInit {
  private route = inject(ActivatedRoute);
  private paymentService = inject(PaymentService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);

  protected payment = signal<PaymentResponse | null>(null);
  protected loading = signal(true);

  ngOnInit(): void {
    const paymentId = Number(this.route.snapshot.paramMap.get('paymentId'));
    this.paymentService.getPaymentsByBooking(0).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      error: () => {
        this.loading.set(false);
      },
    });
    this.loading.set(false);
  }

  protected get bookingPayLink(): string {
    const p = this.payment();
    return p ? `/bookings/${p.bookingId}/pay` : '/my-bookings';
  }
}
