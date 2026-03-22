import { Component, ChangeDetectionStrategy, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { DialogModule } from 'primeng/dialog';
import { FloatLabelModule } from 'primeng/floatlabel';
import { TextareaModule } from 'primeng/textarea';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { AdminService } from '../../../core/services/admin';
import { AdminUserDetail as AdminUserDetailData } from '../../../core/models/admin.models';

@Component({
  selector: 'app-admin-user-detail',
  imports: [
    DatePipe, DecimalPipe, RouterLink, ReactiveFormsModule,
    ButtonModule, TagModule, SkeletonModule,
    DialogModule, FloatLabelModule, TextareaModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-user-detail.html',
  styleUrl: './admin-user-detail.scss',
})
export class AdminUserDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private adminService = inject(AdminService);
  private messageService = inject(MessageService);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  protected user = signal<AdminUserDetailData | null>(null);
  protected loading = signal(true);
  protected actionLoading = signal(false);
  protected deactivateDialogVisible = signal(false);

  protected deactivateForm = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(10)]],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.adminService.getUserDetail(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data: AdminUserDetailData) => { this.user.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected openDeactivate(): void {
    this.deactivateForm.reset();
    this.deactivateDialogVisible.set(true);
  }

  protected submitDeactivate(): void {
    if (this.deactivateForm.invalid) { this.deactivateForm.markAllAsTouched(); return; }
    const id = this.user()?.id;
    if (!id) return;
    this.actionLoading.set(true);
    this.adminService.deactivateUser(id, { reason: this.deactivateForm.value.reason! }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.messageService.add({ severity: 'warn', summary: 'Compte désactivé', life: 3000 });
        this.deactivateDialogVisible.set(false);
        this.reload(id);
        this.actionLoading.set(false);
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
        this.messageService.add({ severity: 'success', summary: 'Compte réactivé', life: 3000 });
        this.reload(id);
        this.actionLoading.set(false);
      },
      error: () => this.actionLoading.set(false),
    });
  }

  protected roleSeverity(role: string): 'success' | 'info' | 'warn' {
    return role === 'ADMIN' ? 'warn' : role === 'LANDLORD' ? 'info' : 'success';
  }

  private reload(id: number): void {
    this.adminService.getUserDetail(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => this.user.set(data),
    });
  }
}
