import { ApplicationConfig, APP_INITIALIZER, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withInMemoryScrolling, TitleStrategy } from '@angular/router';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withFetch, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { of, catchError } from 'rxjs';
import { provideMotionConfig, provideMotionFeatures, ngmAnimationFeatures } from '@scripttype/ng-motion';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth-interceptor';
import { AuthService } from './core/services/auth';
import { AppTitleStrategy } from './core/strategies/app-title-strategy';

function initAuth(authService: AuthService) {
  return () => authService.loadCurrentUser().pipe(catchError(() => of(null)));
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'enabled' })),
    { provide: TitleStrategy, useClass: AppTitleStrategy },
    provideClientHydration(withEventReplay()),
    provideAnimationsAsync(),
    provideHttpClient(
      withFetch(),
      withInterceptors([authInterceptor]),
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
    ),
    {
      provide: APP_INITIALIZER,
      useFactory: (authService: AuthService) => initAuth(authService),
      deps: [AuthService],
      multi: true,
    },
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: false,
          cssLayer: {
            name: 'primeng',
            order: 'theme, base, primeng',
          },
        },
      },
    }),
    provideMotionConfig({ reducedMotion: 'user' }),
    provideMotionFeatures(ngmAnimationFeatures),
  ],
};
