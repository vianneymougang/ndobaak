import { Component, ChangeDetectionStrategy, inject, signal, computed, OnInit, DestroyRef, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';
import {
  DashboardShell,
  ShellMenuItem,
  ShellBottomTabItem,
  ShellMoreItem,
} from '../../../shared/components/dashboard-shell/dashboard-shell';
import { AuthService } from '../../../core/services/auth';
import { AdminService } from '../../../core/services/admin';

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, ToastModule, ConfirmDialogModule, DashboardShell],
  providers: [MessageService, ConfirmationService],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
})
export class AdminLayout implements OnInit {
  private authService = inject(AuthService);
  private adminService = inject(AdminService);
  private destroyRef = inject(DestroyRef);
  private platformId = inject(PLATFORM_ID);

  protected userName = computed(() => this.authService.user()?.firstName ?? '');

  protected pendingListingsCount = signal(0);
  protected pendingReviewsCount = signal(0);
  protected anomaliesCount = signal(0);

  protected totalModerationCount = computed(
    () => this.pendingListingsCount() + this.pendingReviewsCount() + this.anomaliesCount(),
  );

  protected menuItems = computed<ShellMenuItem[]>(() => [
    { label: 'Tableau de bord', icon: 'pi-th-large', route: '/admin/dashboard' },
    { label: 'Modération', icon: 'pi-bell', route: '/admin/moderation', badge: this.totalModerationCount() },
    { label: 'Annonces', icon: 'pi-home', route: '/admin/listings' },
    { label: 'Communauté', icon: 'pi-users', route: '/admin/community' },
    { label: 'Finances', icon: 'pi-wallet', route: '/admin/finances' },
  ]);

  protected bottomTabItems = computed<ShellBottomTabItem[]>(() => [
    { label: 'Accueil', icon: 'pi-th-large', route: '/admin/dashboard' },
    { label: 'Modération', icon: 'pi-bell', route: '/admin/moderation', badge: this.totalModerationCount() },
    { label: 'Annonces', icon: 'pi-home', route: '/admin/listings' },
    { label: 'Communauté', icon: 'pi-users', route: '/admin/community' },
    { label: 'Finances', icon: 'pi-wallet', route: '/admin/finances' },
  ]);

  protected moreItems: ShellMoreItem[] = [
    { label: 'Retour au site', icon: 'pi-arrow-left', route: '/' },
    { label: 'Déconnexion', icon: 'pi-sign-out', action: 'logout' },
  ];

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadBadgeCounts();
    }
  }

  private loadBadgeCounts(): void {
    forkJoin({
      listings: this.adminService.getPendingListings(),
      reviews: this.adminService.getReviews('pending', 0, 1),
      anomalies: this.adminService.getAnomalies(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ listings, reviews, anomalies }) => {
          this.pendingListingsCount.set(listings.length);
          this.pendingReviewsCount.set(reviews.totalElements);
          this.anomaliesCount.set(anomalies.length);
        },
      });
  }
}
