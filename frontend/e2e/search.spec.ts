import { test, expect } from '@playwright/test';
import { SearchPage } from './pages/search.page';

test.describe('Search page', () => {
  let searchPage: SearchPage;

  test.beforeEach(async ({ page }) => {
    searchPage = new SearchPage(page);
  });

  test('should load search page with filter bar and map', async ({ page }) => {
    await searchPage.goto();
    await searchPage.expectFilterBarVisible();
    await searchPage.expectResultsOrEmpty();
  });

  test('should render map with OpenStreetMap tiles', async ({ page }) => {
    await searchPage.goto();
    await searchPage.expectMapVisible();
    await searchPage.expectMapTilesRendered();
  });

  test('should display results or empty state', async ({ page }) => {
    await searchPage.goto();
    await searchPage.expectResultsOrEmpty();
  });

  test('should search by keyword', async ({ page }) => {
    await searchPage.goto();
    await searchPage.search('studio');
    await searchPage.expectResultsOrEmpty();
  });

  test('should show location banner when geolocation denied', async ({ context, page }) => {
    await context.grantPermissions([]);
    await context.clearPermissions();

    searchPage = new SearchPage(page);
    await page.goto('/search');

    await page.evaluate(() => {
      const originalGetCurrentPosition = navigator.geolocation.getCurrentPosition;
      navigator.geolocation.getCurrentPosition = function (_success, error) {
        if (error) {
          error({
            code: 1,
            message: 'User denied Geolocation',
            PERMISSION_DENIED: 1,
            POSITION_UNAVAILABLE: 2,
            TIMEOUT: 3,
          } as GeolocationPositionError);
        }
      };
    });

    await page.reload();
    await page.waitForLoadState('networkidle');

    await searchPage.expectLocationBannerVisible();
  });

  test('should show radius selector when geolocation granted', async ({ context, page }) => {
    await context.grantPermissions(['geolocation']);
    await context.setGeolocation({ latitude: 3.848, longitude: 11.502 });

    searchPage = new SearchPage(page);
    await searchPage.goto();

    await searchPage.expectLocationBannerHidden();
    await searchPage.expectRadiusSelectorVisible();
  });

  test('should show "search this area" button after map drag', async ({ context, page }) => {
    await context.grantPermissions(['geolocation']);
    await context.setGeolocation({ latitude: 3.848, longitude: 11.502 });

    searchPage = new SearchPage(page);
    await searchPage.goto();
    await searchPage.expectMapVisible();

    const mapEl = searchPage.mapContainer;
    const box = await mapEl.boundingBox();
    if (box) {
      await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
      await page.mouse.down();
      await page.mouse.move(box.x + box.width / 2 + 80, box.y + box.height / 2, { steps: 5 });
      await page.mouse.up();
    }

    await expect(searchPage.searchAreaButton).toBeVisible({ timeout: 5_000 });
    await expect(searchPage.searchAreaButton).toContainText('Rechercher dans cette zone');
  });

  test('listing cards should display distance label', async ({ page }) => {
    await searchPage.goto();

    const hasResults = await searchPage.listingCards.first().isVisible().catch(() => false);
    if (hasResults) {
      const firstCard = searchPage.listingCards.first();
      const distanceText = firstCard.locator('text=/km/');
      await expect(distanceText).toBeVisible();
    }
  });
});
