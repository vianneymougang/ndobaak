import { type Page, type Locator, expect } from '@playwright/test';

export class SearchPage {
  readonly page: Page;
  readonly filterBar: Locator;
  readonly searchInput: Locator;
  readonly searchButton: Locator;
  readonly mapContainer: Locator;
  readonly resultsGrid: Locator;
  readonly listingCards: Locator;
  readonly resultsCount: Locator;
  readonly emptyState: Locator;
  readonly locationBanner: Locator;
  readonly radiusSelector: Locator;
  readonly searchAreaButton: Locator;
  readonly paginator: Locator;
  readonly skeletons: Locator;

  constructor(page: Page) {
    this.page = page;
    this.filterBar = page.locator('.filter-bar');
    this.searchInput = page.locator('.filter-input-search');
    this.searchButton = page.locator('.filter-btn-search');
    this.mapContainer = page.locator('.map-container');
    this.resultsGrid = page.locator('.results-grid');
    this.listingCards = page.locator('app-listing-card');
    this.resultsCount = page.locator('.results-count');
    this.emptyState = page.locator('.empty-state');
    this.locationBanner = page.locator('.location-banner');
    this.radiusSelector = page.locator('.filter-radius p-selectbutton');
    this.searchAreaButton = page.locator('.search-area-btn');
    this.paginator = page.locator('p-paginator');
    this.skeletons = page.locator('.listing-skeleton');
  }

  async goto() {
    await this.page.goto('/');
    await this.page.waitForLoadState('networkidle');
    const searchLink = this.page.locator('a[href="/search"]').first();
    await searchLink.click();
    await this.page.waitForURL('**/search**', { timeout: 10_000 });
    await this.waitForSearchLoaded();
  }

  async waitForSearchLoaded() {
    await expect(this.filterBar).toBeVisible({ timeout: 10_000 });
    await this.page.waitForLoadState('networkidle');
  }

  async expectFilterBarVisible() {
    await expect(this.filterBar).toBeVisible();
    await expect(this.searchInput).toBeVisible();
    await expect(this.searchButton).toBeVisible();
  }

  async expectMapVisible() {
    await expect(this.mapContainer).toBeVisible({ timeout: 10_000 });
  }

  async expectResultsOrEmpty() {
    await expect(
      this.listingCards.first().or(this.emptyState)
    ).toBeVisible({ timeout: 15_000 });
  }

  async expectResultsCountVisible() {
    await expect(this.resultsCount).toBeVisible({ timeout: 10_000 });
  }

  async expectLocationBannerVisible() {
    await expect(this.locationBanner).toBeVisible({ timeout: 10_000 });
    await expect(this.locationBanner).toContainText('localisation');
  }

  async expectLocationBannerHidden() {
    await expect(this.locationBanner).not.toBeVisible();
  }

  async expectRadiusSelectorVisible() {
    await expect(this.radiusSelector).toBeVisible({ timeout: 10_000 });
  }

  async search(keyword: string) {
    await this.searchInput.fill(keyword);
    await this.searchButton.click();
    await this.page.waitForLoadState('networkidle');
  }

  async getListingCardCount(): Promise<number> {
    return this.listingCards.count();
  }

  async expectMapTilesRendered() {
    const leafletContainer = this.page.locator('.leaflet-container');
    await expect(leafletContainer.first()).toBeVisible({ timeout: 10_000 });
    const tiles = this.page.locator('.leaflet-tile-loaded');
    const tileCount = await tiles.count();
    if (tileCount > 0) {
      await expect(tiles.first()).toBeVisible();
    }
  }
}
