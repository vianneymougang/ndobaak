import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SkeletonModule } from 'primeng/skeleton';

@Component({
  selector: 'app-kpi-card',
  imports: [RouterLink, SkeletonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './kpi-card.html',
  styleUrl: './kpi-card.scss',
})
export class KpiCard {
  icon = input.required<string>();
  label = input.required<string>();
  value = input<string>('');
  trend = input<number | null>(null);
  trendLabel = input<string>('');
  loading = input(false);
  accent = input(false);
  routerLink = input<string | null>(null);
  animationDelay = input(0);

  protected trendPositive = computed(() => (this.trend() ?? 0) > 0);
  protected trendNegative = computed(() => (this.trend() ?? 0) < 0);

  protected formattedTrend = computed(() => {
    const v = this.trend();
    if (v === null || v === undefined || v === 0) return '';
    const sign = v > 0 ? '+' : '';
    return sign + new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(v) + '%';
  });
}
