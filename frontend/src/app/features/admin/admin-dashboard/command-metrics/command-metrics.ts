import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  computed,
  ElementRef,
  viewChildren,
  afterNextRender,
  PLATFORM_ID,
  inject,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';

import { GlobalStats } from '../../../../core/models/admin.models';
import type { MonthDelta } from '../admin-dashboard';

interface MetricConfig {
  key: string;
  label: string;
  icon: string;
  route: string;
  getValue: (s: GlobalStats) => string;
  getSub: (s: GlobalStats) => string;
  delta: MonthDelta | null;
  sparkData: number[];
}

@Component({
  selector: 'app-command-metrics',
  imports: [RouterLink, SkeletonModule, ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './command-metrics.html',
  styleUrl: './command-metrics.scss',
})
export class CommandMetrics {
  stats = input<GlobalStats | null>(null);
  userDelta = input<MonthDelta>({ current: 0, previous: 0, percent: 0 });
  bookingDelta = input<MonthDelta>({ current: 0, previous: 0, percent: 0 });
  revenueDelta = input<MonthDelta>({ current: 0, previous: 0, percent: 0 });
  loading = input(false);
  error = input(false);
  retry = output<void>();

  private platformId = inject(PLATFORM_ID);
  protected sparkCanvases = viewChildren<ElementRef<HTMLCanvasElement>>('sparkCanvas');

  protected metrics = computed<MetricConfig[]>(() => {
    const s = this.stats();
    if (!s) return [];
    return [
      {
        key: 'users',
        label: 'Utilisateurs',
        icon: 'pi pi-users',
        route: '/admin/community',
        getValue: (st) => this.fmtNum(st.totalUsers),
        getSub: (st) => `+${st.newUsersThisMonth} ce mois`,
        delta: this.userDelta(),
        sparkData: (s.monthlyUsers ?? []).map((p) => p.value),
      },
      {
        key: 'listings',
        label: 'Annonces',
        icon: 'pi pi-home',
        route: '/admin/listings',
        getValue: (st) => `${st.publishedListings}/${st.totalListings}`,
        getSub: (st) => `${st.pendingListings} en attente`,
        delta: null,
        sparkData: [],
      },
      {
        key: 'bookings',
        label: 'Réservations',
        icon: 'pi pi-calendar',
        route: '/admin/finances',
        getValue: (st) => `${st.confirmedBookings}/${st.totalBookings}`,
        getSub: (st) => `+${st.newBookingsThisMonth} ce mois`,
        delta: this.bookingDelta(),
        sparkData: (s.monthlyBookings ?? []).map((p) => p.value),
      },
      {
        key: 'revenue',
        label: 'Revenus',
        icon: 'pi pi-wallet',
        route: '/admin/finances',
        getValue: (st) => this.fmtXAF(st.totalRevenue),
        getSub: () => '',
        delta: this.revenueDelta(),
        sparkData: (s.monthlyRevenue ?? []).map((p) => p.value),
      },
      {
        key: 'subscriptions',
        label: 'Abonnements',
        icon: 'pi pi-star',
        route: '/admin/community',
        getValue: (st) => `${st.activeSubscriptions}`,
        getSub: (st) => `sur ${st.totalLandlords} propriétaires`,
        delta: null,
        sparkData: [],
      },
    ];
  });

  constructor() {
    afterNextRender(() => {
      this.drawSparklines();
    });
  }

  protected formatDelta(delta: MonthDelta | null): string {
    if (!delta || delta.percent === 0) return '';
    const sign = delta.percent > 0 ? '+' : '';
    return (
      (delta.percent > 0 ? '↑ ' : '↓ ') +
      sign +
      new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(
        delta.percent,
      ) +
      '%'
    );
  }

  protected isDeltaPositive(delta: MonthDelta | null): boolean {
    return (delta?.percent ?? 0) > 0;
  }

  protected isDeltaNegative(delta: MonthDelta | null): boolean {
    return (delta?.percent ?? 0) < 0;
  }

  private drawSparklines(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const canvases = this.sparkCanvases();
    const metricsVal = this.metrics();
    canvases.forEach((canvasRef, idx) => {
      const metric = metricsVal[idx];
      if (!metric || metric.sparkData.length < 2) return;
      this.drawSparkline(canvasRef.nativeElement, metric.sparkData);
    });
  }

  private drawSparkline(canvas: HTMLCanvasElement, data: number[]): void {
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const dpr = window.devicePixelRatio || 1;
    const w = canvas.clientWidth;
    const h = canvas.clientHeight;
    canvas.width = w * dpr;
    canvas.height = h * dpr;
    ctx.scale(dpr, dpr);

    const min = Math.min(...data);
    const max = Math.max(...data);
    const range = max - min || 1;
    const padY = 2;

    ctx.beginPath();
    data.forEach((val, i) => {
      const x = (i / (data.length - 1)) * w;
      const y = h - padY - ((val - min) / range) * (h - padY * 2);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });

    const style = getComputedStyle(document.documentElement);
    ctx.strokeStyle = style.getPropertyValue('--color-ndb-forest-light').trim() || '#40916C';
    ctx.lineWidth = 1.5;
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    ctx.stroke();

    ctx.lineTo(w, h);
    ctx.lineTo(0, h);
    ctx.closePath();
    const grad = ctx.createLinearGradient(0, 0, 0, h);
    grad.addColorStop(0, 'rgba(64,145,108,0.15)');
    grad.addColorStop(1, 'rgba(64,145,108,0)');
    ctx.fillStyle = grad;
    ctx.fill();
  }

  private fmtNum(n: number): string {
    return new Intl.NumberFormat('fr-FR').format(n);
  }

  private fmtXAF(n: number): string {
    if (n >= 1_000_000) {
      return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 }).format(n / 1_000_000) + 'M';
    }
    return new Intl.NumberFormat('fr-FR').format(n);
  }
}
