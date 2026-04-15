import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { DividerModule } from 'primeng/divider';
import { AuthService } from '../../../core/services/auth';
import {
  LeaseContractResponse,
  LeaseStatus,
  RentalCycleService,
} from '../../../core/services/rental-cycle';

@Component({
  selector: 'app-lease-viewer',
  imports: [
    CommonModule,
    ButtonModule,
    CardModule,
    TagModule,
    SkeletonModule,
    MessageModule,
    ToastModule,
    DividerModule,
  ],
  templateUrl: './lease-viewer.html',
  styleUrl: './lease-viewer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService],
})
export class LeaseViewer implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private rentalCycleService = inject(RentalCycleService);
  private authService = inject(AuthService);
  private messageService = inject(MessageService);

  bookingId = signal<number>(0);
  lease = signal<LeaseContractResponse | null>(null);
  loading = signal(true);
  signingInProgress = signal(false);
  paymentInProgress = signal(false);
  errorMessage = signal<string | null>(null);

  currentUser = this.authService.user;

  canSignAsLandlord = computed(() => {
    const l = this.lease();
    const user = this.currentUser();
    return l?.status === 'AWAITING_LANDLORD_SIGNATURE' && user?.role === 'LANDLORD';
  });

  canSignAsStudent = computed(() => {
    const l = this.lease();
    const user = this.currentUser();
    return l?.status === 'AWAITING_TENANT_SIGNATURE' && user?.role === 'STUDENT';
  });

  canPay = computed(() => {
    const l = this.lease();
    const user = this.currentUser();
    return l?.status === 'FULLY_SIGNED' && user?.role === 'STUDENT';
  });

  statusSeverity = computed((): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' => {
    switch (this.lease()?.status) {
      case 'FULLY_SIGNED':
      case 'TAX_REGISTERED':
        return 'success';
      case 'AWAITING_LANDLORD_SIGNATURE':
      case 'AWAITING_TENANT_SIGNATURE':
        return 'warn';
      case 'TERMINATED':
        return 'danger';
      default:
        return 'info';
    }
  });

  statusLabel = computed(() => {
    const labels: Record<LeaseStatus, string> = {
      DRAFT: 'Brouillon',
      AWAITING_LANDLORD_SIGNATURE: 'Signature propriétaire requise',
      AWAITING_TENANT_SIGNATURE: 'Signature locataire requise',
      FULLY_SIGNED: 'Signé par les deux parties',
      TAX_REGISTERED: 'Enregistré fiscalement',
      TERMINATED: 'Résilié',
    };
    return this.lease()?.status ? labels[this.lease()!.status] : '—';
  });

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('bookingId'));
    this.bookingId.set(id);
    this.loadLease(id);
  }

  retryLoad() {
    this.loadLease(this.bookingId());
  }

  private loadLease(bookingId: number) {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.rentalCycleService.getLease(bookingId).subscribe({
      next: (data) => {
        this.lease.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(
          err?.error?.message ?? 'Impossible de charger le contrat. Vérifiez votre connexion et réessayez.',
        );
        this.loading.set(false);
      },
    });
  }

  openPdf() {
    const url = this.rentalCycleService.getLeasePdfUrl(this.bookingId());
    window.open(url, '_blank');
  }

  sign() {
    this.signingInProgress.set(true);
    this.rentalCycleService.signLease(this.bookingId()).subscribe({
      next: (data) => {
        this.lease.set(data);
        this.signingInProgress.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Contrat signé',
          detail: 'Votre signature a été enregistrée avec succès.',
          life: 4000,
        });
      },
      error: (err) => {
        this.signingInProgress.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: err?.error?.message ?? 'Impossible de signer le contrat. Veuillez réessayer.',
          life: 5000,
        });
      },
    });
  }

  proceedToPayment() {
    this.paymentInProgress.set(true);
    this.rentalCycleService.initiatePayment(this.bookingId()).subscribe({
      next: (response) => {
        this.paymentInProgress.set(false);
        if (response.authorizationUrl) {
          window.location.href = response.authorizationUrl;
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: "Impossible d'initier le paiement. Veuillez réessayer.",
            life: 5000,
          });
        }
      },
      error: (err) => {
        this.paymentInProgress.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: err?.error?.message ?? "Impossible d'initier le paiement. Veuillez réessayer.",
          life: 5000,
        });
      },
    });
  }

  goBack() {
    this.router.navigate(['/my-bookings']);
  }
}
