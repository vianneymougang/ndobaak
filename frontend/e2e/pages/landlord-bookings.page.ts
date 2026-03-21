import { type Page, type Locator, expect } from '@playwright/test';

export class LandlordBookingsPage {
  readonly page: Page;
  readonly pageTitle: Locator;
  readonly bookingCards: Locator;
  readonly emptyState: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('.page-title');
    this.bookingCards = page.locator('.booking-card');
    this.emptyState = page.locator('.empty-state');
  }

  async expectPageLoaded() {
    await expect(this.page).toHaveURL('/landlord/bookings');
  }

  async expectContentVisible() {
    await expect(
      this.page.locator('.booking-card, .empty-state').first()
    ).toBeVisible({ timeout: 15_000 });
  }
}
