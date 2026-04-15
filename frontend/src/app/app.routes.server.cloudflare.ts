import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  { path: 'login', renderMode: RenderMode.Prerender },
  { path: 'register', renderMode: RenderMode.Prerender },
  { path: 'auth/forgot-password', renderMode: RenderMode.Prerender },
  { path: 'auth/reset-password', renderMode: RenderMode.Prerender },

  { path: '**', renderMode: RenderMode.Client },
];
