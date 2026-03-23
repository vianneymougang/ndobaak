import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  OnInit,
  DestroyRef,
  PLATFORM_ID,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { FloatLabelModule } from 'primeng/floatlabel';
import { TagModule } from 'primeng/tag';
import { DividerModule } from 'primeng/divider';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';
import { FileUploadModule } from 'primeng/fileupload';
import { MessageService } from 'primeng/api';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { AuthService } from '../../../core/services/auth';
import { ProfileService } from '../../../core/services/profile';

@Component({
  selector: 'app-landlord-profile',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    AvatarModule,
    ButtonModule,
    InputTextModule,
    FloatLabelModule,
    TagModule,
    DividerModule,
    SkeletonModule,
    TooltipModule,
    FileUploadModule,
    NgmMotionDirective,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './landlord-profile.html',
  styleUrl: './landlord-profile.scss',
})
export class LandlordProfile implements OnInit {
  private authService = inject(AuthService);
  private profileService = inject(ProfileService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);
  private platformId = inject(PLATFORM_ID);
  private fb = inject(FormBuilder);

  protected user = this.authService.user;
  protected loading = signal(true);
  protected editMode = signal(false);
  protected saving = signal(false);
  protected avatarUploading = signal(false);

  protected profileForm = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    phone: ['', Validators.required],
  });

  protected userInitials = computed(() => {
    const u = this.user();
    if (!u) return '';
    return (u.firstName?.charAt(0) ?? '') + (u.lastName?.charAt(0) ?? '');
  });

  protected fullName = computed(() => {
    const u = this.user();
    if (!u) return '';
    return `${u.firstName} ${u.lastName}`;
  });

  protected isVerified = computed(() => !!this.user()?.verified);

  protected registrationDateFormatted = computed(() => {
    const date = this.user()?.registrationDate;
    if (!date) return '';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  });

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadProfile();
    }
  }

  private loadProfile(): void {
    this.authService
      .loadCurrentUser()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.populateForm();
        },
        error: () => this.loading.set(false),
      });
  }

  private populateForm(): void {
    const u = this.user();
    if (!u) return;
    this.profileForm.patchValue({
      firstName: u.firstName,
      lastName: u.lastName,
      phone: u.phone,
    });
  }

  protected toggleEdit(): void {
    if (this.editMode()) {
      this.editMode.set(false);
      this.populateForm();
    } else {
      this.editMode.set(true);
    }
  }

  protected saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.profileService
      .updateProfile({
        firstName: this.profileForm.value.firstName!,
        lastName: this.profileForm.value.lastName!,
        phone: this.profileForm.value.phone!,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.editMode.set(false);
          this.messageService.add({
            severity: 'success',
            summary: 'Profil mis à jour',
            detail: 'Vos informations ont été enregistrées.',
          });
        },
        error: () => {
          this.saving.set(false);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de mettre à jour le profil.',
          });
        },
      });
  }

  protected onAvatarSelect(event: { files: File[] }): void {
    const file = event.files[0];
    if (!file) return;
    this.avatarUploading.set(true);
    this.profileService
      .uploadAvatar(file)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.avatarUploading.set(false);
          this.messageService.add({
            severity: 'success',
            summary: 'Photo mise à jour',
            detail: 'Votre photo de profil a été changée.',
          });
        },
        error: () => {
          this.avatarUploading.set(false);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: "Impossible de mettre à jour la photo.",
          });
        },
      });
  }

  protected logout(): void {
    this.authService.logout();
  }
}
