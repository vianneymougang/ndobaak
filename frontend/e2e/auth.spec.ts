import { test } from '@playwright/test';
import { TEST_STUDENT, TEST_LANDLORD } from './helpers';
import { LoginPage } from './pages/login.page';

test.describe('Authentication', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
  });

  test('should login as student with email', async () => {
    await loginPage.expectHeading();
    await loginPage.login(TEST_STUDENT.email, TEST_STUDENT.password);
    await loginPage.expectLoginSuccess();
  });

  test('should login as landlord with email', async () => {
    await loginPage.login(TEST_LANDLORD.email, TEST_LANDLORD.password);
    await loginPage.expectLoginSuccess();
  });

  test('should show error on invalid credentials', async () => {
    await loginPage.login('wrong@test.com', 'wrongpassword');
    await loginPage.expectErrorBanner();
  });

  test('should validate required fields', async () => {
    await loginPage.submitButton.click();
    await loginPage.expectFieldErrors();
  });
});
