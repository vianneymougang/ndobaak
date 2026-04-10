import { Component, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import {
  DashboardShell,
  ShellMenuItem,
  ShellBottomTabItem,
  ShellMoreItem,
} from '../../../shared/components/dashboard-shell/dashboard-shell';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-landlord-layout',
  imports: [RouterOutlet, ToastModule, DashboardShell],
  providers: [MessageService],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './landlord-layout.html',
  styleUrl: './landlord-layout.scss',
})
export class LandlordLayout {
  private authService = inject(AuthService);

  protected userName = computed(() => this.authService.user()?.firstName ?? '');

  protected menuItems: ShellMenuItem[] = [
    { label: 'Tableau de bord', icon: 'pi-th-large', route: '/landlord/dashboard' },
    { label: 'Mes annonces', icon: 'pi-home', route: '/landlord/listings' },
    { label: 'Réservations', icon: 'pi-calendar', route: '/landlord/bookings' },
    { label: 'Mes documents', icon: 'pi-file-check', route: '/landlord/documents' },
    { label: 'Transactions', icon: 'pi-wallet', route: '/landlord/transactions' },
    { label: 'Souscription', icon: 'pi-star', route: '/landlord/subscription' },
    { label: 'Mon profil', icon: 'pi-user', route: '/landlord/profile' },
  ];

  protected bottomTabItems: ShellBottomTabItem[] = [
    { label: 'Accueil', icon: 'pi-th-large', route: '/landlord/dashboard' },
    { label: 'Annonces', icon: 'pi-home', route: '/landlord/listings' },
    { label: 'Réservations', icon: 'pi-calendar', route: '/landlord/bookings' },
    { label: 'Transactions', icon: 'pi-wallet', route: '/landlord/transactions' },
    { label: 'Plus', icon: 'pi-ellipsis-h', isMore: true },
  ];

  protected moreItems: ShellMoreItem[] = [
    { label: 'Mes documents', icon: 'pi-file-check', route: '/landlord/documents' },
    { label: 'Souscription', icon: 'pi-star', route: '/landlord/subscription' },
    { label: 'Mon profil', icon: 'pi-user', route: '/landlord/profile' },
    { label: 'Déconnexion', icon: 'pi-sign-out', action: 'logout' },
  ];
}
