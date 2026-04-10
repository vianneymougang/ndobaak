import { type Page, type Locator, expect } from '@playwright/test';

export class AdminListingsPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly pendingTab: Locator;
  readonly approveButton: Locator;
  readonly rejectButton: Locator;
  readonly rejectionDialog: Locator;
  readonly confirmRejectButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: /annonces/i });
    this.pendingTab = page.getByRole('button', { name: /en attente/i });
    this.approveButton = page.locator('button').filter({ hasText: /approuver/i });
    this.rejectButton = page.locator('button').filter({ hasText: /rejeter/i });
    this.rejectionDialog = page.locator('.p-dialog').filter({ hasText: /refuser/i });
    this.confirmRejectButton = page.locator('button').filter({ hasText: /confirmer le refus/i });
  }

  async goto() {
    await this.page.goto('/admin/listings');
    await this.page.waitForLoadState('networkidle');
  }

  async expectPageLoaded() {
    await expect(this.heading).toBeVisible({ timeout: 10_000 });
  }

  async expectTableVisible() {
    await expect(this.page.locator('text=Jean-Baptiste Mbarga').first()).toBeVisible({ timeout: 10_000 });
  }

  async clickPendingTab() {
    await this.pendingTab.click();
    await this.page.waitForLoadState('networkidle');
  }

  async openFirstListingDrawer() {
    await this.page.locator('button.p-button-icon-only').first().click();
    await this.page.waitForTimeout(500);
  }

  async approveFirstListing() {
    await this.openFirstListingDrawer();
    await expect(this.approveButton).toBeVisible({ timeout: 5_000 });
    await this.approveButton.click();
    await this.page.waitForTimeout(1_000);
    await this.page.waitForLoadState('networkidle');
  }

  async openRejectDialogForFirstListing() {
    await this.openFirstListingDrawer();
    await expect(this.rejectButton).toBeVisible({ timeout: 5_000 });
    await this.rejectButton.click();
    await expect(this.rejectionDialog).toBeVisible({ timeout: 5_000 });
  }

  async confirmRejection() {
    // Select first motif option in PrimeNG select
    await this.page.locator('p-select[formcontrolname="motif"]').click();
    await this.page.waitForTimeout(300);
    await this.page.locator('.p-select-option').first().click();
    await this.confirmRejectButton.click();
    await this.page.waitForTimeout(1_000);
    await this.page.waitForLoadState('networkidle');
  }

  async expectSuccessToast() {
    // Drawer stays open after action — badge changes to Publiée or Refusée
    await expect(
      this.page.getByText(/publiée|refusée/i).first()
    ).toBeVisible({ timeout: 10_000 });
  }
}
