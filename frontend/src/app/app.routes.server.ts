import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  { path: 'login', renderMode: RenderMode.Prerender },
  { path: 'register', renderMode: RenderMode.Prerender },
  { path: 'auth/forgot-password', renderMode: RenderMode.Prerender },
  { path: 'auth/reset-password', renderMode: RenderMode.Prerender },
  { path: 'auth/oauth2/callback', renderMode: RenderMode.Client },

  { path: 'landlord/listings/create', renderMode: RenderMode.Client },
  { path: 'landlord/listings/:id/edit', renderMode: RenderMode.Client },
  { path: 'my-bookings', renderMode: RenderMode.Client },
  { path: 'landlord', renderMode: RenderMode.Client },
  { path: 'landlord/dashboard', renderMode: RenderMode.Client },
  { path: 'landlord/bookings', renderMode: RenderMode.Client },
  { path: 'landlord/listings', renderMode: RenderMode.Client },
  { path: 'landlord/subscription', renderMode: RenderMode.Client },
  { path: 'landlord/transactions', renderMode: RenderMode.Client },
  { path: 'landlord/profile', renderMode: RenderMode.Client },
  { path: 'landlord/profile/notifications', renderMode: RenderMode.Client },
  { path: 'notifications', renderMode: RenderMode.Client },
  { path: 'transactions', renderMode: RenderMode.Client },
  { path: 'profile/notifications', renderMode: RenderMode.Client },

  { path: 'bookings/:bookingId/pay', renderMode: RenderMode.Client },
  { path: 'bookings/:bookingId/pay/processing', renderMode: RenderMode.Client },
  { path: 'bookings/:bookingId/installments', renderMode: RenderMode.Client },
  { path: 'payments/:paymentId/success', renderMode: RenderMode.Client },
  { path: 'payments/:paymentId/failure', renderMode: RenderMode.Client },
  { path: 'payments/:paymentId/receipt', renderMode: RenderMode.Client },

  { path: 'admin', renderMode: RenderMode.Client },
  { path: 'admin/dashboard', renderMode: RenderMode.Client },
  { path: 'admin/listings', renderMode: RenderMode.Client },
  { path: 'admin/listings/:id', renderMode: RenderMode.Client },
  { path: 'admin/users', renderMode: RenderMode.Client },
  { path: 'admin/users/:id', renderMode: RenderMode.Client },
  { path: 'admin/reviews', renderMode: RenderMode.Client },
  { path: 'admin/transactions', renderMode: RenderMode.Client },

  { path: 'listings/:id', renderMode: RenderMode.Server },
  { path: '**', renderMode: RenderMode.Server },
];
