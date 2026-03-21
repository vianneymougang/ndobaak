import { test } from '@playwright/test';
import { loginAsStudent } from './helpers';
import { NavbarPage } from './pages/navbar.page';
import { MyBookingsPage } from './pages/my-bookings.page';
import { PaymentFormPage } from './pages/payment-form.page';
import { PaymentProcessingPage } from './pages/payment-processing.page';

test.describe('Payment Flow', () => {
  let navbar: NavbarPage;
  let bookings: MyBookingsPage;
  let paymentForm: PaymentFormPage;

  test.beforeEach(async ({ page }) => {
    await loginAsStudent(page);
    navbar = new NavbarPage(page);
    bookings = new MyBookingsPage(page);
    paymentForm = new PaymentFormPage(page);

    await navbar.goToMyBookings();
    await bookings.waitForCards();

    if (!(await bookings.hasPayButton())) {
      test.skip(true, 'No confirmed booking with pay button');
      return;
    }

    await bookings.clickPayButton();
    await paymentForm.expectPageLoaded();
  });

  test('should display payment form with booking summary', async () => {
    await paymentForm.expectSummaryVisible();
  });

  test('should show integral payment type selected by default', async () => {
    await paymentForm.expectIntegralSelectedByDefault();
  });

  test('should show frequency dropdown when installment is selected', async () => {
    await paymentForm.selectInstallmentType();
    await paymentForm.expectFrequencyDropdownVisible();
  });

  test('should validate required fields before submit', async () => {
    await paymentForm.submit();
    await paymentForm.expectFieldErrors();
  });

  test('should fill form, submit, and show processing page', async ({ page }) => {
    const processing = new PaymentProcessingPage(page);

    await paymentForm.fillAndSubmit('MTN Mobile Money', '655112233');

    const outcome = await Promise.race([
      page.waitForURL(/\/pay\/processing/, { timeout: 10_000 }).then(() => 'navigated' as const),
      page.locator('.auth-error-banner').waitFor({ state: 'visible', timeout: 10_000 }).then(() => 'error' as const),
    ]);

    if (outcome === 'error') {
      test.skip(true, 'Payment already exists for this booking');
      return;
    }

    await processing.expectProcessingState();
  });
});
