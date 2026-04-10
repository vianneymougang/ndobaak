import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';

import type { DashboardInsight } from '../admin-dashboard';
import { SectionHeader } from '../../../../shared/components/section-header/section-header';

@Component({
  selector: 'app-admin-insights',
  imports: [SkeletonModule, ButtonModule, SectionHeader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-insights.html',
  styleUrl: './admin-insights.scss',
})
export class AdminInsights {
  insights = input<DashboardInsight[]>([]);
  loading = input(false);
  error = input(false);
  retry = output<void>();
}
