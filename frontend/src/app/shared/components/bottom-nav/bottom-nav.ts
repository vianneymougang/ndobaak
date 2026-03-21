import { Component, ChangeDetectionStrategy, computed, inject, signal } from '@angular/core';
import { PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth';
import { NavState } from '../../services/nav-state';

@Component({
  selector: 'app-bottom-nav',
  imports: [RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './bottom-nav.html',
  styleUrl: './bottom-nav.scss',
  host: {
    '(window:scroll)': 'onScroll()',
  },
})
export class BottomNav {
  private authService = inject(AuthService);
  private platformId = inject(PLATFORM_ID);
  private navState = inject(NavState);

  protected isAuthenticated = this.authService.isAuthenticated;
  protected isLandlord = computed(() => this.authService.userRole() === 'LANDLORD');
  protected userInitials = computed(() => {
    const u = this.authService.user();
    if (!u) return '';
    return (u.firstName?.charAt(0) ?? '') + (u.lastName?.charAt(0) ?? '');
  });

  protected accountLabel = computed(() => (this.isAuthenticated() ? 'Compte' : 'Connexion'));

  protected isVisible = signal(false);
  private lastScrollY = 0;

  protected openDrawer(): void {
    this.navState.drawerOpen.set(true);
  }

  onScroll(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const currentY = window.scrollY;
    const threshold = window.innerHeight * 0.45;
    const delta = currentY - this.lastScrollY;

    if (currentY < threshold) {
      this.isVisible.set(false);
    } else if (delta < -8) {
      this.isVisible.set(true);
    } else if (delta > 8) {
      this.isVisible.set(false);
    }

    this.lastScrollY = currentY;
  }
}
