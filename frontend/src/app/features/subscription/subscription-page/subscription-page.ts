import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  OnInit,
  DestroyRef,
} from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { InputMaskModule } from 'primeng/inputmask';
import { FloatLabelModule } from 'primeng/floatlabel';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { DividerModule } from 'primeng/divider';
import { MessageModule } from 'primeng/message';
import { MessageService } from 'primeng/api';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { SubscriptionService } from '../../../core/services/subscription';
import { Subscription, SubscriptionStatus } from '../../../core/models/subscription.models';
import { MobileMoneyOperator } from '../../../core/models/payment.models';

@Component({
  selector: 'app-subscription-page',
  imports: [
    ReactiveFormsModule,
    DatePipe,
    DecimalPipe,
    ButtonModule,
    DialogModule,
    SelectModule,
    InputMaskModule,
    FloatLabelModule,
    SkeletonModule,
    TagModule,
    DividerModule,
    MessageModule,
    NgmMotionDirective,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './subscription-page.html',
  styleUrl: './subscription-page.scss',
})
export class SubscriptionPage implements OnInit {
  private subscriptionService = inject(SubscriptionService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);
  private fb = inject(FormBuilder);

  protected subscription = signal<Subscription | null | undefined>(undefined);
  protected loading = signal(true);
  protected submitting = signal(false);
  protected cancellingAutoRenew = signal(false);
  protected isRenewal = signal(false);
  protected dialogVisible = false;

  protected readonly operatorOptions: { label: string; value: MobileMoneyOperator }[] = [
    { label: 'MTN Mobile Money', value: 'MTN_MOMO' },
    { label: 'Orange Money', value: 'ORANGE_MONEY' },
  ];

  protected form = this.fb.nonNullable.group({
    operator: [null as MobileMoneyOperator | null, Validators.required],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^6[0-9]{8}$/)]],
  });

  protected isExpiringSoon = computed(() => {
    const sub = this.subscription();
    return sub?.status === 'ACTIVE' && (sub.daysRemaining ?? 0) <= 30;
  });

  protected statusSeverity = computed((): 'success' | 'warn' | 'danger' | 'info' => {
    const status = this.subscription()?.status;
    if (status === 'ACTIVE') return 'success';
    if (status === 'EXPIRED' || status === 'CANCELLED') return 'danger';
    if (status === 'PENDING_PAYMENT') return 'info';
    return 'warn';
  });

  protected statusLabel = computed(() => {
    const status = this.subscription()?.status;
    const labels: Record<SubscriptionStatus, string> = {
      ACTIVE: 'ACTIF',
      EXPIRED: 'EXPIRÉ',
      CANCELLED: 'ANNULÉ',
      PENDING_PAYMENT: 'PAIEMENT EN COURS',
    };
    return status ? labels[status] : '';
  });

  ngOnInit(): void {
    this.loadSubscription();
  }

  protected loadSubscription(): void {
    this.loading.set(true);
    this.subscriptionService
      .getMySubscription()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (sub) => {
          this.subscription.set(sub);
          this.loading.set(false);
        },
        error: () => {
          this.subscription.set(null);
          this.loading.set(false);
        },
      });
  }

  protected openSubscribeDialog(): void {
    this.isRenewal.set(false);
    this.form.reset();
    this.dialogVisible = true;
  }

  protected openRenewDialog(): void {
    this.isRenewal.set(true);
    this.form.reset();
    this.dialogVisible = true;
  }

  protected closeDialog(): void {
    this.dialogVisible = false;
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.submitting.set(true);
    const { operator, phoneNumber } = this.form.getRawValue();
    const request = { operator: operator!, phoneNumber: `237${phoneNumber}` };
    const action$ = this.isRenewal()
      ? this.subscriptionService.renew(request)
      : this.subscriptionService.subscribe(request);

    action$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (sub) => {
        this.submitting.set(false);
        this.dialogVisible = false;
        this.subscription.set(sub);
        this.messageService.add({
          severity: 'success',
          summary: 'Demande envoyée',
          detail: 'Vérifiez votre téléphone pour confirmer le paiement MoMo.',
        });
      },
      error: (err) => {
        this.submitting.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: err?.error?.message ?? 'Impossible de traiter votre demande. Réessayez.',
        });
      },
    });
  }

  protected cancelAutoRenewAction(): void {
    this.cancellingAutoRenew.set(true);
    this.subscriptionService
      .cancelAutoRenew()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.subscription.set(updated);
          this.cancellingAutoRenew.set(false);
          this.messageService.add({
            severity: 'info',
            summary: 'Renouvellement automatique désactivé',
            detail: 'Votre abonnement ne sera pas renouvelé automatiquement.',
          });
        },
        error: () => {
          this.cancellingAutoRenew.set(false);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de modifier le renouvellement automatique.',
          });
        },
      });
  }
}
