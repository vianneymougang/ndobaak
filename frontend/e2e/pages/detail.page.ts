import { type Page, type Locator, expect } from '@playwright/test';

export class DetailPage {
  readonly page: Page;
  readonly detailPage: Locator;
  readonly title: Locator;
  readonly price: Locator;
  readonly photoGrid: Locator;
  readonly mapSection: Locator;
  readonly mapContainer: Locator;
  readonly reviewSection: Locator;
  readonly reviewList: Locator;
  readonly amenities: Locator;
  readonly loadingSpinner: Locator;

  constructor(page: Page) {
    this.page = page;
    this.detailPage = page.locator('.detail-page');
    this.title = page.locator('.detail-title');
    this.price = page.locator('.detail-price');
    this.photoGrid = page.locator('.photo-grid');
    this.mapSection = page.getByRole('heading', { name: 'Localisation' });
    this.mapContainer = page.locator('.detail-map');
    this.reviewSection = page.getByRole('heading', { name: 'Avis' });
    this.reviewList = page.locator('app-review-list');
    this.amenities = page.locator('.amenity-chip');
    this.loadingSpinner = page.locator('p-progressSpinner');
  }

  async expectLoaded() {
    await expect(this.detailPage).toBeVisible({ timeout: 10_000 });
  }

  async expectTitleVisible() {
    await expect(this.title).toBeVisible();
  }

  async expectPriceVisible() {
    await expect(this.price).toBeVisible();
    await expect(this.price).toContainText('FCFA');
  }

  async expectMapSectionVisible() {
    await expect(this.mapSection).toBeVisible();
    await expect(this.mapContainer).toBeVisible();
  }

  async expectMapTilesRendered() {
    const tiles = this.page.locator('.detail-map .leaflet-tile-loaded');
    await expect(tiles.first()).toBeVisible({ timeout: 10_000 });
  }

  async expectReviewSectionVisible() {
    await expect(this.reviewSection).toBeVisible();
    await expect(this.reviewList).toBeVisible();
  }
}
