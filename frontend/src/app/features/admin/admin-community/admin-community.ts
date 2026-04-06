import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { InputTextModule } from 'primeng/inputtext';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { AvatarModule } from 'primeng/avatar';
import { AdminService } from '../../../core/services/admin';
import {
  AdminUserListItem,
  AdminUserRole,
  GlobalStats,
} from '../../../core/models/admin.models';
import { AdminEmptyState } from '../shared/admin-empty-state/admin-empty-state';
import { AdminErrorState } from '../shared/admin-error-state/admin-error-state';
import { UserDetailDrawer } from '../shared/user-detail-drawer/user-detail-drawer';

@Component({
  selector: 'app-admin-community',
  imports: [
    DatePipe,
    FormsModule,
    TableModule,
    TagModule,
    ButtonModule,
    SelectButtonModule,
    InputTextModule,
    IconFieldModule,
    InputIconModule,
    PaginatorModule,
    SkeletonModule,
    AvatarModule,
    AdminEmptyState,
    AdminErrorState,
    UserDetailDrawer,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-community.html',
  styleUrl: './admin-community.scss',
})
export class AdminCommunity implements OnInit {
  private adminService = inject(AdminService);
  private destroyRef = inject(DestroyRef);

  protected stats = signal<GlobalStats | null>(null);
  protected loadingStats = signal(true);
  protected errorStats = signal(false);
  protected users = signal<AdminUserListItem[]>([]);
  protected totalElements = signal(0);
  protected loading = signal(true);
  protected error = signal(false);

  protected search = signal('');
  protected roleFilter = signal<string>('ALL');
  protected statusFilter = signal<string>('');
  protected page = signal(0);
  protected pageSize = 20;

  protected selectedUserId = signal<number | null>(null);

  protected roleOptions = [
    { label: 'Tous', value: 'ALL' },
    { label: 'Étudiants', value: 'STUDENT' },
    { label: 'Propriétaires', value: 'LANDLORD' },
  ];

  ngOnInit(): void {
    this.loadStats();
    this.loadUsers();
  }

  protected onSearch(value: string): void {
    if (this.search() === value) return;
    this.search.set(value);
    this.page.set(0);
    this.loadUsers();
  }

  protected onRoleChange(role: string): void {
    if (this.roleFilter() === role) return;
    this.roleFilter.set(role);
    this.page.set(0);
    this.loadUsers();
  }

  protected onPageChange(event: PaginatorState): void {
    this.page.set(event.page ?? 0);
    this.loadUsers();
  }

  protected openUserDetail(user: AdminUserListItem): void {
    this.selectedUserId.set(user.id);
  }

  protected onDrawerClosed(): void {
    this.selectedUserId.set(null);
  }

  protected onUserChanged(): void {
    this.loadUsers();
  }

  protected roleSeverity(role: string): 'info' | 'success' | 'warn' {
    if (role === 'LANDLORD') return 'info';
    if (role === 'ADMIN') return 'warn';
    return 'success';
  }

  protected roleLabel(role: string): string {
    if (role === 'LANDLORD') return 'Propriétaire';
    if (role === 'STUDENT') return 'Étudiant';
    if (role === 'ADMIN') return 'Admin';
    return role;
  }

  protected statusSeverity(user: AdminUserListItem): 'success' | 'danger' | 'warn' {
    if (user.deactivated) return 'danger';
    if (user.status === 'ACTIVE') return 'success';
    return 'warn';
  }

  protected statusLabel(user: AdminUserListItem): string {
    if (user.deactivated) return 'Désactivé';
    if (user.status === 'ACTIVE') return 'Actif';
    if (user.status === 'INACTIVE') return 'Inactif';
    return user.status;
  }

  protected retryStats(): void {
    this.loadStats();
  }

  protected retryLoad(): void {
    this.loadStats();
    this.loadUsers();
  }

  private loadStats(): void {
    this.loadingStats.set(true);
    this.errorStats.set(false);
    this.adminService.getGlobalStats().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.stats.set(data);
        this.loadingStats.set(false);
      },
      error: () => {
        this.errorStats.set(true);
        this.loadingStats.set(false);
      },
    });
  }

  private loadUsers(): void {
    this.loading.set(true);
    this.error.set(false);
    const role = this.roleFilter() === 'ALL' ? undefined : this.roleFilter();
    const status = this.statusFilter() || undefined;
    const search = this.search() || undefined;

    this.adminService.getUsers(role, status, search, this.page(), this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.users.set(page.content);
          this.totalElements.set(page.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.error.set(true);
          this.loading.set(false);
        },
      });
  }
}
