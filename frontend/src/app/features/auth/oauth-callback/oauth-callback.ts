import { Component, ChangeDetectionStrategy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { take } from 'rxjs';
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
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);

  protected status = signal<'loading' | 'error'>('loading');

  ngOnInit(): void {
    this.route.queryParams.pipe(take(1)).subscribe((params) => {
      const token = params['token'];
      const refreshToken = params['refreshToken'];

      if (token && refreshToken) {
        this.authService.handleOAuthCallback(token, refreshToken).subscribe({
          next: () => this.router.navigate(['/']),
          error: () => this.status.set('error'),
        });
      } else {
        this.status.set('error');
      }
    });
  }
}
