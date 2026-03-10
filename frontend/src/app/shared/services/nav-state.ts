import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class NavState {
  readonly drawerOpen = signal(false);
}
