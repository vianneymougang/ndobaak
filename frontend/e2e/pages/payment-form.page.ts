import { type Page, type Locator, expect, test } from '@playwright/test';

export class PaymentFormPage {
  readonly page: Page;
  readonly pageTitle: Locator;
  readonly summaryCard: Locator;
  readonly summaryTitle: Locator;
  readonly activeTypeCard: Locator;
  readonly typeCards: Locator;
  readonly frequencyDropdown: Locator;
  readonly operatorDropdown: Locator;
  readonly phoneInput: Locator;
  readonly submitButton: Locator;
  readonly fieldErrors: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('.page-title');
    this.summaryCard = page.locator('.summary-card');
    this.summaryTitle = page.locator('.summary-title');
    this.activeTypeCard = page.locator('.type-card--active');
    this.typeCards = page.locator('.type-card');
    this.frequencyDropdown = page.locator('#frequency');
    this.operatorDropdown = page.locator('#operator');
    this.phoneInput = page.locator('#phone');
    this.submitButton = page.locator('.submit-desktop p-button');
    this.fieldErrors = page.locator('.field-error');
  }

  async expectPageLoaded() {
    await expect(this.page).toHaveURL(/\/bookings\/\d+\/pay$/);
    await expect(this.pageTitle).toHaveText('Paiement');
  }

  async expectSummaryVisible() {
    await expect(this.summaryCard).toBeVisible();
    await expect(this.summaryTitle).not.toBeEmpty();
  }

  async expectIntegralSelectedByDefault() {
    await expect(this.activeTypeCard).toBeVisible();
    await expect(this.activeTypeCard).toContainText('Paiement intégral');
  }

  async selectInstallmentType() {
    await this.typeCards.nth(1).click();
  }

  async expectFrequencyDropdownVisible() {
    await expect(this.frequencyDropdown).toBeVisible();
  }

  async selectOperator(name: string) {
    await this.operatorDropdown.click();
    await this.page.getByText(name).click();
  }

  async fillPhone(phone: string) {
    await this.phoneInput.fill(phone);
  }

  async submit() {
    await this.submitButton.click();
  }

  async expectFieldErrors() {
    await expect(this.fieldErrors.first()).toBeVisible();
  }

  async hasExistingPaymentError(): Promise<boolean> {
    const banner = this.page.locator('.auth-error-banner');
    return banner.isVisible({ timeout: 2_000 }).catch(() => false);
  }

  async skipIfPaymentExists() {
    if (await this.hasExistingPaymentError()) {
      test.skip(true, 'Payment already exists for this booking');
    }
  }

  async fillAndSubmit(operator: string, phone: string) {
    await this.selectOperator(operator);
    await this.fillPhone(phone);
    await this.submit();
  }
}
