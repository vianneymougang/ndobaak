import { test, expect } from '@playwright/test';
import { SearchPage } from './pages/search.page';
import { DetailPage } from './pages/detail.page';

test.describe('Listing detail page', () => {

  test('should navigate from search card to detail page', async ({ page }) => {
    const searchPage = new SearchPage(page);
    await searchPage.goto();
    await searchPage.expectResultsOrEmpty();

    const count = await searchPage.getListingCardCount();
    test.skip(count === 0, 'No listings available to navigate to detail');

    await searchPage.listingCards.first().click();
    await page.waitForURL(/\/listings\/\d+/, { timeout: 10_000 });

    const detailPage = new DetailPage(page);
    await detailPage.expectLoaded();
    await detailPage.expectTitleVisible();
    await detailPage.expectPriceVisible();
  });

  test('should display map with tiles on detail page', async ({ page }) => {
    const searchPage = new SearchPage(page);
    await searchPage.goto();
    await searchPage.expectResultsOrEmpty();

    const count = await searchPage.getListingCardCount();
    test.skip(count === 0, 'No listings available to navigate to detail');

    await searchPage.listingCards.first().click();
    await page.waitForURL(/\/listings\/\d+/, { timeout: 10_000 });

    const detailPage = new DetailPage(page);
    await detailPage.expectLoaded();
    await detailPage.expectMapSectionVisible();
    await detailPage.expectMapTilesRendered();
  });

  test('should display reviews section on detail page', async ({ page }) => {
    const searchPage = new SearchPage(page);
    await searchPage.goto();
    await searchPage.expectResultsOrEmpty();

    const count = await searchPage.getListingCardCount();
    test.skip(count === 0, 'No listings available to navigate to detail');

    await searchPage.listingCards.first().click();
    await page.waitForURL(/\/listings\/\d+/, { timeout: 10_000 });

    const detailPage = new DetailPage(page);
    await detailPage.expectLoaded();
    await detailPage.expectReviewSectionVisible();
  });
});
