import { Component, ChangeDetectionStrategy, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { MessageModule } from 'primeng/message';
import { AdminService } from '../../../core/services/admin';
import { GlobalStats } from '../../../core/models/admin.models';

@Component({
  selector: 'app-admin-dashboard',
  imports: [DecimalPipe, RouterLink, CardModule, SkeletonModule, TagModule, ButtonModule, ChartModule, MessageModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
})
export class AdminDashboard implements OnInit {
  private adminService = inject(AdminService);
  private destroyRef = inject(DestroyRef);

  protected stats = signal<GlobalStats | null>(null);
  protected loading = signal(true);
  protected error = signal(false);

  protected bookingsChartData = signal<object | null>(null);
  protected revenueChartData = signal<object | null>(null);
  protected chartOptions = {
    responsive: true,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true } },
  };

  ngOnInit(): void {
    this.adminService.getGlobalStats().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.stats.set(data);
        this.buildCharts(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  private buildCharts(data: GlobalStats): void {
    if (data.monthlyBookings?.length) {
      this.bookingsChartData.set({
        labels: data.monthlyBookings.map((p) => p.month),
        datasets: [{
          label: 'Réservations',
          data: data.monthlyBookings.map((p) => p.value),
          backgroundColor: 'rgba(249,115,22,0.2)',
          borderColor: '#f97316',
          borderWidth: 2,
          tension: 0.4,
          fill: true,
        }],
      });
    }
    if (data.monthlyRevenue?.length) {
      this.revenueChartData.set({
        labels: data.monthlyRevenue.map((p) => p.month),
        datasets: [{
          label: 'Revenus (FCFA)',
          data: data.monthlyRevenue.map((p) => p.value),
          backgroundColor: 'rgba(99,102,241,0.2)',
          borderColor: '#6366f1',
          borderWidth: 2,
          tension: 0.4,
          fill: true,
        }],
      });
    }
  }
}
