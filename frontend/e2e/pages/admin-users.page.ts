import { type Page, type Locator, expect } from '@playwright/test';

export class AdminUsersPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly usersTable: Locator;
  readonly searchInput: Locator;
  readonly deactivateButton: Locator;
  readonly reactivateButton: Locator;
  readonly confirmDialog: Locator;
  readonly confirmButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: /utilisateurs/i });
    this.usersTable = page.locator('p-table, .p-datatable');
    this.searchInput = page.getByPlaceholder(/rechercher/i).first();
    this.deactivateButton = page.getByRole('button', { name: /désactiver/i }).first();
    this.reactivateButton = page.getByRole('button', { name: /réactiver/i }).first();
    this.confirmDialog = page.locator('p-confirmdialog, .p-confirmdialog');
    this.confirmButton = page.getByRole('button', { name: /oui|confirmer/i }).last();
  }

  async goto() {
    await this.page.goto('/admin/users');
    await this.page.waitForLoadState('networkidle');
  }

  async expectPageLoaded() {
    await expect(this.heading).toBeVisible({ timeout: 10_000 });
  }

  async expectTableVisible() {
    await expect(this.usersTable).toBeVisible({ timeout: 10_000 });
  }

  async searchUser(query: string) {
    await this.searchInput.fill(query);
    await this.page.waitForTimeout(500);
  }

  async expectRowsVisible() {
    const rows = this.page.locator('tr.p-row, tbody tr').first();
    await expect(rows).toBeVisible({ timeout: 10_000 });
  }
}
