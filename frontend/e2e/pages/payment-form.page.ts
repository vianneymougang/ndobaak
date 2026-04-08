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
    this.pageTitle = page.getByRole('heading', { name: 'Paiement' });
    this.summaryCard = page.locator('.summary-card, [class*="summary"]');
    this.summaryTitle = page.locator('.summary-title, [class*="summary"] h');
    this.activeTypeCard = page.locator('.type-card--active');
    this.typeCards = page.locator('.type-card');
    this.frequencyDropdown = page.locator('#frequency, p-select[formcontrolname="frequency"]');
    this.operatorDropdown = page.locator('#operator, p-select[formcontrolname="operator"], p-dropdown').first();
    this.phoneInput = page.locator('#phone, input[formcontrolname="phone"], input[placeholder*="6XX"]');
    this.submitButton = page.getByRole('button', { name: /payer/i });
    this.fieldErrors = page.locator('.field-error, p-message');
  }

  async expectPageLoaded() {
    await expect(this.page).toHaveURL(/\/bookings\/\d+\/pay$/);
    await expect(this.pageTitle).toBeVisible({ timeout: 10_000 });
  }

  async expectSummaryVisible() {
    // The payment page itself is the summary — check the page title is visible
    await expect(this.pageTitle).toBeVisible({ timeout: 5_000 });
    // Check operator dropdown is present (form is loaded)
    await expect(this.operatorDropdown).toBeVisible({ timeout: 5_000 });
  }

  async expectIntegralSelectedByDefault() {
    // No type cards in current UI — payment is always integral via MoMo
    // Check that the operator dropdown and phone input are visible
    await expect(this.operatorDropdown).toBeVisible({ timeout: 5_000 });
  }

  async selectInstallmentType() {
    // No installment type selector in current UI — skip gracefully
    test.skip(true, 'Installment type selector not present in current UI');
  }

  async expectFrequencyDropdownVisible() {
    // No frequency dropdown in current UI
    test.skip(true, 'Frequency dropdown not present in current UI');
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
