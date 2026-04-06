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
import { forkJoin } from 'rxjs';
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';
import { MessageService, ConfirmationService } from 'primeng/api';

import { AdminService } from '../../../core/services/admin';
import { GlobalStats, AdminReview, AdminTransaction } from '../../../core/models/admin.models';
import { ListingResponse } from '../../../core/models/listing.models';
import { PlatformHealth } from './platform-health/platform-health';
import { CommandMetrics } from './command-metrics/command-metrics';
import { AttentionPanel } from './attention-panel/attention-panel';
import { AdminCharts } from './admin-charts/admin-charts';
import { AdminInsights } from './admin-insights/admin-insights';
import { TransactionFeed } from './transaction-feed/transaction-feed';

export interface MonthDelta {
  current: number;
  previous: number;
  percent: number;
}

export interface DashboardInsight {
  icon: string;
  text: string;
  type: 'success' | 'warning' | 'error' | 'info';
}

@Component({
  selector: 'app-admin-dashboard',
  imports: [
    SkeletonModule,
    ButtonModule,

    PlatformHealth,
    CommandMetrics,
    AttentionPanel,
    AdminCharts,
    AdminInsights,
    TransactionFeed,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
})
export class AdminDashboard implements OnInit {
  private adminService = inject(AdminService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private destroyRef = inject(DestroyRef);

  protected stats = signal<GlobalStats | null>(null);
  protected pendingListings = signal<ListingResponse[]>([]);
  protected pendingReviews = signal<AdminReview[]>([]);
  protected anomalies = signal<AdminTransaction[]>([]);
  protected recentTransactions = signal<AdminTransaction[]>([]);

  protected loadingStats = signal(true);
  protected loadingListings = signal(true);
  protected loadingReviews = signal(true);
  protected loadingAnomalies = signal(true);
  protected loadingTransactions = signal(true);

  protected errorStats = signal(false);
  protected errorListings = signal(false);
  protected errorReviews = signal(false);
  protected errorAnomalies = signal(false);
  protected errorTransactions = signal(false);

  protected actionLoading = signal<Record<string, boolean>>({});

  protected healthScore = computed(() => {
    const s = this.stats();
    if (!s) return 0;
    const pendingCount = this.pendingListings().length;
    const anomalyCount = this.anomalies().length;
    const reviewCount = this.pendingReviews().length;

    let score = 100;
    score -= Math.min(pendingCount * 5, 25);
    score -= Math.min(anomalyCount * 15, 30);
    score -= Math.min(reviewCount * 5, 15);

    const revenueDelta = this.revenueDelta();
    if (revenueDelta.percent < -10) score -= 15;
    else if (revenueDelta.percent < 0) score -= 5;

    const userDelta = this.userDelta();
    if (userDelta.percent < -10) score -= 10;
    else if (userDelta.percent < 0) score -= 5;

    return Math.max(0, Math.min(100, score));
  });

  protected healthStatus = computed<'good' | 'warning' | 'critical'>(() => {
    const score = this.healthScore();
    if (score >= 75) return 'good';
    if (score >= 50) return 'warning';
    return 'critical';
  });

  protected userDelta = computed<MonthDelta>(() => this.computeDelta(this.stats()?.monthlyUsers));
  protected bookingDelta = computed<MonthDelta>(() => this.computeDelta(this.stats()?.monthlyBookings));
  protected revenueDelta = computed<MonthDelta>(() => this.computeDelta(this.stats()?.monthlyRevenue));

  protected attentionCount = computed(() => {
    return this.pendingListings().length + this.anomalies().length + this.pendingReviews().length;
  });

  protected failedTransactionCount = computed(() => {
    return this.anomalies().filter((a) => a.status === 'FAILED').length;
  });

  protected urgentListings = computed(() => {
    const now = new Date().getTime();
    const dayMs = 86_400_000;
    return this.pendingListings()
      .map((l) => ({
        ...l,
        waitingDays: Math.floor((now - new Date(l.createdAt).getTime()) / dayMs),
      }))
      .sort((a, b) => b.waitingDays - a.waitingDays);
  });

  protected insights = computed<DashboardInsight[]>(() => {
    const s = this.stats();
    if (!s) return [];
    const result: DashboardInsight[] = [];

    const ud = this.userDelta();
    if (ud.percent > 0) {
      result.push({
        icon: 'pi pi-users',
        text: `Les inscriptions sont en hausse de ${this.formatPercent(ud.percent)} par rapport au mois dernier.`,
        type: 'success',
      });
    } else if (ud.percent < 0) {
      result.push({
        icon: 'pi pi-users',
        text: `Les inscriptions sont en baisse de ${this.formatPercent(Math.abs(ud.percent))} par rapport au mois dernier.`,
        type: 'warning',
      });
    }

    const pending = this.pendingListings().length;
    if (pending > 0) {
      const urgent = this.urgentListings().filter((l) => l.waitingDays >= 2).length;
      const urgentText = urgent > 0 ? ` dont ${urgent} depuis plus de 48h` : '';
      result.push({
        icon: 'pi pi-home',
        text: `${pending} annonce${pending > 1 ? 's' : ''} attend${pending > 1 ? 'ent' : ''} une validation${urgentText}.`,
        type: pending > 5 ? 'warning' : 'info',
      });
    }

    const failedTx = this.anomalies().filter((a) => a.status === 'FAILED');
    if (failedTx.length > 0) {
      const operators = [...new Set(failedTx.map((t) => this.formatOperator(t.operator)))];
      result.push({
        icon: 'pi pi-exclamation-triangle',
        text: `${failedTx.length} transaction${failedTx.length > 1 ? 's ont' : ' a'} échoué${failedTx.length > 1 ? '' : 'e'} — ${operators.join(', ')}.`,
        type: 'error',
      });
    }

    const rd = this.revenueDelta();
    if (rd.current > 0) {
      const direction = rd.percent >= 0 ? 'en hausse' : 'en baisse';
      result.push({
        icon: 'pi pi-wallet',
        text: `Le revenu mensuel a atteint ${this.formatXAF(rd.current)}, ${direction} de ${this.formatPercent(Math.abs(rd.percent))}.`,
        type: rd.percent >= 0 ? 'success' : 'warning',
      });
    }

    if (s.activeSubscriptions > 0 && s.totalLandlords > 0) {
      const rate = (s.activeSubscriptions / s.totalLandlords) * 100;
      result.push({
        icon: 'pi pi-star',
        text: `${s.activeSubscriptions} abonnements actifs sur ${s.totalLandlords} propriétaires inscrits (${this.formatPercent(rate)}).`,
        type: rate > 60 ? 'success' : 'info',
      });
    }

    return result;
  });

  ngOnInit(): void {
    this.loadStats();
    this.loadListings();
    this.loadReviews();
    this.loadAnomalies();
    this.loadTransactions();
  }

  protected loadStats(): void {
    this.loadingStats.set(true);
    this.errorStats.set(false);
    this.adminService
      .getGlobalStats()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.stats.set(data);
          this.loadingStats.set(false);
        },
        error: () => {
          this.errorStats.set(true);
          this.loadingStats.set(false);
        },
      });
  }

  protected loadListings(): void {
    this.loadingListings.set(true);
    this.errorListings.set(false);
    this.adminService
      .getPendingListings()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.pendingListings.set(data);
          this.loadingListings.set(false);
        },
        error: () => {
          this.errorListings.set(true);
          this.loadingListings.set(false);
        },
      });
  }

  protected loadReviews(): void {
    this.loadingReviews.set(true);
    this.errorReviews.set(false);
    this.adminService
      .getReviews('pending', 0, 5)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.pendingReviews.set(data.content);
          this.loadingReviews.set(false);
        },
        error: () => {
          this.errorReviews.set(true);
          this.loadingReviews.set(false);
        },
      });
  }

  protected loadAnomalies(): void {
    this.loadingAnomalies.set(true);
    this.errorAnomalies.set(false);
    this.adminService
      .getAnomalies()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.anomalies.set(data);
          this.loadingAnomalies.set(false);
        },
        error: () => {
          this.errorAnomalies.set(true);
          this.loadingAnomalies.set(false);
        },
      });
  }

  protected loadTransactions(): void {
    this.loadingTransactions.set(true);
    this.errorTransactions.set(false);
    this.adminService
      .getTransactions(undefined, undefined, 0, 10)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.recentTransactions.set(data.content);
          this.loadingTransactions.set(false);
        },
        error: () => {
          this.errorTransactions.set(true);
          this.loadingTransactions.set(false);
        },
      });
  }

  protected onValidateListing(listing: ListingResponse): void {
    const key = `listing-${listing.id}`;
    this.setActionLoading(key, true);
    this.adminService
      .validateListing(listing.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.setActionLoading(key, false);
          this.pendingListings.update((items) => items.filter((l) => l.id !== listing.id));
          this.messageService.add({
            severity: 'success',
            summary: 'Annonce validée',
            detail: `"${listing.title}" a été publiée.`,
            life: 4000,
          });
        },
        error: () => {
          this.setActionLoading(key, false);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de valider cette annonce. Réessayez.',
            life: 4000,
          });
        },
      });
  }

  protected onRejectListing(listing: ListingResponse): void {
    this.confirmationService.confirm({
      header: 'Confirmer le rejet',
      message: `Rejeter l'annonce "${listing.title}" ?`,
      icon: 'pi pi-exclamation-triangle',
      rejectButtonProps: { label: 'Annuler', severity: 'secondary', outlined: true },
      acceptButtonProps: { label: 'Rejeter', severity: 'danger' },
      accept: () => {
        const key = `listing-reject-${listing.id}`;
        this.setActionLoading(key, true);
        this.adminService
          .rejectListing(listing.id, { motif: 'INCOMPLETE_INFORMATION' })
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.setActionLoading(key, false);
              this.pendingListings.update((items) => items.filter((l) => l.id !== listing.id));
              this.messageService.add({
                severity: 'warn',
                summary: 'Annonce rejetée',
                detail: `"${listing.title}" a été rejetée.`,
                life: 4000,
              });
            },
            error: () => {
              this.setActionLoading(key, false);
              this.messageService.add({
                severity: 'error',
                summary: 'Erreur',
                detail: 'Impossible de rejeter cette annonce. Réessayez.',
                life: 4000,
              });
            },
          });
      },
    });
  }

  protected onApproveReview(review: AdminReview): void {
    const key = `review-${review.id}`;
    this.setActionLoading(key, true);
    this.adminService
      .approveReview(review.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.setActionLoading(key, false);
          this.pendingReviews.update((items) => items.filter((r) => r.id !== review.id));
          this.messageService.add({
            severity: 'success',
            summary: 'Avis approuvé',
            detail: `L'avis de ${review.studentName} a été approuvé.`,
            life: 4000,
          });
        },
        error: () => {
          this.setActionLoading(key, false);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: "Impossible d'approuver cet avis. Réessayez.",
            life: 4000,
          });
        },
      });
  }

  protected onRejectReview(review: AdminReview): void {
    const key = `review-reject-${review.id}`;
    this.setActionLoading(key, true);
    this.adminService
      .rejectReview(review.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.setActionLoading(key, false);
          this.pendingReviews.update((items) => items.filter((r) => r.id !== review.id));
          this.messageService.add({
            severity: 'warn',
            summary: 'Avis rejeté',
            detail: `L'avis de ${review.studentName} a été rejeté.`,
            life: 4000,
          });
        },
        error: () => {
          this.setActionLoading(key, false);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de rejeter cet avis. Réessayez.',
            life: 4000,
          });
        },
      });
  }

  protected isActionLoading(key: string): boolean {
    return this.actionLoading()[key] ?? false;
  }

  private setActionLoading(key: string, loading: boolean): void {
    this.actionLoading.update((state) => {
      const next = { ...state };
      if (loading) {
        next[key] = true;
      } else {
        delete next[key];
      }
      return next;
    });
  }

  private computeDelta(points?: { month: string; value: number }[]): MonthDelta {
    if (!points || points.length < 2) return { current: 0, previous: 0, percent: 0 };
    const sorted = [...points].sort((a, b) => a.month.localeCompare(b.month));
    const current = sorted[sorted.length - 1].value;
    const previous = sorted[sorted.length - 2].value;
    const percent = previous > 0 ? ((current - previous) / previous) * 100 : 0;
    return { current, previous, percent };
  }

  protected formatXAF(value: number): string {
    return new Intl.NumberFormat('fr-FR').format(value) + ' XAF';
  }

  protected formatPercent(value: number): string {
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value) + '%';
  }

  private formatOperator(op?: string): string {
    if (!op) return 'Inconnu';
    if (op.includes('MTN')) return 'MTN MoMo';
    if (op.includes('ORANGE')) return 'Orange Money';
    return op;
  }
}
