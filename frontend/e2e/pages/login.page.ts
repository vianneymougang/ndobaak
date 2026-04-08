import { type Page, type Locator, expect } from '@playwright/test';

export class LoginPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly submitButton: Locator;
  readonly errorBanner: Locator;
  readonly fieldErrors: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.locator('h1');
    this.emailInput = page.locator('#loginEmail');
    this.passwordInput = page.locator('#loginPassword input');
    this.submitButton = page.getByRole('button', { name: 'Se connecter' });
    this.errorBanner = page.locator('.auth-error-banner');
    this.fieldErrors = page.locator('.field-error');
  }

  async goto() {
    await this.page.context().clearCookies();
    await this.page.goto('/login');
    await this.page.waitForLoadState('networkidle');
  }

  async login(email: string, password: string) {
    await this.emailInput.waitFor({ state: 'visible', timeout: 5_000 });
    await this.emailInput.click();
    await this.emailInput.fill(email);
    await this.passwordInput.click();
    await this.passwordInput.fill(password);
    await this.submitButton.click();
  }

  async expectHeading() {
    await expect(this.heading).toHaveText('Connexion');
  }

  async expectLoginSuccess() {
    await this.page.waitForURL(/\/(landlord|admin)?$|\/(landlord|admin)\//, { timeout: 10_000 });
  }

  async expectErrorBanner() {
    await expect(this.errorBanner).toBeVisible({ timeout: 10_000 });
  }

  async expectFieldErrors() {
    await expect(this.fieldErrors.first()).toBeVisible();
  }
}
