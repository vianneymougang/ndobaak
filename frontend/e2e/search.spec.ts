import { test, expect } from '@playwright/test';
import { SearchPage } from './pages/search.page';

test.describe('Search page', () => {

  test('should load search page with filter bar', async ({ page }) => {
    const searchPage = new SearchPage(page);
    await searchPage.goto();
    await searchPage.expectFilterBarVisible();
  });

  test('should render map with OpenStreetMap tiles', async ({ page }) => {
    const searchPage = new SearchPage(page);
    await searchPage.goto();
    await searchPage.expectMapVisible();
    await searchPage.expectMapTilesRendered();
  });

  test('should display results or empty state', async ({ page }) => {
    const searchPage = new SearchPage(page);
    await searchPage.goto();
    await searchPage.expectResultsOrEmpty();
  });

  test('should search by keyword', async ({ page }) => {
    const searchPage = new SearchPage(page);
    await searchPage.goto();
    await searchPage.search('studio');
    await searchPage.expectResultsOrEmpty();
  });

  test('should show location banner when geolocation denied', async ({ context, page }) => {
    await context.grantPermissions([]);

    const searchPage = new SearchPage(page);
    await searchPage.goto();

    await searchPage.expectLocationBannerVisible();
  });

  test('should show radius selector when geolocation granted', async ({ context, page }) => {
    await context.grantPermissions(['geolocation']);
    await context.setGeolocation({ latitude: 3.848, longitude: 11.502 });

    const searchPage = new SearchPage(page);
    await searchPage.goto();

    await searchPage.expectLocationBannerHidden();
    await searchPage.expectRadiusSelectorVisible();
  });

  test('should show "search this area" button after map drag', async ({ context, page }) => {
    await context.grantPermissions(['geolocation']);
    await context.setGeolocation({ latitude: 3.848, longitude: 11.502 });

    const searchPage = new SearchPage(page);
    await searchPage.goto();
    await searchPage.expectMapVisible();

    await page.evaluate(() => {
      const map = (window as any).__ndb_map;
      if (map) map.panBy([200, 0]);
    });
    await page.waitForTimeout(1000);

    await expect(searchPage.searchAreaButton).toBeVisible({ timeout: 10_000 });
    await expect(searchPage.searchAreaButton).toContainText('Rechercher dans cette zone');
  });

  test('listing cards should display distance label', async ({ page }) => {
    const searchPage = new SearchPage(page);
    await searchPage.goto();
    await searchPage.expectResultsOrEmpty();

    const count = await searchPage.getListingCardCount();
    if (count > 0) {
      const firstCard = searchPage.listingCards.first();
      const distanceText = firstCard.locator('text=/km/');
      await expect(distanceText).toBeVisible({ timeout: 5_000 });
    }
  });
});
