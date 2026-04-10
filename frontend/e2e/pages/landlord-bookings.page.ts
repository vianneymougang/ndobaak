import { type Page, type Locator, expect } from '@playwright/test';

export class LandlordBookingsPage {
  readonly page: Page;
  readonly bookingCards: Locator;
  readonly emptyState: Locator;

  constructor(page: Page) {
    this.page = page;
    this.bookingCards = page.locator('.booking-card');
    this.emptyState = page.locator('.empty-state');
  }

  async goto() {
    await this.page.goto('/landlord/bookings');
    await this.page.waitForLoadState('networkidle');
  }

  async expectPageLoaded() {
    await expect(this.page).toHaveURL('/landlord/bookings');
  }

  async expectContentVisible() {
    await expect(
      this.page.locator('.booking-card, .empty-state, [class*="booking"]').first()
    ).toBeVisible({ timeout: 15_000 });
  }
}
