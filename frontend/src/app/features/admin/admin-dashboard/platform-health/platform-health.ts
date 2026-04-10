import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  computed,
} from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';


@Component({
  selector: 'app-platform-health',
  imports: [SkeletonModule, ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './platform-health.html',
  styleUrl: './platform-health.scss',
})
export class PlatformHealth {
  score = input(0);
  status = input<'good' | 'warning' | 'critical'>('good');
  pendingCount = input(0);
  anomalyCount = input(0);
  failedCount = input(0);
  loading = input(false);
  error = input(false);
  retry = output<void>();

  protected statusLabel = computed(() => {
    switch (this.status()) {
      case 'good':
        return 'Tout va bien';
      case 'warning':
        return 'Attention requise';
      case 'critical':
        return 'Action urgente';
    }
  });

  protected statusIcon = computed(() => {
    switch (this.status()) {
      case 'good':
        return 'pi pi-check-circle';
      case 'warning':
        return 'pi pi-exclamation-circle';
      case 'critical':
        return 'pi pi-times-circle';
    }
  });

  protected strokeDashoffset = computed(() => {
    const circumference = 2 * Math.PI * 42;
    return circumference - (this.score() / 100) * circumference;
  });

  protected circumference = 2 * Math.PI * 42;
}
