import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  DestroyRef,
  OnInit,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { Skeleton } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { NotificationService } from '../../../core/services/notification';
import {
  NotificationTypePreferenceResponse,
  PreferenceGroup,
  PREFERENCE_GROUPS,
  NOTIFICATION_LABELS,
} from '../../../core/models/notification-models';

@Component({
  selector: 'app-notification-preferences',
  imports: [FormsModule, ToggleSwitch, Skeleton, TooltipModule, ToastModule],
  providers: [MessageService],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './notification-preferences.html',
  styleUrl: './notification-preferences.scss',
})
export class NotificationPreferences implements OnInit {
  private notificationService = inject(NotificationService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);

  protected loading = signal(true);
  protected groups = signal<PreferenceGroup[]>([]);

  protected readonly NOTIFICATION_LABELS = NOTIFICATION_LABELS;
  protected readonly PREFERENCE_GROUPS = PREFERENCE_GROUPS;

  ngOnInit(): void {
    this.notificationService
      .getPreferences()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (prefs) => {
          this.groups.set(this.buildGroups(prefs));
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  protected onEmailChange(pref: NotificationTypePreferenceResponse, checked: boolean): void {
    this.savePreference(pref.notificationType, { emailEnabled: checked });
    this.groups.update((gs) =>
      gs.map((g) => ({
        ...g,
        preferences: g.preferences.map((p) =>
          p.notificationType === pref.notificationType ? { ...p, emailEnabled: checked } : p,
        ),
      })),
    );
  }

  protected onSmsChange(pref: NotificationTypePreferenceResponse, checked: boolean): void {
    this.savePreference(pref.notificationType, { smsEnabled: checked });
    this.groups.update((gs) =>
      gs.map((g) => ({
        ...g,
        preferences: g.preferences.map((p) =>
          p.notificationType === pref.notificationType ? { ...p, smsEnabled: checked } : p,
        ),
      })),
    );
  }

  private savePreference(type: string, update: { emailEnabled?: boolean; smsEnabled?: boolean }): void {
    this.notificationService
      .updatePreference(type, update)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de sauvegarder la préférence.',
          });
          this.reloadPreferences();
        },
      });
  }

  private reloadPreferences(): void {
    this.notificationService
      .getPreferences()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((prefs) => this.groups.set(this.buildGroups(prefs)));
  }

  private buildGroups(prefs: NotificationTypePreferenceResponse[]): PreferenceGroup[] {
    return PREFERENCE_GROUPS.map((group) => ({
      ...group,
      preferences: prefs.filter((p) => group.types.includes(p.notificationType)),
    }));
  }
}
