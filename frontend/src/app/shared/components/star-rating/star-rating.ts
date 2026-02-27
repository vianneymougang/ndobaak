import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';

@Component({
  selector: 'app-star-rating',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="star-rating" [attr.aria-label]="rating() + ' out of 5 stars'">
      @for (star of stars(); track star.index) {
        <i
          class="pi"
          [class.pi-star-fill]="star.type === 'full'"
          [class.pi-star]="star.type === 'empty'"
          [style.color]="star.type === 'full' ? '#FFC107' : '#DEE2E6'"
        ></i>
      }
      <span class="rating-value">{{ rating().toFixed(1) }}</span>
    </span>
  `,
  styles: `
    .star-rating {
      display: inline-flex;
      align-items: center;
      gap: 1px;
    }
    .rating-value {
      margin-left: 4px;
      font-size: 0.875rem;
      font-weight: 600;
      color: #374151;
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
