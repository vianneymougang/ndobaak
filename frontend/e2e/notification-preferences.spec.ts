import { test } from '@playwright/test';
import { loginAsStudent, loginAsLandlord } from './helpers';
import { NotificationPreferencesPage } from './pages/notification-preferences.page';

test.describe('Notification Preferences — Student', () => {
  let prefsPage: NotificationPreferencesPage;

  test.beforeEach(async ({ page }) => {
    await loginAsStudent(page);
    prefsPage = new NotificationPreferencesPage(page);
    await prefsPage.goto();
  });

  test('should load the notification preferences page', async () => {
    await prefsPage.expectPageLoaded();
  });

  test('should display toggle switches for preferences', async () => {
    await prefsPage.waitForPreferencesLoaded();
    await prefsPage.expectTogglesVisible();
  });

  test('should auto-save when a toggle is changed', async ({ page }) => {
    await prefsPage.waitForPreferencesLoaded();
    const [, response] = await Promise.all([
      prefsPage.toggleFirstEmailPreference(),
      page.waitForResponse(
        (r) => r.url().includes('/notifications/preferences') && r.request().method() === 'PUT',
        { timeout: 5_000 },
      ),
    ]);
    if (response) {
      await response;
    }
  });
});

test.describe('Notification Preferences — Landlord', () => {
  let prefsPage: NotificationPreferencesPage;

  test.beforeEach(async ({ page }) => {
    await loginAsLandlord(page);
    prefsPage = new NotificationPreferencesPage(page);
    await prefsPage.goto();
  });

  test('should load preferences page for landlord', async () => {
    await prefsPage.expectPageLoaded();
  });

  test('should show toggles with email and SMS columns', async () => {
    await prefsPage.waitForPreferencesLoaded();
    await prefsPage.expectTogglesVisible();
  });
});
