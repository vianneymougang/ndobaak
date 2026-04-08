import { test } from '@playwright/test';
import { execSync } from 'child_process';
import { loginAsAdmin } from './helpers';
import { AdminListingsPage } from './pages/admin-listings.page';
import { AdminUsersPage } from './pages/admin-users.page';
import { AdminReviewsPage } from './pages/admin-reviews.page';

function resetPendingListing() {
  try {
    execSync(`docker exec ndobaak-db psql -U ndobaak -d ndobaak_dev -c "UPDATE listing SET status = 'PENDING_REVIEW' WHERE id = 6;"`, { stdio: 'pipe' });
  } catch {}
}

test.describe('Admin — Listing Validation', () => {
  let adminListings: AdminListingsPage;

  test.beforeEach(async ({ page }) => {
    resetPendingListing();
    await loginAsAdmin(page);
    adminListings = new AdminListingsPage(page);
    await adminListings.goto();
  });

  test('should load the admin listings page', async () => {
    await adminListings.expectPageLoaded();
    await adminListings.expectTableVisible();
  });

  test('should show pending listings tab', async () => {
    await adminListings.clickPendingTab();
    await adminListings.expectTableVisible();
  });

  test('should approve a pending listing', async () => {
    await adminListings.clickPendingTab();
    await adminListings.approveFirstListing();
    await adminListings.expectSuccessToast();
  });
});

test.describe('Admin — Listing Rejection', () => {
  let adminListings: AdminListingsPage;

  test.beforeEach(async ({ page }) => {
    resetPendingListing();
    await loginAsAdmin(page);
    adminListings = new AdminListingsPage(page);
    await adminListings.goto();
  });

  test('should open rejection dialog with motif selector', async () => {
    await adminListings.clickPendingTab();
    await adminListings.openRejectDialogForFirstListing();
  });

  test('should reject a listing with a selected motif', async () => {
    await adminListings.clickPendingTab();
    await adminListings.openRejectDialogForFirstListing();
    await adminListings.confirmRejection();
    await adminListings.expectSuccessToast();
  });
});

test.describe('Admin — User Management', () => {
  let adminUsers: AdminUsersPage;

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    adminUsers = new AdminUsersPage(page);
    await adminUsers.goto();
  });

  test('should load the admin users page with table', async () => {
    await adminUsers.expectPageLoaded();
    await adminUsers.expectTableVisible();
  });

  test('should filter users by search query', async () => {
    await adminUsers.expectPageLoaded();
    await adminUsers.searchUser('essono');
    await adminUsers.expectRowsVisible();
  });
});

test.describe('Admin — Review Moderation', () => {
  let adminReviews: AdminReviewsPage;

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    adminReviews = new AdminReviewsPage(page);
    await adminReviews.goto();
  });

  test('should load the admin reviews page', async () => {
    await adminReviews.expectPageLoaded();
  });

  test('should show pending, flagged and all tabs', async () => {
    await adminReviews.expectTabsVisible();
  });

  test('should navigate to flagged reviews tab', async () => {
    await adminReviews.clickFlaggedTab();
    await adminReviews.expectPageLoaded();
  });
});
