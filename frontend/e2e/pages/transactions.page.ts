import { type Page, type Locator, expect } from '@playwright/test';

export class TransactionsPage {
  readonly page: Page;
  readonly pageTitle: Locator;
  readonly emptyState: Locator;
  readonly transactionRows: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('.page-title');
    this.emptyState = page.locator('.empty-state');
    this.transactionRows = page.locator('.transaction-row');
  }

  async expectPageLoaded() {
    await expect(this.page).toHaveURL('/transactions');
  }

  async expectTitleVisible() {
    await expect(this.pageTitle).toHaveText('Mes transactions', { timeout: 10_000 });
  }

  async expectContentVisible() {
    await expect(
      this.page.locator('.empty-state, .transaction-row').first()
    ).toBeVisible({ timeout: 15_000 });
  }
}
