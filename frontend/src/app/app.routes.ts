import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { roleGuard } from './core/guards/role-guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./shared/components/layout/layout').then((m) => m.Layout),
    children: [
      {
        path: '',
        loadComponent: () => import('./features/home/home').then((m) => m.Home),
      },
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
      },
      {
        path: 'register',
        loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
      },
      {
        path: 'search',
        loadComponent: () => import('./features/search/search').then((m) => m.Search),
      },
      {
        path: 'listings/:id',
        loadComponent: () => import('./features/listing/detail/detail').then((m) => m.Detail),
      },
      {
        path: 'my-listings',
        canActivate: [authGuard, roleGuard('LANDLORD')],
        loadComponent: () =>
          import('./features/listing/my-listings/my-listings').then((m) => m.MyListings),
      },
      {
        path: 'listings/create',
        canActivate: [authGuard, roleGuard('LANDLORD')],
        loadComponent: () => import('./features/listing/create/create').then((m) => m.Create),
      },
    ],
  },
];
