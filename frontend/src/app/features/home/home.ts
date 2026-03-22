import { Component, ChangeDetectionStrategy, inject, signal, computed, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { environment } from '../../../environments/environment';
import { EMPTY, catchError } from 'rxjs';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { ListingService } from '../../core/services/listing';
import { AuthService } from '../../core/services/auth';
import { ListingSearchResponse } from '../../core/models/listing.models';
import { ListingCard } from '../../shared/components/listing-card/listing-card';

@Component({
  selector: 'app-home',
  imports: [ButtonModule, SelectModule, FormsModule, ListingCard, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home implements OnInit {
  private router = inject(Router);
  private listingService = inject(ListingService);
  private authService = inject(AuthService);
  private destroyRef = inject(DestroyRef);

  protected isLandlord = computed(() => this.authService.userRole() === 'LANDLORD');
  protected isStudent = computed(() => this.authService.userRole() === 'STUDENT');

  protected readonly heroImageUrl = `url('${environment.mediaBaseUrl}/static/hero.jpg')`;

  protected popularListings = signal<ListingSearchResponse[]>([]);

  protected roomTypes = [
    { label: 'Tous les types', value: '' },
    { label: 'Studio', value: 'STUDIO' },
    { label: 'Chambre simple', value: 'SINGLE_ROOM' },
    { label: 'Chambre double', value: 'DOUBLE_ROOM' },
    { label: 'Appartement', value: 'APARTMENT' },
  ];

  protected budgetRanges = [
    { label: 'Tous les budgets', value: '' },
    { label: 'Moins de 30 000 FCFA', value: '0-30000' },
    { label: '30 000 – 50 000 FCFA', value: '30000-50000' },
    { label: '50 000 – 80 000 FCFA', value: '50000-80000' },
    { label: 'Plus de 80 000 FCFA', value: '80000-' },
  ];

  protected selectedRoomType = '';
  protected selectedBudget = '';

  protected features = [
    {
      icon: 'pi-search',
      title: 'Recherchez',
      description:
        "Filtrez par type, budget et distance du campus pour trouver le logement idéal près de l'IUSJC.",
    },
    {
      icon: 'pi-verified',
      title: 'Annonces vérifiées',
      description:
        "Chaque annonce est contrôlée par notre équipe avant publication. Zéro surprise à l'arrivée.",
    },
    {
      icon: 'pi-home',
      title: 'Emménagez',
      description:
        "Réservez en ligne et payez par Mobile Money. Notre équipe vous accompagne jusqu'à l'emménagement.",
    },
  ];

  protected stats = [
    { value: '150+', label: 'Logements disponibles', icon: 'pi-home' },
    { value: '1 500', label: 'Étudiants logés', icon: 'pi-users' },
    { value: '100%', label: 'Annonces vérifiées', icon: 'pi-verified' },
    { value: '50+', label: 'Propriétaires actifs', icon: 'pi-star' },
  ];

  ngOnInit(): void {
    this.listingService
      .search({ sortBy: 'RATING_DESC', size: 3 })
      .pipe(catchError(() => EMPTY), takeUntilDestroyed(this.destroyRef))
      .subscribe((res) => this.popularListings.set(res.content));
  }

  protected onSearch(): void {
    const params: Record<string, string> = {};
    if (this.selectedRoomType) params['roomType'] = this.selectedRoomType;
    if (this.selectedBudget) {
      const [min, max] = this.selectedBudget.split('-');
      if (min) params['priceMin'] = min;
      if (max) params['priceMax'] = max;
    }
    this.router.navigate(['/search'], { queryParams: params });
  }
}
