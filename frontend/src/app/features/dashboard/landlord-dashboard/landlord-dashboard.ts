import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  OnInit,
  DestroyRef,
  PLATFORM_ID,
} from '@angular/core';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { SelectButtonModule } from 'primeng/selectbutton';
import { ChartModule } from 'primeng/chart';
import { TagModule } from 'primeng/tag';
import { ProgressBarModule } from 'primeng/progressbar';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import type { ChartData, ChartOptions, TooltipItem } from 'chart.js';
import { DashboardService } from '../../../core/services/dashboard';
import { BookingService } from '../../../core/services/booking';
import { SubscriptionService } from '../../../core/services/subscription';
import { Subscription } from '../../../core/models/subscription.models';
import type {
  LandlordDashboard as LandlordDashboardData,
  MonthlyRevenue,
  DailyViews,
} from '../../../core/models/dashboard.models';
import { KpiCard } from '../../../shared/components/kpi-card/kpi-card';
import { SectionHeader } from '../../../shared/components/section-header/section-header';
import {
  ActivityTimeline,
  ActivityItem,
} from '../../../shared/components/activity-timeline/activity-timeline';

interface PeriodOption {
  label: string;
  value: string;
  days: number;
  months: number;
}

@Component({
  selector: 'app-landlord-dashboard',
  imports: [
    RouterLink,
    FormsModule,
    ButtonModule,
    SkeletonModule,
    SelectButtonModule,
    ChartModule,
    TagModule,
    ProgressBarModule,
    TooltipModule,
    NgmMotionDirective,
    KpiCard,
    SectionHeader,
    ActivityTimeline,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './landlord-dashboard.html',
  styleUrl: './landlord-dashboard.scss',
})
export class LandlordDashboard implements OnInit {
  private dashboardService = inject(DashboardService);
  private bookingService = inject(BookingService);
  private subscriptionService = inject(SubscriptionService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);
  private platformId = inject(PLATFORM_ID);
  private doc = inject(DOCUMENT);
  private router = inject(Router);

  protected readonly periodOptions: PeriodOption[] = [
    { label: '7j', value: '7d', days: 7, months: 1 },
    { label: '30j', value: '30d', days: 30, months: 3 },
    { label: '3 mois', value: '3m', days: 90, months: 3 },
    { label: '12 mois', value: '12m', days: 365, months: 12 },
  ];

  protected selectedPeriod = '30d';

  protected dashboard = signal<LandlordDashboardData | null>(null);
  protected subscription = signal<Subscription | null | undefined>(undefined);
  protected kpiLoading = signal(true);
  protected kpiError = signal(false);
  protected chartsLoading = signal(true);
  protected chartsError = signal(false);
  protected subscriptionLoading = signal(true);
  protected activityError = signal(false);
  protected pendingError = signal(false);

  protected revenueChartData = signal<ChartData<'line'> | null>(null);
  protected revenueChartOptions = signal<ChartOptions<'line'> | null>(null);
  protected viewsChartData = signal<ChartData<'line'> | null>(null);
  protected viewsChartOptions = signal<ChartOptions<'line'> | null>(null);

  protected pendingBookings = computed(() => this.dashboard()?.recentPendingBookings ?? []);
  protected pendingCount = computed(() => this.dashboard()?.pendingBookings ?? 0);
  protected activityItems = computed<ActivityItem[]>(() =>
    (this.dashboard()?.recentActivities ?? []).map((a) => ({
      type: a.type,
      title: a.title,
      message: a.message,
      createdAt: a.createdAt,
      referenceId: a.referenceId,
    })),
  );

  protected subscriptionExpiringSoon = computed(() => {
    const sub = this.subscription();
    return sub?.status === 'ACTIVE' && (sub.daysRemaining ?? 0) <= 30;
  });

  protected subscriptionProgress = computed(() => {
    const sub = this.subscription();
    if (!sub || sub.status !== 'ACTIVE') return 0;
    const total = 365;
    return Math.min(Math.round(((total - (sub.daysRemaining ?? 0)) / total) * 100), 100);
  });

  protected subscriptionProgressClass = computed(() => {
    const sub = this.subscription();
    if (!sub) return '';
    if (sub.status !== 'ACTIVE') return 'progress--expired';
    if ((sub.daysRemaining ?? 0) <= 30) return 'progress--warning';
    return 'progress--ok';
  });

  protected progressBarValueColor = computed(() => {
    const sub = this.subscription();
    if (!sub || sub.status !== 'ACTIVE') return 'var(--color-ndb-error)';
    if ((sub.daysRemaining ?? 0) <= 30) return 'var(--color-ndb-warning)';
    return 'var(--color-ndb-gold)';
  });

  protected dismissedBookings = signal<Set<number>>(new Set());
  protected loadingBookings = signal<Set<number>>(new Set());

  protected visibleBookings = computed(() =>
    this.pendingBookings().filter((b) => !this.dismissedBookings().has(b.bookingId)),
  );

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadDashboard();
      this.loadSubscription();
    }
  }

  protected onPeriodChange(): void {
    this.loadDashboard();
  }

  protected retryKpi(): void {
    this.kpiError.set(false);
    this.loadDashboard();
  }

  protected retryCharts(): void {
    this.chartsError.set(false);
    this.chartsLoading.set(true);
    const { months, days } = this.getPeriod();
    this.loadCharts(months, days);
  }

  protected retryActivity(): void {
    this.activityError.set(false);
    this.loadDashboard();
  }

  protected retryPending(): void {
    this.pendingError.set(false);
    this.loadDashboard();
  }

  private loadSubscription(): void {
    this.subscriptionLoading.set(true);
    this.subscriptionService
      .getMySubscription()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (sub) => {
          this.subscription.set(sub);
          this.subscriptionLoading.set(false);
        },
        error: () => {
          this.subscription.set(null);
          this.subscriptionLoading.set(false);
        },
      });
  }

  private getPeriod(): { from: string; to: string; days: number; months: number } {
    const period = this.periodOptions.find((p) => p.value === this.selectedPeriod)!;
    const now = new Date();
    const fromDate = new Date(now);
    fromDate.setDate(fromDate.getDate() - period.days);
    return {
      from: this.toIsoDate(fromDate),
      to: this.toIsoDate(now),
      days: period.days,
      months: period.months,
    };
  }

  private toIsoDate(date: Date): string {
    return date.toISOString().slice(0, 10);
  }

  private loadDashboard(): void {
    const { from, to, days, months } = this.getPeriod();
    this.kpiLoading.set(true);
    this.chartsLoading.set(true);
    this.kpiError.set(false);
    this.chartsError.set(false);

    this.dashboardService
      .getLandlordDashboard(from, to)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data: LandlordDashboardData) => {
          this.dashboard.set(data);
          this.kpiLoading.set(false);
          this.loadCharts(months, days);
        },
        error: () => {
          this.kpiLoading.set(false);
          this.kpiError.set(true);
          this.chartsLoading.set(false);
          this.chartsError.set(true);
          this.activityError.set(true);
          this.pendingError.set(true);
        },
      });
  }

  private loadCharts(months: number, days: number): void {
    forkJoin({
      revenue: this.dashboardService.getRevenueChart(months),
      views: this.dashboardService.getViewsChart(days),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ revenue, views }) => {
          this.buildRevenueChart(revenue);
          this.buildViewsChart(views);
          this.chartsLoading.set(false);
        },
        error: () => {
          this.chartsLoading.set(false);
          this.chartsError.set(true);
        },
      });
  }

  private getCssVar(name: string): string {
    if (!isPlatformBrowser(this.platformId)) return '';
    return getComputedStyle(this.doc.documentElement).getPropertyValue(name).trim();
  }

  private hexAlpha(hex: string, alpha: number): string {
    const a = Math.round(alpha * 255)
      .toString(16)
      .padStart(2, '0');
    return `${hex}${a}`;
  }

  private buildRevenueChart(data: MonthlyRevenue[]): void {
    const gold = this.getCssVar('--color-ndb-gold');
    const forest = this.getCssVar('--color-ndb-forest');
    const muted = this.getCssVar('--color-ndb-muted');
    const border = this.getCssVar('--color-ndb-border');
    const monthLabels = [
      'Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin',
      'Juil', 'Août', 'Sep', 'Oct', 'Nov', 'Déc',
    ];

    this.revenueChartData.set({
      labels: data.map((d) => monthLabels[parseInt(d.month.split('-')[1], 10) - 1]),
      datasets: [
        {
          label: 'Revenus (XAF)',
          data: data.map((d) => d.revenue),
          backgroundColor: this.hexAlpha(forest, 0.15),
          borderColor: gold,
          borderWidth: 2,
          fill: true,
          tension: 0.4,
          pointRadius: 4,
          pointBackgroundColor: gold,
          pointBorderColor: '#fff',
          pointBorderWidth: 2,
          pointHoverRadius: 6,
        },
      ],
    });
    this.revenueChartOptions.set({
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: forest,
          titleFont: { family: 'Lato' },
          bodyFont: { family: 'Lato' },
          cornerRadius: 8,
          padding: 12,
          callbacks: {
            label: (ctx: TooltipItem<'line'>) =>
              new Intl.NumberFormat('fr-FR').format(ctx.raw as number) + ' XAF',
          },
        },
      },
      scales: {
        y: {
          grid: {
            color: this.hexAlpha(border, 0.3),
          },
          border: { display: false },
          ticks: {
            font: { family: 'Lato', size: 11 },
            color: muted,
            callback: (v: number | string) => {
              const n = typeof v === 'string' ? parseFloat(v) : v;
              return n >= 1_000_000
                ? `${(n / 1_000_000).toFixed(1)}M`
                : n >= 1_000
                  ? `${Math.round(n / 1_000)}k`
                  : String(n);
            },
          },
        },
        x: {
          grid: { display: false },
          border: { display: false },
          ticks: {
            font: { family: 'Lato', size: 11 },
            color: muted,
          },
        },
      },
    });
  }

  private buildViewsChart(data: DailyViews[]): void {
    const forestMid = this.getCssVar('--color-ndb-forest-mid');
    const forest = this.getCssVar('--color-ndb-forest');
    const muted = this.getCssVar('--color-ndb-muted');
    const border = this.getCssVar('--color-ndb-border');

    this.viewsChartData.set({
      labels: data.map((d) => {
        const date = new Date(d.date);
        return date.toLocaleDateString('fr-FR', { weekday: 'short' });
      }),
      datasets: [
        {
          label: 'Vues',
          data: data.map((d) => d.viewCount),
          borderColor: forestMid,
          backgroundColor: this.hexAlpha(forestMid, 0.09),
          fill: true,
          tension: 0.4,
          pointRadius: 3,
          pointBackgroundColor: forestMid,
          pointBorderColor: '#fff',
          pointBorderWidth: 2,
          yAxisID: 'y',
        },
      ],
    });
    this.viewsChartOptions.set({
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: forest,
          titleFont: { family: 'Lato' },
          bodyFont: { family: 'Lato' },
          cornerRadius: 8,
          padding: 12,
        },
      },
      scales: {
        y: {
          position: 'left' as const,
          grid: {
            color: this.hexAlpha(border, 0.3),
          },
          border: { display: false },
          ticks: {
            font: { family: 'Lato', size: 11 },
            color: muted,
          },
        },
        x: {
          grid: { display: false },
          border: { display: false },
          ticks: {
            font: { family: 'Lato', size: 11 },
            color: muted,
            maxTicksLimit: 7,
          },
        },
      },
    });
  }

  protected formatXAF(amount: number): string {
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(amount) + ' XAF';
  }

  protected formatPercent(value: number): string {
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value) + '%';
  }

  protected formatDateFr(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }

  protected confirmBooking(bookingId: number): void {
    this.loadingBookings.update((s) => new Set(s).add(bookingId));
    this.bookingService
      .confirmBooking(bookingId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.loadingBookings.update((s) => {
            const next = new Set(s);
            next.delete(bookingId);
            return next;
          });
          this.messageService.add({
            severity: 'success',
            summary: 'Réservation confirmée',
            detail: 'La réservation a été confirmée avec succès.',
          });
          this.dismissedBookings.update((s) => new Set(s).add(bookingId));
          this.dashboard.update((d) =>
            d ? { ...d, pendingBookings: Math.max(0, d.pendingBookings - 1) } : d,
          );
        },
        error: () => {
          this.loadingBookings.update((s) => {
            const next = new Set(s);
            next.delete(bookingId);
            return next;
          });
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de confirmer la réservation.',
          });
        },
      });
  }

  protected rejectBooking(bookingId: number): void {
    this.loadingBookings.update((s) => new Set(s).add(bookingId));
    this.bookingService
      .rejectBooking(bookingId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.loadingBookings.update((s) => {
            const next = new Set(s);
            next.delete(bookingId);
            return next;
          });
          this.messageService.add({
            severity: 'warn',
            summary: 'Demande refusée',
            detail: 'La demande a été refusée.',
          });
          this.dismissedBookings.update((s) => new Set(s).add(bookingId));
          this.dashboard.update((d) =>
            d ? { ...d, pendingBookings: Math.max(0, d.pendingBookings - 1) } : d,
          );
        },
        error: () => {
          this.loadingBookings.update((s) => {
            const next = new Set(s);
            next.delete(bookingId);
            return next;
          });
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de refuser la réservation.',
          });
        },
      });
  }

  protected isBookingLoading(bookingId: number): boolean {
    return this.loadingBookings().has(bookingId);
  }

  protected onActivityItemClick(item: ActivityItem): void {
    const routes: Record<string, string> = {
      PAYMENT_RECEIVED: '/landlord/transactions',
      PAYMENT_FAILED: '/landlord/transactions',
      BOOKING_CREATED: '/landlord/bookings',
      BOOKING_CONFIRMED: '/landlord/bookings',
      BOOKING_CANCELLED: '/landlord/bookings',
      REVIEW_POSTED: `/listings/${item.referenceId}`,
      LISTING_APPROVED: `/listings/${item.referenceId}`,
      LISTING_REJECTED: `/listings/${item.referenceId}`,
    };
    const route = routes[item.type];
    if (route) {
      this.router.navigateByUrl(route);
    }
  }

  protected staggerDelay(i: number): number {
    return i * 0.05;
  }
}
