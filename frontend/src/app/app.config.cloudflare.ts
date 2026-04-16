import { ApplicationConfig, APP_INITIALIZER, PLATFORM_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { provideRouter, withInMemoryScrolling, TitleStrategy } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withFetch, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { of, catchError, timeout } from 'rxjs';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth-interceptor';
import { AuthService } from './core/services/auth';
import { AppTitleStrategy } from './core/strategies/app-title-strategy';

function initAuth(authService: AuthService, platformId: object) {
  return () => {
    if (!isPlatformBrowser(platformId)) return of(null);
    return authService.loadCurrentUser().pipe(timeout(5000), catchError(() => of(null)));
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'enabled' })),
    { provide: TitleStrategy, useClass: AppTitleStrategy },
    provideAnimationsAsync(),
    provideHttpClient(
      withFetch(),
      withInterceptors([authInterceptor]),
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
    ),
    {
      provide: APP_INITIALIZER,
      useFactory: (authService: AuthService, platformId: object) => initAuth(authService, platformId),
      deps: [AuthService, PLATFORM_ID],
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
  ],
};
