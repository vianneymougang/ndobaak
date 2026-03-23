import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  OnDestroy,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ButtonModule } from 'primeng/button';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { MessageService } from 'primeng/api';
import { PaymentService } from '../../../core/services/payment';

@Component({
  selector: 'app-payment-processing',
  imports: [ProgressSpinnerModule, ButtonModule, NgmMotionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './payment-processing.html',
  styleUrl: './payment-processing.scss',
})
export class PaymentProcessing implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private paymentService = inject(PaymentService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);

  protected timedOut = signal(false);
  protected elapsedSeconds = signal(0);

  private paymentId = 0;
  private bookingId = '';
  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private timeoutTimer: ReturnType<typeof setTimeout> | null = null;
  private tickTimer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.paymentId = Number(this.route.snapshot.queryParamMap.get('paymentId'));
    this.bookingId = this.route.snapshot.paramMap.get('bookingId') ?? '';

    this.startPolling();
    this.startTimeout();
    this.startTick();
  }

  ngOnDestroy(): void {
    this.clearTimers();
  }

  private startPolling(): void {
    this.pollTimer = setInterval(() => this.checkStatus(), 3000);
  }

  private startTimeout(): void {
    this.timeoutTimer = setTimeout(() => {
      this.timedOut.set(true);
    }, 180_000);
  }

  private startTick(): void {
    this.tickTimer = setInterval(() => {
      this.elapsedSeconds.update((v) => v + 1);
    }, 1000);
  }

  private clearTimers(): void {
    if (this.pollTimer) clearInterval(this.pollTimer);
    if (this.timeoutTimer) clearTimeout(this.timeoutTimer);
    if (this.tickTimer) clearInterval(this.tickTimer);
  }

  private checkStatus(): void {
    this.paymentService.getPaymentsByBooking(Number(this.bookingId)).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (payments) => {
        const payment = payments.find((p) => p.id === this.paymentId);
        if (!payment) return;

        if (payment.status === 'COMPLETED') {
          this.clearTimers();
          this.router.navigate(['/payments', this.paymentId, 'success']);
        } else if (payment.status === 'FAILED') {
          this.clearTimers();
          this.router.navigate(['/payments', this.paymentId, 'failure']);
        }
      },
      error: () => {},
    });
  }

  protected retry(): void {
    this.timedOut.set(false);
    this.elapsedSeconds.set(0);
    this.clearTimers();
    this.startPolling();
    this.startTimeout();
    this.startTick();
  }

  protected cancel(): void {
    this.clearTimers();
    this.messageService.add({
      severity: 'info',
      summary: 'Paiement annulé',
      detail: 'Vous pouvez réessayer à tout moment.',
    });
    this.router.navigate(['/my-bookings']);
  }
}
