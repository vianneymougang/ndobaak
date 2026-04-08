import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CanActivateFn, Router } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs';
import { AuthService } from '../services/auth';

export const guestGuard: CanActivateFn = () => {
  const platformId = inject(PLATFORM_ID);
  if (!isPlatformBrowser(platformId)) return true;

  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.authReady()) {
    return authService.isAuthenticated() ? router.createUrlTree(['/']) : true;
  }

  return toObservable(authService.authReady).pipe(
    filter(Boolean),
    take(1),
    map(() => authService.isAuthenticated() ? router.createUrlTree(['/']) : true),
  );
};
