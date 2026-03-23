import { Component, ChangeDetectionStrategy, input } from '@angular/core';

@Component({
  selector: 'app-admin-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-empty-state.html',
  styleUrl: './admin-empty-state.scss',
})
export class AdminEmptyState {
  icon = input('pi pi-check-circle');
  title = input('Tout est à jour');
  subtitle = input('Aucun élément à afficher.');
}
