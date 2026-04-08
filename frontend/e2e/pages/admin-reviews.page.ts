import { type Page, type Locator, expect } from '@playwright/test';

export class AdminReviewsPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly pendingTab: Locator;
  readonly flaggedTab: Locator;
  readonly anomaliesTab: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: /modération/i });
    // Tabs are rendered as buttons/links, not role="tab" in this UI
    this.pendingTab = page.getByRole('tab', { name: /annonces en attente/i })
      .or(page.locator('.p-tablist-tab-list').getByText(/annonces en attente/i));
    this.flaggedTab = page.getByRole('tab', { name: /avis à modérer/i })
      .or(page.locator('.p-tablist-tab-list').getByText(/avis à modérer/i));
    this.anomaliesTab = page.getByRole('tab', { name: /anomalies/i })
      .or(page.locator('.p-tablist-tab-list').getByText(/anomalies/i));
  }

  async goto() {
    await this.page.goto('/admin/moderation');
    await this.page.waitForLoadState('networkidle');
  }

  async expectPageLoaded() {
    await expect(this.heading).toBeVisible({ timeout: 10_000 });
  }

  async clickFlaggedTab() {
    await this.flaggedTab.click();
    await this.page.waitForLoadState('networkidle');
  }

  async expectTabsVisible() {
    await expect(this.pendingTab).toBeVisible({ timeout: 10_000 });
    await expect(this.flaggedTab).toBeVisible({ timeout: 10_000 });
  }
}
