import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-admin-error-state',
  imports: [ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-error-state.html',
  styleUrl: './admin-error-state.scss',
})
export class AdminErrorState {
  icon = input('pi pi-exclamation-triangle');
  message = input('Impossible de charger les données.');
  retryLabel = input('Réessayer');
  retry = output<void>();
}
