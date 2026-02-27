import { Component, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AvatarModule } from 'primeng/avatar';
import { MenuModule } from 'primeng/menu';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, ButtonModule, AvatarModule, MenuModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar {
  private authService = inject(AuthService);

  protected isAuthenticated = this.authService.isAuthenticated;
  protected user = this.authService.user;
  protected userInitials = computed(() => {
    const u = this.user();
    if (!u) return '';
    return (u.firstName?.charAt(0) ?? '') + (u.lastName?.charAt(0) ?? '');
  });
  protected isLandlord = computed(() => this.authService.userRole() === 'LANDLORD');
  protected menuOpen = false;

  protected logout(): void {
    this.authService.logout();
  }

  protected toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }
}
