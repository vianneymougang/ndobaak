import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  computed,
  signal,
  inject,
  effect,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { DrawerModule } from 'primeng/drawer';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { DialogModule } from 'primeng/dialog';
import { FloatLabelModule } from 'primeng/floatlabel';
import { TextareaModule } from 'primeng/textarea';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AdminErrorState } from '../admin-error-state/admin-error-state';
import { AdminService } from '../../../../core/services/admin';
import { AdminUserDetail, AdminUserRole, AdminUserStatus } from '../../../../core/models/admin.models';

@Component({
  selector: 'app-user-detail-drawer',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    DrawerModule,
    ButtonModule,
    TagModule,
    SkeletonModule,
    DialogModule,
    FloatLabelModule,
    TextareaModule,
    AdminErrorState,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './user-detail-drawer.html',
  styleUrl: './user-detail-drawer.scss',
})
export class UserDetailDrawer {
  private adminService = inject(AdminService);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  userId = input<number | null>(null);

  deactivated = output<number>();
  reactivated = output<number>();
  closed = output<void>();

  protected visible = computed(() => this.userId() !== null);
  protected user = signal<AdminUserDetail | null>(null);
  protected loading = signal(false);
  protected error = signal(false);
  protected actionLoading = signal(false);
  protected deactivateDialogVisible = signal(false);

  protected roleSeverity = computed<'success' | 'info' | 'warn'>(() => {
    const role = this.user()?.role;
    if (role === 'ADMIN') return 'warn';
    if (role === 'LANDLORD') return 'info';
    return 'success';
  });

  protected roleLabel = computed(() => {
    const role = this.user()?.role;
    if (role === 'LANDLORD') return 'Propriétaire';
    if (role === 'STUDENT') return 'Étudiant';
    if (role === 'ADMIN') return 'Admin';
    return role ?? '';
  });

  protected statusLabel = computed(() => {
    const u = this.user();
    if (!u) return '';
    if (u.deactivated) return 'Désactivé';
    if (u.status === 'ACTIVE') return 'Actif';
    if (u.status === 'INACTIVE') return 'Inactif';
    if (u.status === 'SUSPENDED') return 'Suspendu';
    return u.status;
  });

  protected statusSeverity = computed<'success' | 'warn' | 'danger'>(() => {
    const u = this.user();
    if (!u) return 'warn';
    if (u.deactivated) return 'danger';
    if (u.status === 'ACTIVE') return 'success';
    return 'warn';
  });

  protected deactivateForm = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(10)]],
  });

  constructor() {
    effect(() => {
      const id = this.userId();
      if (id !== null) {
        this.loadUser(id);
      } else {
        this.user.set(null);
      }
    });
  }

  protected onHide(): void {
    this.closed.emit();
  }

  protected formatXAF(amount: number): string {
    return new Intl.NumberFormat('fr-FR').format(amount) + ' XAF';
  }

  protected openDeactivateDialog(): void {
    this.deactivateForm.reset();
    this.deactivateDialogVisible.set(true);
  }

  protected submitDeactivate(): void {
    if (this.deactivateForm.invalid) {
      this.deactivateForm.markAllAsTouched();
      return;
    }
    const id = this.user()?.id;
    if (!id) return;
    this.actionLoading.set(true);
    this.adminService
      .deactivateUser(id, { reason: this.deactivateForm.value.reason! })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deactivateDialogVisible.set(false);
          this.actionLoading.set(false);
          this.loadUser(id);
          this.deactivated.emit(id);
        },
        error: () => this.actionLoading.set(false),
      });
  }

  protected reactivate(): void {
    const id = this.user()?.id;
    if (!id) return;
    this.actionLoading.set(true);
    this.adminService.reactivateUser(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.loadUser(id);
        this.reactivated.emit(id);
      },
      error: () => this.actionLoading.set(false),
    });
  }

  protected retryLoad(): void {
    const id = this.userId();
    if (id !== null) this.loadUser(id);
  }

  private loadUser(id: number): void {
    this.loading.set(true);
    this.error.set(false);
    this.adminService.getUserDetail(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.user.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
