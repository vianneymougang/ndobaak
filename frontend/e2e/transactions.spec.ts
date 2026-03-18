import { test } from '@playwright/test';
import { loginAsStudent } from './helpers';
import { NavbarPage } from './pages/navbar.page';
import { TransactionsPage } from './pages/transactions.page';

test.describe('Transaction History', () => {
  let navbar: NavbarPage;
  let transactions: TransactionsPage;

  test.beforeEach(async ({ page }) => {
    await loginAsStudent(page);
    navbar = new NavbarPage(page);
    transactions = new TransactionsPage(page);
  });

  test('should navigate to transactions from navbar', async () => {
    await navbar.expectTransactionsLinkVisible();
    await navbar.goToTransactions();
    await transactions.expectPageLoaded();
  });

  test('should display transactions page title', async () => {
    await navbar.goToTransactions();
    await transactions.expectTitleVisible();
  });

  test('should show empty state or transaction list', async () => {
    await navbar.goToTransactions();
    await transactions.expectContentVisible();
  });
});
