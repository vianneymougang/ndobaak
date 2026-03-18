import { type Page, type Locator, expect } from '@playwright/test';

export class NavbarPage {
  readonly page: Page;
  readonly myBookingsLink: Locator;
  readonly landlordBookingsLink: Locator;
  readonly transactionsLink: Locator;
  readonly searchLink: Locator;
  readonly myListingsLink: Locator;
  readonly loginButton: Locator;
  readonly registerButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.myBookingsLink = page.getByRole('link', { name: 'Mes réservations' }).first();
    this.landlordBookingsLink = page.getByRole('link', { name: 'Réservations reçues' }).first();
    this.transactionsLink = page.getByRole('link', { name: 'Transactions' }).first();
    this.searchLink = page.getByRole('link', { name: 'Rechercher' }).first();
    this.myListingsLink = page.getByRole('link', { name: 'Mes annonces' }).first();
    this.loginButton = page.getByRole('link', { name: 'Se connecter' }).first();
    this.registerButton = page.getByRole('link', { name: "S'inscrire" }).first();
  }

  async goToMyBookings() {
    await this.myBookingsLink.click();
    await this.page.waitForURL('/my-bookings', { timeout: 10_000 });
  }

  async goToLandlordBookings() {
    await this.landlordBookingsLink.click();
    await this.page.waitForURL('/landlord/bookings', { timeout: 10_000 });
  }

  async goToTransactions() {
    await this.transactionsLink.click();
    await this.page.waitForURL('/transactions', { timeout: 10_000 });
  }

  async expectTransactionsLinkVisible() {
    await expect(this.transactionsLink).toBeVisible({ timeout: 10_000 });
  }

  async expectMyBookingsLinkVisible() {
    await expect(this.myBookingsLink).toBeVisible({ timeout: 10_000 });
  }

  async expectLandlordBookingsLinkVisible() {
    await expect(this.landlordBookingsLink).toBeVisible({ timeout: 10_000 });
  }
}
