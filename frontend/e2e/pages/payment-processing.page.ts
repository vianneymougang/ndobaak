import { type Page, type Locator, expect } from '@playwright/test';

export class PaymentProcessingPage {
  readonly page: Page;
  readonly processingTitle: Locator;
  readonly phoneIcon: Locator;
  readonly elapsedTime: Locator;
  readonly cancelButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.processingTitle = page.locator('.processing-title');
    this.phoneIcon = page.locator('.phone-icon');
    this.elapsedTime = page.locator('.elapsed');
    this.cancelButton = page.getByRole('button', { name: 'Annuler' });
  }

  async expectPageLoaded() {
    await expect(this.page).toHaveURL(/\/pay\/processing/, { timeout: 10_000 });
  }

  async expectProcessingState() {
    await expect(this.processingTitle).toContainText('Confirmez sur votre téléphone');
    await expect(this.phoneIcon).toBeVisible();
  }
}
