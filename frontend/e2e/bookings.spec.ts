import { test } from '@playwright/test';
import { loginAsStudent, loginAsLandlord } from './helpers';
import { NavbarPage } from './pages/navbar.page';
import { MyBookingsPage } from './pages/my-bookings.page';
import { LandlordBookingsPage } from './pages/landlord-bookings.page';

test.describe('Student Bookings', () => {
  let navbar: NavbarPage;
  let bookings: MyBookingsPage;

  test.beforeEach(async ({ page }) => {
    await loginAsStudent(page);
    navbar = new NavbarPage(page);
    bookings = new MyBookingsPage(page);
    await navbar.goToMyBookings();
  });

  test('should display my bookings page with tabs', async () => {
    await bookings.expectPageLoaded();
    await bookings.expectTabsVisible();
  });

  test('should show booking cards with listing info', async () => {
    await bookings.expectBookingCardsVisible();
    await bookings.expectBookingCardHasTitle();
  });

  test('should show pay button on confirmed bookings', async () => {
    await bookings.waitForCards();
    await bookings.expectPayButtonVisible();
  });

  test('should navigate to payment form from pay button', async () => {
    await bookings.navigateToPayment();
  });
});

test.describe('Landlord Bookings', () => {
  let navbar: NavbarPage;
  let bookings: LandlordBookingsPage;

  test.beforeEach(async ({ page }) => {
    await loginAsLandlord(page);
    bookings = new LandlordBookingsPage(page);
    await bookings.goto();
  });

  test('should display landlord bookings page', async () => {
    await bookings.expectPageLoaded();
    await bookings.expectContentVisible();
  });
});
