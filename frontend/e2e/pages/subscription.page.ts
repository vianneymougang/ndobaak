import { type Page, type Locator, expect } from '@playwright/test';

export class SubscriptionPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly subscribeButton: Locator;
  readonly activeStatusBadge: Locator;
  readonly cancelButton: Locator;
  readonly momoDialog: Locator;
  readonly phoneInput: Locator;
  readonly confirmPayButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: /abonnement/i });
    this.subscribeButton = page.getByRole('button', { name: /s'abonner|souscrire/i }).first();
    this.activeStatusBadge = page.locator('.subscription-active, [data-status="ACTIVE"]');
    this.cancelButton = page.getByRole('button', { name: /annuler|résilier/i }).first();
    this.momoDialog = page.locator('p-dialog, .p-dialog');
    this.phoneInput = page.getByPlaceholder(/6[0-9]{8}/);
    this.confirmPayButton = page.getByRole('button', { name: /payer|confirmer/i }).last();
  }

  async goto() {
    await this.page.goto('/subscription');
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
