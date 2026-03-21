import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';

@Component({
  selector: 'app-star-rating',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="star-rating" [attr.aria-label]="rating() + ' étoiles sur 5'">
      @for (star of stars(); track star.index) {
        <i
          class="pi"
          [class.pi-star-fill]="star.type === 'full'"
          [class.pi-star]="star.type === 'empty'"
          [style.color]="star.type === 'full' ? 'var(--color-ndb-gold)' : 'var(--color-ndb-border)'"
        ></i>
      }
      <span class="rating-value">{{ rating().toFixed(1) }}</span>
    </span>
  `,
  styles: `
    .star-rating {
      display: inline-flex;
      align-items: center;
      gap: 2px;
    }
    .rating-value {
      margin-left: 5px;
      font-size: 0.875rem;
      font-weight: 700;
      font-family: var(--font-urbanist);
      color: var(--color-ndb-body);
    }
  `,
})
export class StarRating {
  rating = input.required<number>();

  protected stars = computed(() => {
    const r = this.rating();
    return Array.from({ length: 5 }, (_, i) => ({
      index: i,
      type: i < Math.round(r) ? ('full' as const) : ('empty' as const),
    }));
  });
}
