import { test } from '@playwright/test';
import { loginAsLandlord } from './helpers';
import { SubscriptionPage } from './pages/subscription.page';

test.describe('Subscription Flow', () => {
  let subscriptionPage: SubscriptionPage;

  test.beforeEach(async ({ page }) => {
    await loginAsLandlord(page);
    subscriptionPage = new SubscriptionPage(page);
    await subscriptionPage.goto();
  });

  test('should load the subscription page', async () => {
    await subscriptionPage.expectPageLoaded();
  });

  test('should show subscribe button when no active subscription', async () => {
    await subscriptionPage.expectSubscribeButtonVisible();
  });

  test('should open MoMo payment dialog on subscribe click', async () => {
    await subscriptionPage.clickSubscribe();
    await subscriptionPage.expectMomoDialogOpen();
  });

  test('should allow entering phone number in payment dialog', async ({ page }) => {
    await subscriptionPage.clickSubscribe();
    await subscriptionPage.expectMomoDialogOpen();
    await subscriptionPage.fillPhoneAndConfirm('677123456');
    await page.waitForLoadState('networkidle');
  });
});
