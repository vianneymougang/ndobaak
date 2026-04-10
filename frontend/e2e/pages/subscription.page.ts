import { type Page, type Locator, expect } from '@playwright/test';

export class SubscriptionPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly subscribeButton: Locator;
  readonly momoDialog: Locator;
  readonly operatorSelect: Locator;
  readonly phoneInput: Locator;
  readonly confirmPayButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.locator('h1, h2').filter({ hasText: /abonnement/i }).first();
    this.subscribeButton = page.getByRole('button', { name: /s'abonner/i }).first();
    this.momoDialog = page.locator('p-dialog .p-dialog-content, .p-dialog-content');
    this.operatorSelect = page.locator('p-select, p-dropdown').first();
    this.phoneInput = page.locator('input[placeholder*="6XX"], input[type="tel"]').first();
    this.confirmPayButton = page.getByRole('button', { name: /s'abonner/i }).last();
  }

  async goto() {
    await this.page.goto('/landlord/subscription');
    await this.page.waitForLoadState('networkidle');
  }

  async expectPageLoaded() {
    await expect(this.heading).toBeVisible({ timeout: 10_000 });
  }

  async expectSubscribeButtonVisible() {
    await expect(this.subscribeButton).toBeVisible({ timeout: 10_000 });
  }

  async clickSubscribe() {
    await this.subscribeButton.click();
  }

  async expectMomoDialogOpen() {
    await expect(this.momoDialog).toBeVisible({ timeout: 5_000 });
  }

  async fillPhoneAndConfirm(phone: string) {
    await this.phoneInput.fill(phone);
    await this.confirmPayButton.click();
  }
}
