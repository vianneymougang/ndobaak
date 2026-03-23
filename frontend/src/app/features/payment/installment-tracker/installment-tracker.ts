import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  OnInit,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ProgressBarModule } from 'primeng/progressbar';
import { DividerModule } from 'primeng/divider';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { FloatLabelModule } from 'primeng/floatlabel';
import { InputMaskModule } from 'primeng/inputmask';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageService } from 'primeng/api';
import { PaymentService } from '../../../core/services/payment';
import {
  InstallmentResponse,
  InstallmentStatus,
  MobileMoneyOperator,
  PaymentScheduleResponse,
} from '../../../core/models/payment.models';

@Component({
  selector: 'app-installment-tracker',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    ButtonModule,
    TagModule,
    ProgressBarModule,
    DividerModule,
    DialogModule,
    SelectModule,
    FloatLabelModule,
    InputMaskModule,
    SkeletonModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './installment-tracker.html',
  styleUrl: './installment-tracker.scss',
})
export class InstallmentTracker implements OnInit {
  private route = inject(ActivatedRoute);
  private paymentService = inject(PaymentService);
  private messageService = inject(MessageService);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  protected schedule = signal<PaymentScheduleResponse | null>(null);
  protected loading = signal(true);
  protected payDialogVisible = signal(false);
  protected payingInstallmentId = signal<number | null>(null);
  protected submitting = signal(false);

  protected readonly operatorOptions: { label: string; value: MobileMoneyOperator }[] = [
    { label: 'MTN Mobile Money', value: 'MTN_MOMO' },
    { label: 'Orange Money', value: 'ORANGE_MONEY' },
  ];

  protected payForm = this.fb.nonNullable.group({
    operator: [null as MobileMoneyOperator | null, Validators.required],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^6[0-9]{8}$/)]],
  });

  protected readonly statusLabels: Record<InstallmentStatus, string> = {
    PENDING: 'À payer',
    PAID: 'Payé',
    OVERDUE: 'En retard',
    CANCELLED: 'Annulé',
  };

  protected readonly statusSeverities: Record<
    InstallmentStatus,
    'warn' | 'success' | 'danger' | 'secondary'
  > = {
    PENDING: 'warn',
    PAID: 'success',
    OVERDUE: 'danger',
    CANCELLED: 'secondary',
  };

  protected progressPercent = computed(() => {
    const s = this.schedule();
    if (!s || s.numberOfInstallments === 0) return 0;
    return Math.round((s.paidInstallments / s.numberOfInstallments) * 100);
  });

  protected formattedPaid = computed(() => {
    const s = this.schedule();
    return s ? new Intl.NumberFormat('fr-FR').format(s.paidAmount) : '—';
  });

  protected formattedRemaining = computed(() => {
    const s = this.schedule();
    return s ? new Intl.NumberFormat('fr-FR').format(s.remainingAmount) : '—';
  });

  protected formattedTotal = computed(() => {
    const s = this.schedule();
    return s ? new Intl.NumberFormat('fr-FR').format(s.totalAmount) : '—';
  });

  protected frequencyLabel = computed(() => {
    const s = this.schedule();
    if (!s) return '';
    return s.frequency === 'MONTHLY' ? 'Mensuel' : 'Trimestriel';
  });

  ngOnInit(): void {
    const bookingId = Number(this.route.snapshot.paramMap.get('bookingId'));
    this.paymentService.getPaymentSchedule(bookingId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (schedule) => {
        this.schedule.set(schedule);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Impossible de charger l\'échéancier.',
        });
      },
    });
  }

  protected formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-FR').format(amount);
  }

  protected formatDate(dateStr: string): string {
    return new Intl.DateTimeFormat('fr-FR').format(new Date(dateStr));
  }

  protected canPay(installment: InstallmentResponse): boolean {
    return installment.status === 'PENDING' || installment.status === 'OVERDUE';
  }

  protected openPayDialog(installmentId: number): void {
    this.payingInstallmentId.set(installmentId);
    this.payForm.reset();
    this.payDialogVisible.set(true);
  }

  protected submitPay(): void {
    this.payForm.markAllAsTouched();
    if (this.payForm.invalid) return;

    const installmentId = this.payingInstallmentId();
    if (!installmentId) return;

    this.submitting.set(true);
    const { operator, phoneNumber } = this.payForm.getRawValue();
    this.paymentService
      .payInstallment(installmentId, { operator: operator!, phoneNumber: `237${phoneNumber}` })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.payDialogVisible.set(false);
          this.messageService.add({
            severity: 'success',
            summary: 'Paiement initié',
            detail: 'Confirmez le paiement sur votre téléphone.',
          });
          const bookingId = Number(this.route.snapshot.paramMap.get('bookingId'));
          this.paymentService.getPaymentSchedule(bookingId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: (schedule) => this.schedule.set(schedule),
          });
        },
        error: (err) => {
          this.submitting.set(false);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: err?.error?.message ?? 'Impossible de procéder au paiement.',
          });
        },
      });
  }

  protected staggerDelay(i: number): number {
    return Math.min(i, 12) * 0.05;
  }
}
