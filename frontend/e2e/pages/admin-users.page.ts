import { type Page, type Locator, expect } from '@playwright/test';

export class AdminUsersPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly usersTable: Locator;
  readonly searchInput: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: /communauté/i });
    this.usersTable = page.locator('table, p-table, .p-datatable');
    this.searchInput = page.getByPlaceholder(/rechercher/i).first();
  }

  async goto() {
    await this.page.goto('/admin/community');
    await this.page.waitForLoadState('networkidle');
  }

  async expectPageLoaded() {
    await expect(this.heading).toBeVisible({ timeout: 10_000 });
  }

  async expectTableVisible() {
    // Community page uses a custom list with user rows
    await expect(this.page.getByText('mbarga@test.ndobaak.com').first()).toBeVisible({ timeout: 10_000 });
  }

  async searchUser(query: string) {
    await this.searchInput.fill(query);
    await this.page.waitForTimeout(500);
  }

  async expectRowsVisible() {
    const rows = this.page.locator('table tbody tr, .user-row').first();
    await expect(rows).toBeVisible({ timeout: 10_000 });
  }
}
