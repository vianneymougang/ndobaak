import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  DestroyRef,
  PLATFORM_ID,
} from '@angular/core';
import { DOCUMENT, isPlatformBrowser, DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { SelectButtonModule } from 'primeng/selectbutton';
import { ChartModule } from 'primeng/chart';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { DashboardService } from '../../../core/services/dashboard';
import { BookingService } from '../../../core/services/booking';
import type {
  LandlordDashboard as LandlordDashboardData,
  MonthlyRevenue,
  DailyViews,
  BookingsByStatus,
} from '../../../core/models/dashboard.models';

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
    DatePipe,
    DecimalPipe,
    FormsModule,
    ButtonModule,
    SkeletonModule,
    SelectButtonModule,
    ChartModule,
    TooltipModule,
    NgmMotionDirective,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './landlord-dashboard.html',
  styleUrl: './landlord-dashboard.scss',
})
export class LandlordDashboard implements OnInit {
  private dashboardService = inject(DashboardService);
  private bookingService = inject(BookingService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);
  private platformId = inject(PLATFORM_ID);
  private doc = inject(DOCUMENT);

  protected readonly periodOptions: PeriodOption[] = [
    { label: '7j', value: '7d', days: 7, months: 1 },
    { label: '30j', value: '30d', days: 30, months: 3 },
    { label: '3 mois', value: '3m', days: 90, months: 3 },
    { label: '12 mois', value: '12m', days: 365, months: 12 },
  ];

  protected selectedPeriod = '30d';

  protected dashboard = signal<LandlordDashboardData | null>(null);
  protected kpiLoading = signal(true);
  protected chartsLoading = signal(true);
  protected revenueChartData = signal<any>(null);
  protected revenueChartOptions = signal<any>(null);
  protected viewsChartData = signal<any>(null);
  protected viewsChartOptions = signal<any>(null);
  protected doughnutChartData = signal<any>(null);
  protected doughnutChartOptions = signal<any>(null);

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadDashboard();
    }
  }

  protected onPeriodChange(): void {
    this.loadDashboard();
  }

  protected staggerDelay(i: number): number {
    return i * 0.07;
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

    this.dashboardService
      .getLandlordDashboard(from, to)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data: LandlordDashboardData) => {
          this.dashboard.set(data);
          this.kpiLoading.set(false);
          this.buildDoughnutChart(data.bookingsByStatus);
          this.loadCharts(months, days);
        },
        error: () => {
          this.kpiLoading.set(false);
          this.chartsLoading.set(false);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger le tableau de bord. Réessayez.',
          });
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
        },
      });
  }

  private getCssVar(name: string): string {
    if (!isPlatformBrowser(this.platformId)) return '';
    return getComputedStyle(this.doc.documentElement).getPropertyValue(name).trim();
  }

  private buildRevenueChart(data: MonthlyRevenue[]): void {
    const primary = this.getCssVar('--color-ndb-gold');
    const monthLabels = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Août', 'Sep', 'Oct', 'Nov', 'Déc'];
    this.revenueChartData.set({
      labels: data.map((d) => monthLabels[parseInt(d.month.split('-')[1], 10) - 1]),
      datasets: [
        {
          label: 'Revenus (FCFA)',
          data: data.map((d) => d.revenue),
          backgroundColor: primary,
          borderRadius: 6,
          borderSkipped: false,
        },
      ],
    });
    this.revenueChartOptions.set({
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        y: {
          grid: { color: 'rgba(0,0,0,0.04)' },
          ticks: {
            callback: (v: number) => (v >= 1000 ? `${Math.round(v / 1000)}k` : String(v)),
          },
        },
        x: { grid: { display: false } },
      },
    });
  }

  private buildViewsChart(data: DailyViews[]): void {
    const accent = this.getCssVar('--color-ndb-forest');
    const fill = accent ? `${accent}20` : '';
    this.viewsChartData.set({
      labels: data.map((d) => d.date.slice(5)),
      datasets: [
        {
          label: 'Vues',
          data: data.map((d) => d.viewCount),
          borderColor: accent,
          backgroundColor: fill,
          fill: true,
          tension: 0.4,
          pointRadius: 3,
          pointBackgroundColor: accent,
        },
      ],
    });
    this.viewsChartOptions.set({
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        y: { grid: { color: 'rgba(0,0,0,0.04)' } },
        x: { ticks: { maxTicksLimit: 7 }, grid: { display: false } },
      },
    });
  }

  private buildDoughnutChart(data: BookingsByStatus): void {
    const gold = this.getCssVar('--color-ndb-gold');
    const forest = this.getCssVar('--color-ndb-forest');
    const forestLight = this.getCssVar('--color-ndb-forest-light');
    const error = this.getCssVar('--color-ndb-error');
    this.doughnutChartData.set({
      labels: ['En attente', 'Confirmées', 'Terminées', 'Annulées'],
      datasets: [
        {
          data: [data.pending, data.confirmed, data.completed, data.cancelled],
          backgroundColor: [gold, forest, forestLight, error],
          hoverOffset: 6,
        },
      ],
    });
    this.doughnutChartOptions.set({
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom', labels: { boxWidth: 12, padding: 16 } },
      },
      cutout: '65%',
    });
  }

  protected confirmBooking(bookingId: number): void {
    this.bookingService
      .confirmBooking(bookingId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Réservation confirmée',
            detail: 'La réservation a été confirmée avec succès.',
          });
          this.loadDashboard();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de confirmer la réservation.',
          });
        },
      });
  }

  protected rejectBooking(bookingId: number): void {
    this.bookingService
      .rejectBooking(bookingId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'warn',
            summary: 'Réservation refusée',
            detail: 'La demande a été refusée.',
          });
          this.loadDashboard();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de refuser la réservation.',
          });
        },
      });
  }

  protected getActivityIcon(type: string): string {
    const icons: Record<string, string> = {
      BOOKING_CREATED: 'pi-calendar-plus',
      BOOKING_CONFIRMED: 'pi-check-circle',
      BOOKING_CANCELLED: 'pi-times-circle',
      PAYMENT_RECEIVED: 'pi-wallet',
      PAYMENT_FAILED: 'pi-exclamation-triangle',
      REVIEW_POSTED: 'pi-star',
      LISTING_APPROVED: 'pi-verified',
      LISTING_REJECTED: 'pi-ban',
    };
    return icons[type] ?? 'pi-bell';
  }

  protected formatRevenue(amount: number): string {
    return new Intl.NumberFormat('fr-FR').format(amount) + ' FCFA';
  }
}
