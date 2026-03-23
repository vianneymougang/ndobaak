import {
  Component,
  ChangeDetectionStrategy,
  input,
  computed,
  signal,
  effect,
} from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { GlobalStats, MonthlyStatPoint } from '../../../../core/models/admin.models';

@Component({
  selector: 'app-finance-chart-header',
  imports: [ChartModule, SkeletonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './finance-chart-header.html',
  styleUrl: './finance-chart-header.scss',
})
export class FinanceChartHeader {
  stats = input<GlobalStats | null>(null);
  loading = input(false);

  protected chartData = signal<unknown>(null);
  protected chartOptions = signal<unknown>(null);

  protected monthlyRevenue = computed(() => {
    const s = this.stats();
    if (!s) return 0;
    return s.monthlyRevenue?.length > 0 ? s.monthlyRevenue[s.monthlyRevenue.length - 1].value : 0;
  });

  protected transactionsThisMonth = computed(() => {
    const s = this.stats();
    if (!s) return 0;
    return s.newBookingsThisMonth;
  });

  constructor() {
    effect(() => {
      const s = this.stats();
      if (s?.monthlyRevenue?.length) {
        this.buildChart(s.monthlyRevenue);
      }
    });
  }

  protected formatXAF(amount: number): string {
    return new Intl.NumberFormat('fr-FR').format(amount) + ' XAF';
  }

  protected formatCompact(amount: number): string {
    if (amount >= 1000000) return (amount / 1000000).toFixed(1) + 'M';
    if (amount >= 1000) return (amount / 1000).toFixed(0) + 'K';
    return '' + amount;
  }

  private buildChart(data: MonthlyStatPoint[]): void {
    const labels = data.map(d => {
      const [year, month] = d.month.split('-');
      const date = new Date(+year, +month - 1);
      return date.toLocaleDateString('fr-FR', { month: 'short' });
    });

    const values = data.map(d => d.value);

    this.chartData.set({
      labels,
      datasets: [
        {
          label: 'Revenus',
          data: values,
          backgroundColor: 'rgba(27, 67, 50, 0.8)',
          borderRadius: 6,
          borderSkipped: false,
        },
      ],
    });

    this.chartOptions.set({
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx: { parsed: { y: number } }) =>
              new Intl.NumberFormat('fr-FR').format(ctx.parsed.y) + ' XAF',
          },
        },
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { font: { family: 'Lato', size: 11 } },
        },
        y: {
          grid: { color: 'rgba(0,0,0,0.04)' },
          ticks: {
            font: { family: 'Lato', size: 11 },
            callback: (value: number) => this.formatCompact(value),
          },
        },
      },
    });
  }
}
