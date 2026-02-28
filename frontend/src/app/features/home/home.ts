import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { environment } from '../../../environments/environment';
import { EMPTY, catchError } from 'rxjs';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { ListingService } from '../../core/services/listing';
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
    { label: '30 000 - 50 000 FCFA', value: '30000-50000' },
    { label: '50 000 - 80 000 FCFA', value: '50000-80000' },
    { label: 'Plus de 80 000 FCFA', value: '80000-' },
  ];

  protected selectedRoomType = '';
  protected selectedBudget = '';

  protected features = [
    {
      icon: 'pi-search',
      title: 'Recherchez',
      description: 'Trouvez le logement idéal près du campus avec nos filtres avancés : type, budget, distance.',
    },
    {
      icon: 'pi-verified',
      title: 'Annonces vérifiées',
      description: 'Toutes les annonces sont contrôlées par notre équipe avant publication. Aucune surprise.',
    },
    {
      icon: 'pi-home',
      title: 'Emménagez',
      description: 'Réservez en ligne et emménagez en toute sérénité avec le soutien de notre équipe.',
    },
  ];

  ngOnInit(): void {
    this.listingService
      .search({ sortBy: 'RATING_DESC', size: 3 })
      .pipe(catchError(() => EMPTY))
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
