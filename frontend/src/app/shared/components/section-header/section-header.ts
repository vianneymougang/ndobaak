import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BadgeModule } from 'primeng/badge';

@Component({
  selector: 'app-section-header',
  imports: [RouterLink, BadgeModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="section-header">
      <div class="section-header-left">
        <h2 class="section-title">{{ title() }}</h2>
        @if (count() !== null && count()! > 0) {
          <p-badge
            [value]="'' + count()"
            [pt]="{
              root: { style: 'background: var(--color-ndb-gold); color: var(--color-ndb-forest); font-family: var(--font-urbanist); font-weight: 700; font-size: 0.6875rem; min-width: 1.375rem; height: 1.375rem; line-height: 1.375rem;' }
            }"
          />
        }
      </div>
      @if (viewAllLink()) {
        <a [routerLink]="viewAllLink()" [queryParams]="viewAllQueryParams()" class="section-view-all">
          Voir tout
          <i class="pi pi-arrow-right" aria-hidden="true"></i>
        </a>
      }
    </div>
  `,
  styles: `
    @reference "../../../../tailwind.css";

    .section-header {
      @apply flex items-center justify-between;
      margin-bottom: 1rem;
    }

    .section-header-left {
      @apply flex items-center;
      gap: 0.625rem;
    }

    .section-title {
      font-family: var(--font-urbanist);
      font-weight: 700;
      font-size: 1.125rem;
      color: var(--color-ndb-ink);
      margin: 0;
    }

    .section-view-all {
      @apply flex items-center;
      gap: 0.375rem;
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--color-ndb-gold-dark);
      text-decoration: none;
      transition: gap 0.2s ease;

      &:hover {
        gap: 0.625rem;
      }

      i {
        font-size: 0.6875rem;
        transition: transform 0.2s ease;
      }

      &:hover i {
        transform: translateX(2px);
      }
    }

  `,
})
export class SectionHeader {
  title = input.required<string>();
  count = input<number | null>(null);
  viewAllLink = input<string | null>(null);
  viewAllQueryParams = input<Record<string, string>>({});
}
