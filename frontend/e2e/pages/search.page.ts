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
  }

  async goto() {
    await this.page.goto('/search');
    await this.page.waitForLoadState('networkidle');
  }

  async expectFilterBarVisible() {
    await expect(this.filterBar).toBeVisible();
    await expect(this.searchInput).toBeVisible();
    await expect(this.searchButton).toBeVisible();
  }

  async expectMapVisible() {
    await expect(this.mapContainer).toBeVisible();
  }

  async expectResultsOrEmpty() {
    const hasResults = await this.listingCards.first().isVisible().catch(() => false);
    const hasEmpty = await this.emptyState.isVisible().catch(() => false);
    expect(hasResults || hasEmpty).toBeTruthy();
  }

  async expectResultsCountVisible() {
    await expect(this.resultsCount).toBeVisible();
  }

  async expectLocationBannerVisible() {
    await expect(this.locationBanner).toBeVisible();
    await expect(this.locationBanner).toContainText('localisation');
  }

  async expectLocationBannerHidden() {
    await expect(this.locationBanner).not.toBeVisible();
  }

  async expectRadiusSelectorVisible() {
    await expect(this.radiusSelector).toBeVisible();
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
    const tiles = this.page.locator('.leaflet-tile-loaded');
    await expect(tiles.first()).toBeVisible({ timeout: 10_000 });
  }
}
