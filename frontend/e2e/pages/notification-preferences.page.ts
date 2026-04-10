import { type Page, type Locator, expect } from '@playwright/test';

export class NotificationPreferencesPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly toggles: Locator;
  readonly emailToggles: Locator;
  readonly smsToggles: Locator;
  readonly skeletons: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: /notifications|préférences/i });
    this.toggles = page.locator('p-toggleswitch, .p-toggleswitch');
    this.emailToggles = page.locator('[data-channel="email"] p-toggleswitch, .email-toggle');
    this.smsToggles = page.locator('[data-channel="sms"] p-toggleswitch, .sms-toggle');
    this.skeletons = page.locator('p-skeleton');
  }

  async goto(role: 'student' | 'landlord' = 'student') {
    const path = role === 'landlord' ? '/landlord/profile/notifications' : '/profile/notifications';
    await this.page.goto(path);
    await this.page.waitForLoadState('networkidle');
  }

  async expectPageLoaded() {
    await expect(this.heading).toBeVisible({ timeout: 10_000 });
  }

  async waitForPreferencesLoaded() {
    await expect(this.skeletons).toHaveCount(0, { timeout: 10_000 });
    await expect(this.toggles.first()).toBeVisible({ timeout: 10_000 });
  }

  async expectTogglesVisible() {
    await expect(this.toggles.first()).toBeVisible({ timeout: 10_000 });
  }

  async toggleFirstEmailPreference() {
    await this.toggles.first().click();
    await this.page.waitForTimeout(300);
  }
}
