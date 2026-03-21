import {
  Component,
  ChangeDetectionStrategy,
  input,
  signal,
  computed,
  inject,
  TemplateRef,
  contentChild,
} from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgTemplateOutlet } from '@angular/common';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { DrawerModule } from 'primeng/drawer';
import { ButtonModule } from 'primeng/button';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { AuthService } from '../../../core/services/auth';
import { NotificationBell } from '../notification-bell/notification-bell';

export interface ShellMenuItem {
  label: string;
  icon: string;
  route: string;
  badge?: number;
}

export interface ShellBottomTabItem {
  label: string;
  icon: string;
  route?: string;
  isMore?: boolean;
  badge?: number;
}

export interface ShellMoreItem {
  label: string;
  icon: string;
  route?: string;
  action?: string;
}

@Component({
  selector: 'app-dashboard-shell',
  imports: [
    RouterLink,
    RouterLinkActive,
    NgTemplateOutlet,
    AvatarModule,
    BadgeModule,
    DrawerModule,
    ButtonModule,
    NgmMotionDirective,
    NotificationBell,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard-shell.html',
  styleUrl: './dashboard-shell.scss',
})
export class DashboardShell {
  private authService = inject(AuthService);

  menuItems = input<ShellMenuItem[]>([]);
  bottomTabItems = input<ShellBottomTabItem[]>([]);
  moreItems = input<ShellMoreItem[]>([]);
  userName = input('');
  brandLabel = input('Ndobaâk');

  topbarActions = contentChild<TemplateRef<unknown>>('topbarActions');

  protected user = this.authService.user;
  protected moreSheetVisible = signal(false);

  protected userInitials = computed(() => {
    const u = this.user();
    if (!u) return '';
    return (u.firstName?.charAt(0) ?? '') + (u.lastName?.charAt(0) ?? '');
  });

  protected greeting = computed(() => {
    const name = this.userName();
    return name ? `Bonjour, ${name}` : 'Bonjour';
  });

  protected openMoreSheet(): void {
    this.moreSheetVisible.set(true);
  }

  protected closeMoreSheet(): void {
    this.moreSheetVisible.set(false);
  }

  protected onMoreItemClick(item: ShellMoreItem): void {
    if (item.action === 'logout') {
      this.authService.logout();
    }
    this.closeMoreSheet();
  }

  protected logout(): void {
    this.authService.logout();
  }
}
