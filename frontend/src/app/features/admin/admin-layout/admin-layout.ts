import { Component, ChangeDetectionStrategy, inject, signal, computed } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ButtonModule, ToastModule, AvatarModule, BadgeModule],
  providers: [MessageService],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
})
export class AdminLayout {
  private authService = inject(AuthService);

  protected user = this.authService.user;
  protected sidebarOpen = signal(true);

  protected userInitials = computed(() => {
    const u = this.user();
    if (!u) return 'A';
    return (u.firstName?.charAt(0) ?? '') + (u.lastName?.charAt(0) ?? '');
  });

  protected toggleSidebar(): void {
    this.sidebarOpen.update((v) => !v);
  }

  protected logout(): void {
    this.authService.logout();
  }

  protected navLinks = [
    { path: '/admin/dashboard', icon: 'pi pi-chart-bar', label: 'Tableau de bord' },
    { path: '/admin/listings', icon: 'pi pi-home', label: 'Annonces' },
    { path: '/admin/users', icon: 'pi pi-users', label: 'Utilisateurs' },
    { path: '/admin/reviews', icon: 'pi pi-star', label: 'Avis' },
    { path: '/admin/transactions', icon: 'pi pi-credit-card', label: 'Transactions' },
  ];
}
