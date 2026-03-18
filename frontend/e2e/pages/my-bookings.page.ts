import { type Page, type Locator, expect } from '@playwright/test';

export class MyBookingsPage {
  readonly page: Page;
  readonly pageTitle: Locator;
  readonly tabs: Locator;
  readonly bookingCards: Locator;
  readonly emptyState: Locator;
  readonly payButton: Locator;
  readonly cardTitles: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('.page-title');
    this.tabs = page.locator('[role="tab"]');
    this.bookingCards = page.locator('.booking-card');
    this.emptyState = page.locator('.empty-state');
    this.payButton = page.getByRole('button', { name: 'Payer' }).first();
    this.cardTitles = page.locator('.card-title');
  }

  async expectPageLoaded() {
    await expect(this.page).toHaveURL('/my-bookings');
  }

  async expectTabsVisible() {
    await expect(this.tabs.first()).toBeVisible({ timeout: 10_000 });
  }

  async expectBookingCardsVisible() {
    await expect(this.bookingCards.first()).toBeVisible({ timeout: 15_000 });
  }

  async expectBookingCardHasTitle() {
    await expect(this.cardTitles.first()).not.toBeEmpty();
  }

  async waitForCards() {
    await this.page.waitForSelector('.booking-card', { timeout: 15_000 });
  }

  async hasPayButton(): Promise<boolean> {
    return this.payButton.isVisible({ timeout: 5_000 }).catch(() => false);
  }

  async expectPayButtonVisible() {
    await expect(this.payButton).toBeVisible({ timeout: 5_000 });
  }

  async clickPayButton() {
    await this.payButton.click();
  }

  async navigateToPayment() {
    await this.waitForCards();
    await this.clickPayButton();
    await expect(this.page).toHaveURL(/\/bookings\/\d+\/pay$/, { timeout: 10_000 });
  }
}
