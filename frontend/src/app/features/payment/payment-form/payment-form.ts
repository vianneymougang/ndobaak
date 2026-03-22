import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  OnInit,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { InputMaskModule } from 'primeng/inputmask';
import { FloatLabelModule } from 'primeng/floatlabel';
import { DividerModule } from 'primeng/divider';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageModule } from 'primeng/message';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { MessageService } from 'primeng/api';
import { BookingService } from '../../../core/services/booking';
import { PaymentService } from '../../../core/services/payment';
import { BookingResponse } from '../../../core/models/booking.models';
import { MobileMoneyOperator } from '../../../core/models/payment.models';

@Component({
  selector: 'app-payment-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    SelectModule,
    InputMaskModule,
    FloatLabelModule,
    DividerModule,
    SkeletonModule,
    MessageModule,
    NgmMotionDirective,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './payment-form.html',
  styleUrl: './payment-form.scss',
})
export class PaymentForm implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private bookingService = inject(BookingService);
  private paymentService = inject(PaymentService);
  private messageService = inject(MessageService);

  protected booking = signal<BookingResponse | null>(null);
  protected loading = signal(true);
  protected submitting = signal(false);
  protected apiError = signal<string | null>(null);

  protected readonly BLOCK_THRESHOLD = 500_000;

  protected readonly operatorOptions: { label: string; value: MobileMoneyOperator }[] = [
    { label: 'MTN Mobile Money', value: 'MTN_MOMO' },
    { label: 'Orange Money', value: 'ORANGE_MONEY' },
  ];

  protected form = this.fb.nonNullable.group({
    operator: [null as MobileMoneyOperator | null, Validators.required],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^6[0-9]{8}$/)]],
  });

  protected needsSplitPayment = computed(() => {
    const b = this.booking();
    return b ? b.totalAmount > this.BLOCK_THRESHOLD : false;
  });

  protected blockCount = computed(() => {
    const b = this.booking();
    return b ? Math.ceil(b.totalAmount / this.BLOCK_THRESHOLD) : 0;
  });

  protected formattedMonthlyRent = computed(() => {
    const b = this.booking();
    return b ? new Intl.NumberFormat('fr-FR').format(b.monthlyRent) : '—';
  });

  protected formattedTotal = computed(() => {
    const b = this.booking();
    return b ? new Intl.NumberFormat('fr-FR').format(b.totalAmount) : '—';
  });

  protected formattedStartDate = computed(() => {
    const b = this.booking();
    return b ? new Intl.DateTimeFormat('fr-FR').format(new Date(b.startDate)) : '';
  });

  protected formattedEndDate = computed(() => {
    const b = this.booking();
    return b ? new Intl.DateTimeFormat('fr-FR').format(new Date(b.endDate)) : '';
  });

  ngOnInit(): void {
    const bookingId = Number(this.route.snapshot.paramMap.get('bookingId'));
    this.bookingService.getBookingById(bookingId).subscribe({
      next: (booking) => {
        this.booking.set(booking);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Impossible de charger les détails de la réservation.',
        });
      },
    });
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      document.querySelector('.ng-invalid')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }

    const booking = this.booking();
    if (!booking) return;

    this.submitting.set(true);
    this.apiError.set(null);

    const { operator, phoneNumber } = this.form.getRawValue();
    this.paymentService
      .initiatePayment({
        bookingId: booking.id,
        operator: operator!,
        phoneNumber: `237${phoneNumber}`,
      })
      .subscribe({
        next: (payment) => {
          this.submitting.set(false);
          this.router.navigate(['/bookings', booking.id, 'pay', 'processing'], {
            queryParams: { paymentId: payment.id },
          });
        },
        error: (err) => {
          this.submitting.set(false);
          const message =
            err?.error?.message ?? 'Impossible d\'initier le paiement. Veuillez réessayer.';
          this.apiError.set(message);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur de paiement',
            detail: message,
          });
        },
      });
  }
}
