import { Component, ChangeDetectionStrategy, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-oauth-callback',
  imports: [RouterLink, ButtonModule, ProgressSpinnerModule],
  templateUrl: './oauth-callback.html',
  styleUrl: './oauth-callback.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OauthCallback implements OnInit {
  private router = inject(Router);
  private authService = inject(AuthService);

  protected status = signal<'loading' | 'error'>('loading');

  ngOnInit(): void {
    this.authService.handleOAuthCallback().subscribe({
      next: () => {
        const role = this.authService.userRole();
        if (role === 'ADMIN') {
          this.router.navigate(['/admin']);
        } else if (role === 'LANDLORD') {
          this.router.navigate(['/landlord']);
        } else {
          this.router.navigate(['/']);
        }
      },
      error: () => this.status.set('error'),
    });
  }
}
