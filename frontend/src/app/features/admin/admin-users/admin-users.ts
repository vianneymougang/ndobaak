import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { InputTextModule } from 'primeng/inputtext';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { SelectModule } from 'primeng/select';
import { PaginatorModule } from 'primeng/paginator';
import { AdminService } from '../../../core/services/admin';
import { AdminUserListItem, AdminPage } from '../../../core/models/admin.models';

@Component({
  selector: 'app-admin-users',
  imports: [
    DatePipe, RouterLink,
    TableModule, TagModule, ButtonModule, SkeletonModule,
    InputTextModule, IconFieldModule, InputIconModule,
    SelectModule, PaginatorModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss',
})
export class AdminUsers implements OnInit {
  private adminService = inject(AdminService);

  protected page = signal<AdminPage<AdminUserListItem> | null>(null);
  protected loading = signal(true);
  protected currentPage = signal(0);
  protected searchQuery = signal('');
  protected roleFilter = signal('');
  protected statusFilter = signal('');
  protected readonly skeletonRows = Array(10).fill({});

  protected roleOptions = [
    { label: 'Tous les rôles', value: '' },
    { label: 'Étudiant', value: 'STUDENT' },
    { label: 'Propriétaire', value: 'LANDLORD' },
    { label: 'Admin', value: 'ADMIN' },
  ];

  protected statusOptions = [
    { label: 'Tous les statuts', value: '' },
    { label: 'Actif', value: 'ACTIVE' },
    { label: 'Inactif', value: 'INACTIVE' },
    { label: 'Suspendu', value: 'SUSPENDED' },
  ];

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.adminService
      .getUsers(
        this.roleFilter() || undefined,
        this.statusFilter() || undefined,
        this.searchQuery() || undefined,
        this.currentPage(),
      )
      .subscribe({
        next: (data) => { this.page.set(data); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
  }

  protected onSearch(event: Event): void {
    this.searchQuery.set((event.target as HTMLInputElement).value);
    this.currentPage.set(0);
    this.load();
  }

  protected onRoleChange(value: string): void {
    this.roleFilter.set(value);
    this.currentPage.set(0);
    this.load();
  }

  protected onStatusChange(value: string): void {
    this.statusFilter.set(value);
    this.currentPage.set(0);
    this.load();
  }

  protected onPageChange(event: { page?: number }): void {
    this.currentPage.set(event.page ?? 0);
    this.load();
  }

  protected roleSeverity(role: string): 'success' | 'info' | 'warn' {
    return role === 'ADMIN' ? 'warn' : role === 'LANDLORD' ? 'info' : 'success';
  }
}
