import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { roleGuard } from './core/guards/role-guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Connexion',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    title: 'Créer un compte',
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
  },
  {
    path: 'auth/oauth2/callback',
    loadComponent: () =>
      import('./features/auth/oauth-callback/oauth-callback').then((m) => m.OauthCallback),
  },
  {
    path: 'auth/forgot-password',
    title: 'Mot de passe oublié',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'auth/reset-password',
    title: 'Réinitialiser le mot de passe',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password').then((m) => m.ResetPassword),
  },
  {
    path: '',
    loadComponent: () => import('./shared/components/layout/layout').then((m) => m.Layout),
    children: [
      {
        path: '',
        loadComponent: () => import('./features/home/home').then((m) => m.Home),
      },
      {
        path: 'search',
        title: 'Rechercher',
        loadComponent: () => import('./features/search/search').then((m) => m.Search),
      },
      {
        path: 'listings/create',
        title: 'Publier une annonce',
        canActivate: [authGuard, roleGuard('LANDLORD')],
        loadComponent: () => import('./features/listing/create/create').then((m) => m.Create),
      },
      {
        path: 'listings/:id',
        loadComponent: () => import('./features/listing/detail/detail').then((m) => m.Detail),
      },
      {
        path: 'my-listings',
        title: 'Mes annonces',
        canActivate: [authGuard, roleGuard('LANDLORD')],
        loadComponent: () =>
          import('./features/listing/my-listings/my-listings').then((m) => m.MyListings),
      },
      {
        path: 'my-bookings',
        title: 'Mes réservations',
        canActivate: [authGuard, roleGuard('STUDENT')],
        loadComponent: () =>
          import('./features/booking/my-bookings/my-bookings').then((m) => m.MyBookings),
      },
      {
        path: 'landlord/bookings',
        title: 'Demandes de réservation',
        canActivate: [authGuard, roleGuard('LANDLORD')],
        loadComponent: () =>
          import('./features/booking/landlord-bookings/landlord-bookings').then(
            (m) => m.LandlordBookings,
          ),
      },
      {
        path: 'landlord/dashboard',
        title: 'Tableau de bord',
        canActivate: [authGuard, roleGuard('LANDLORD')],
        loadComponent: () =>
          import('./features/dashboard/landlord-dashboard/landlord-dashboard').then(
            (m) => m.LandlordDashboard,
          ),
      },
      {
        path: 'notifications',
        title: 'Notifications',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/notification/notification-list/notification-list').then(
            (m) => m.NotificationList,
          ),
      },
      {
        path: 'bookings/:bookingId/pay',
        title: 'Paiement',
        canActivate: [authGuard, roleGuard('STUDENT')],
        loadComponent: () =>
          import('./features/payment/payment-form/payment-form').then((m) => m.PaymentForm),
      },
      {
        path: 'bookings/:bookingId/pay/processing',
        title: 'Paiement en cours',
        canActivate: [authGuard, roleGuard('STUDENT')],
        loadComponent: () =>
          import('./features/payment/payment-processing/payment-processing').then(
            (m) => m.PaymentProcessing,
          ),
      },
      {
        path: 'payments/:paymentId/success',
        title: 'Paiement confirmé',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/payment/payment-success/payment-success').then(
            (m) => m.PaymentSuccess,
          ),
      },
      {
        path: 'payments/:paymentId/failure',
        title: 'Paiement échoué',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/payment/payment-failure/payment-failure').then(
            (m) => m.PaymentFailure,
          ),
      },
      {
        path: 'payments/:paymentId/receipt',
        title: 'Reçu de paiement',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/payment/payment-receipt/payment-receipt').then(
            (m) => m.PaymentReceipt,
          ),
      },
      {
        path: 'transactions',
        title: 'Mes transactions',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/payment/transaction-history/transaction-history').then(
            (m) => m.TransactionHistory,
          ),
      },
      {
        path: 'bookings/:bookingId/installments',
        title: 'Échéancier de paiement',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/payment/installment-tracker/installment-tracker').then(
            (m) => m.InstallmentTracker,
          ),
      },
      {
        path: 'subscription',
        title: 'Mon abonnement',
        canActivate: [authGuard, roleGuard('LANDLORD')],
        loadComponent: () =>
          import('./features/subscription/subscription-page/subscription-page').then(
            (m) => m.SubscriptionPage,
          ),
      },
      {
        path: 'profile/notifications',
        title: 'Préférences de notification',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/profile/notification-preferences/notification-preferences').then(
            (m) => m.NotificationPreferences,
          ),
      },
    ],
  },
];
