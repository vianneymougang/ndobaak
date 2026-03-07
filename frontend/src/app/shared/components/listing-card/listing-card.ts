import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SlicePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ChipModule } from 'primeng/chip';
import { ListingSearchResponse } from '../../../core/models/listing.models';

@Component({
  selector: 'app-listing-card',
  imports: [RouterLink, SlicePipe, ButtonModule, TagModule, ChipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './listing-card.html',
  styleUrl: './listing-card.scss',
})
export class ListingCard {
  listing = input.required<ListingSearchResponse>();

  protected roomTypeLabel = computed(() => {
    const labels: Record<string, string> = {
      STUDIO: 'Studio',
      SINGLE_ROOM: 'Chambre simple',
      DOUBLE_ROOM: 'Chambre double',
      APARTMENT: 'Appartement',
    };
    return labels[this.listing().roomType] ?? this.listing().roomType;
  });

  protected formattedPrice = computed(() => {
    return new Intl.NumberFormat('fr-FR').format(this.listing().monthlyPrice) + ' FCFA/mois';
  });

  protected distanceLabel = computed(() => {
    const d = this.listing().distanceToCampus;
    return d != null ? d.toFixed(1) + ' km du campus' : '';
  });
}
