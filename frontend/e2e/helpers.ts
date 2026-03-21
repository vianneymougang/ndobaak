import { Page, expect, request as apiRequest } from '@playwright/test';

export const TEST_STUDENT = {
  email: 'essono@test.ndobaak.com',
  password: 'ndobaak123',
  firstName: 'Paul',
};

export const TEST_LANDLORD = {
  email: 'mbarga@test.ndobaak.com',
  password: 'ndobaak123',
  firstName: 'Jean-Baptiste',
};

export const TEST_ADMIN = {
  email: 'admin@test.ndobaak.com',
  password: 'ndobaak123',
  firstName: 'Admin',
};

const API_URL = 'http://localhost:8080/api/v1';

async function getAuthToken(email: string, password: string): Promise<{ accessToken: string; refreshToken: string }> {
  const context = await apiRequest.newContext();
  const response = await context.post(`${API_URL}/auth/login`, {
    data: { email, password },
  });
  if (!response.ok()) {
    throw new Error(`Login failed: ${response.status()} ${await response.text()}`);
  }
  const body = await response.json();
  await context.dispose();
  return { accessToken: body.accessToken, refreshToken: body.refreshToken };
}

export async function loginAsStudent(page: Page) {
  const tokens = await getAuthToken(TEST_STUDENT.email, TEST_STUDENT.password);
  await injectAuth(page, tokens);
}

export async function loginAsLandlord(page: Page) {
  const tokens = await getAuthToken(TEST_LANDLORD.email, TEST_LANDLORD.password);
  await injectAuth(page, tokens);
}

export async function loginAsAdmin(page: Page) {
  const tokens = await getAuthToken(TEST_ADMIN.email, TEST_ADMIN.password);
  await injectAuth(page, tokens);
}

async function injectAuth(page: Page, tokens: { accessToken: string; refreshToken: string }) {
  await page.goto('/');
  await page.evaluate(({ accessToken, refreshToken }) => {
    localStorage.setItem('access_token', accessToken);
    localStorage.setItem('refresh_token', refreshToken);
  }, tokens);
  await page.reload();
  await page.waitForLoadState('networkidle');
}
