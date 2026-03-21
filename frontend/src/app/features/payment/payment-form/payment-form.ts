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
import { RadioButtonModule } from 'primeng/radiobutton';
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
import {
  MobileMoneyOperator,
  PaymentType,
  ScheduleFrequency,
} from '../../../core/models/payment.models';

@Component({
  selector: 'app-payment-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    SelectModule,
    RadioButtonModule,
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

  protected readonly operatorOptions: { label: string; value: MobileMoneyOperator }[] = [
    { label: 'MTN Mobile Money', value: 'MTN_MOMO' },
    { label: 'Orange Money', value: 'ORANGE_MONEY' },
  ];

  protected readonly frequencyOptions: { label: string; value: ScheduleFrequency }[] = [
    { label: 'Mensuel', value: 'MONTHLY' },
    { label: 'Trimestriel', value: 'QUARTERLY' },
  ];

  protected form = this.fb.nonNullable.group({
    paymentType: ['INTEGRAL' as PaymentType, Validators.required],
    operator: [null as MobileMoneyOperator | null, Validators.required],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^6[0-9]{8}$/)]],
    installmentFrequency: [null as ScheduleFrequency | null],
  });

  protected isInstallment = signal(false);

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

  protected onPaymentTypeChange(): void {
    const isInst = this.form.controls.paymentType.value === 'INSTALLMENT';
    this.isInstallment.set(isInst);

    const ctrl = this.form.controls.installmentFrequency;
    if (isInst) {
      ctrl.setValidators(Validators.required);
    } else {
      ctrl.clearValidators();
      ctrl.setValue(null);
    }
    ctrl.updateValueAndValidity();
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

    const { paymentType, operator, phoneNumber, installmentFrequency } = this.form.getRawValue();
    this.paymentService
      .initiatePayment({
        bookingId: booking.id,
        paymentType,
        operator: operator!,
        phoneNumber: `237${phoneNumber}`,
        installmentFrequency: installmentFrequency ?? undefined,
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
