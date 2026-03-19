import { type Page, type Locator, expect } from '@playwright/test';

export class AdminReviewsPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly pendingTab: Locator;
  readonly flaggedTab: Locator;
  readonly allTab: Locator;
  readonly approveButton: Locator;
  readonly rejectButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: /avis|modération/i });
    this.pendingTab = page.getByRole('tab', { name: /en attente/i });
    this.flaggedTab = page.getByRole('tab', { name: /signalés/i });
    this.allTab = page.getByRole('tab', { name: /tous/i });
    this.approveButton = page.getByRole('button', { name: /approuver/i }).first();
    this.rejectButton = page.getByRole('button', { name: /rejeter/i }).first();
  }

  async goto() {
    await this.page.goto('/admin/reviews');
    await this.page.waitForLoadState('networkidle');
  }

  async expectPageLoaded() {
    await expect(this.heading).toBeVisible({ timeout: 10_000 });
  }

  async clickPendingTab() {
    await this.pendingTab.click();
    await this.page.waitForLoadState('networkidle');
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
