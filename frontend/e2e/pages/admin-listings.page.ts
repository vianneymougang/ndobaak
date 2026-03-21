import { type Page, type Locator, expect } from '@playwright/test';

export class AdminListingsPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly listingsTable: Locator;
  readonly pendingTab: Locator;
  readonly approveButton: Locator;
  readonly rejectButton: Locator;
  readonly rejectionDialog: Locator;
  readonly rejectionMotifSelect: Locator;
  readonly confirmRejectButton: Locator;
  readonly successToast: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: /annonces/i });
    this.listingsTable = page.locator('p-table, .p-datatable');
    this.pendingTab = page.getByRole('tab', { name: /en attente/i });
    this.approveButton = page.getByRole('button', { name: /approuver|valider/i }).first();
    this.rejectButton = page.getByRole('button', { name: /rejeter/i }).first();
    this.rejectionDialog = page.locator('p-dialog, .p-dialog');
    this.rejectionMotifSelect = page.locator('p-select, p-dropdown').first();
    this.confirmRejectButton = page.getByRole('button', { name: /confirmer|rejeter/i }).last();
    this.successToast = page.locator('.p-toast-message-success');
  }

  async goto() {
    await this.page.goto('/admin/listings');
    await this.page.waitForLoadState('networkidle');
  }

  async expectPageLoaded() {
    await expect(this.heading).toBeVisible({ timeout: 10_000 });
  }

  async expectTableVisible() {
    await expect(this.listingsTable).toBeVisible({ timeout: 10_000 });
  }

  async clickPendingTab() {
    await this.pendingTab.click();
    await this.page.waitForLoadState('networkidle');
  }

  async approveFirstListing() {
    await this.approveButton.click();
    await this.page.waitForResponse((r) => r.url().includes('/listings/') && r.request().method() === 'POST');
  }

  async openRejectDialogForFirstListing() {
    await this.rejectButton.click();
    await expect(this.rejectionDialog).toBeVisible({ timeout: 5_000 });
  }

  async confirmRejection() {
    await this.confirmRejectButton.click();
  }

  async expectSuccessToast() {
    await expect(this.successToast).toBeVisible({ timeout: 10_000 });
  }
}
