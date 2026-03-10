import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  { path: 'login', renderMode: RenderMode.Prerender },
  { path: 'register', renderMode: RenderMode.Prerender },
  { path: 'auth/forgot-password', renderMode: RenderMode.Prerender },
  { path: 'auth/reset-password', renderMode: RenderMode.Prerender },
  { path: 'auth/oauth2/callback', renderMode: RenderMode.Client },
  { path: 'listings/create', renderMode: RenderMode.Client },
  { path: 'my-listings', renderMode: RenderMode.Client },
  { path: 'listings/:id', renderMode: RenderMode.Server },
  { path: '**', renderMode: RenderMode.Server },
];
