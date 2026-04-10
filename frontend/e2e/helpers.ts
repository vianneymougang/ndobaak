import { Page } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

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
  email: 'admin@ndobaak.com',
  password: 'Admin@ndobaak2026',
  firstName: 'Admin',
};

const AUTH_DIR = path.join(__dirname, '.auth');

async function loadStorageState(page: Page, role: 'student' | 'landlord' | 'admin') {
  const stateFile = path.join(AUTH_DIR, `${role}.json`);
  if (!fs.existsSync(stateFile)) {
    throw new Error(`Storage state not found: ${stateFile}. Run globalSetup first.`);
  }
  const state = JSON.parse(fs.readFileSync(stateFile, 'utf-8'));
  if (state.cookies?.length) {
    await page.context().addCookies(state.cookies);
  }
  await page.goto('/');
  await page.waitForLoadState('networkidle');
}

export async function loginAsStudent(page: Page) {
  await loadStorageState(page, 'student');
}

export async function loginAsLandlord(page: Page) {
  await loadStorageState(page, 'landlord');
}

export async function loginAsAdmin(page: Page) {
  await loadStorageState(page, 'admin');
}
