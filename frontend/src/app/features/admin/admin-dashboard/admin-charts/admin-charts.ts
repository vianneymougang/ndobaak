import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  computed,
  inject,
  PLATFORM_ID,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';

import { GlobalStats } from '../../../../core/models/admin.models';
import type { ChartData, ChartOptions } from 'chart.js';
import { SectionHeader } from '../../../../shared/components/section-header/section-header';

@Component({
  selector: 'app-admin-charts',
  imports: [SkeletonModule, ButtonModule, ChartModule, SectionHeader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-charts.html',
  styleUrl: './admin-charts.scss',
})
export class AdminCharts {
  stats = input<GlobalStats | null>(null);
  loading = input(false);
  error = input(false);
  retry = output<void>();

  private platformId = inject(PLATFORM_ID);
  protected isBrowser = isPlatformBrowser(this.platformId);

  protected revenueChartData = computed<ChartData<'line'> | null>(() => {
    const s = this.stats();
    if (!s?.monthlyRevenue?.length) return null;
    const sorted = [...s.monthlyRevenue].sort((a, b) => a.month.localeCompare(b.month));
    return {
      labels: sorted.map((p) => this.formatMonth(p.month)),
      datasets: [
        {
          label: 'Revenus (XAF)',
          data: sorted.map((p) => p.value),
          borderColor: '#1B4332',
          backgroundColor: 'rgba(27, 67, 50, 0.08)',
          borderWidth: 2,
          tension: 0.4,
          fill: true,
          pointBackgroundColor: '#1B4332',
          pointBorderColor: '#fff',
          pointBorderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6,
        },
      ],
    };
  });

  protected revenueChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#1B4332',
        titleFont: { family: 'Urbanist', weight: 'bold' },
        bodyFont: { family: 'Lato' },
        padding: 10,
        cornerRadius: 8,
        callbacks: {
          label: (ctx) => new Intl.NumberFormat('fr-FR').format(ctx.parsed.y ?? 0) + ' XAF',
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { font: { family: 'Lato', size: 11 }, color: '#6B7280' },
      },
      y: {
        grid: { color: 'rgba(0,0,0,0.04)' },
        ticks: {
          font: { family: 'Lato', size: 11 },
          color: '#6B7280',
          callback: (val: string | number) => {
            const n = Number(val);
            return n >= 1_000_000
              ? new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 }).format(n / 1_000_000) + 'M'
              : new Intl.NumberFormat('fr-FR').format(n);
          },
        },
      },
    },
  };

  protected userChartData = computed<ChartData<'bar'> | null>(() => {
    const s = this.stats();
    if (!s?.monthlyUsers?.length) return null;
    const sorted = [...s.monthlyUsers].sort((a, b) => a.month.localeCompare(b.month));
    return {
      labels: sorted.map((p) => this.formatMonth(p.month)),
      datasets: [
        {
          label: 'Nouveaux utilisateurs',
          data: sorted.map((p) => p.value),
          backgroundColor: 'rgba(212, 160, 23, 0.6)',
          borderColor: '#D4A017',
          borderWidth: 1,
          borderRadius: 6,
          barPercentage: 0.6,
        },
      ],
    };
  });

  protected userChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#1B4332',
        titleFont: { family: 'Urbanist', weight: 'bold' },
        bodyFont: { family: 'Lato' },
        padding: 10,
        cornerRadius: 8,
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { font: { family: 'Lato', size: 11 }, color: '#6B7280' },
      },
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(0,0,0,0.04)' },
        ticks: { font: { family: 'Lato', size: 11 }, color: '#6B7280', stepSize: 10 },
      },
    },
  };

  protected userDonutData = computed<ChartData<'doughnut'> | null>(() => {
    const s = this.stats();
    if (!s) return null;
    return {
      labels: ['Étudiants', 'Propriétaires'],
      datasets: [
        {
          data: [s.totalStudents, s.totalLandlords],
          backgroundColor: ['#1B4332', '#D4A017'],
          borderWidth: 0,
          hoverOffset: 4,
        },
      ],
    };
  });

  protected donutOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '70%',
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          font: { family: 'Lato', size: 12 },
          color: '#374151',
          padding: 16,
          usePointStyle: true,
          pointStyleWidth: 8,
        },
      },
      tooltip: {
        backgroundColor: '#1B4332',
        titleFont: { family: 'Urbanist', weight: 'bold' },
        bodyFont: { family: 'Lato' },
        padding: 10,
        cornerRadius: 8,
      },
    },
  };

  private formatMonth(month: string): string {
    const [y, m] = month.split('-');
    const months = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'];
    return months[parseInt(m, 10) - 1] ?? m;
  }
}
