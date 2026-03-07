import {
  Component,
  ChangeDetectionStrategy,
  inject,
  computed,
  signal,
  HostListener,
} from '@angular/core';
import { PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
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
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);

  protected isAuthenticated = this.authService.isAuthenticated;
  protected user = this.authService.user;
  protected userInitials = computed(() => {
    const u = this.user();
    if (!u) return '';
    return (u.firstName?.charAt(0) ?? '') + (u.lastName?.charAt(0) ?? '');
  });
  protected isLandlord = computed(() => this.authService.userRole() === 'LANDLORD');
  protected menuOpen = false;
  protected scrolled = signal(false);

  private currentUrl = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => this.router.url),
      startWith(this.router.url),
    ),
  );

  /** Transparent only on the homepage before scrolling */
  protected isTransparent = computed(
    () => this.currentUrl() === '/' && !this.scrolled(),
  );

  @HostListener('window:scroll', [])
  onWindowScroll(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.scrolled.set(window.scrollY > 40);
    }
  }

  protected logout(): void {
    this.authService.logout();
  }

  protected toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }
}
