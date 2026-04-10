import { chromium, FullConfig } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

const API_URL = 'http://localhost:8080/api/v1';
const AUTH_DIR = path.join(__dirname, '.auth');

async function loginAndSave(email: string, password: string, role: string) {
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  await page.goto('http://localhost:4200');
  const res = await page.request.post(`${API_URL}/auth/login`, {
    data: { email, password },
    headers: { 'Content-Type': 'application/json' },
  });

  if (!res.ok()) {
    console.warn(`Login failed for ${email}: ${res.status()}`);
  }

  await context.storageState({ path: path.join(AUTH_DIR, `${role}.json`) });
  await browser.close();
}

async function createPendingListing() {
  const { execSync } = await import('child_process');
  try {
    execSync(`docker exec ndobaak-db psql -U ndobaak -d ndobaak_dev -c "
      -- Delete duplicate test listings, keep only id=6
      DELETE FROM photo WHERE listing_id IN (SELECT id FROM listing WHERE title = 'Annonce test e2e en attente' AND id != 6);
      DELETE FROM listing WHERE title = 'Annonce test e2e en attente' AND id != 6;
      
      -- Ensure listing id=6 exists and is PENDING_REVIEW with photos
      DO \\\\\\$\\\\\\$
      DECLARE landlord_id bigint;
      DECLARE loc_id bigint;
      BEGIN
        SELECT id INTO landlord_id FROM users WHERE email = 'mbarga@test.ndobaak.com';
        
        IF NOT EXISTS (SELECT 1 FROM listing WHERE id = 6) THEN
          INSERT INTO location (address, latitude, longitude, neighborhood, city)
          VALUES ('Quartier Eyang, Yaoundé', 3.8667, 11.5167, 'Eyang', 'Yaoundé')
          RETURNING id INTO loc_id;
          INSERT INTO listing (id, title, description, room_type, monthly_price, status, created_at, updated_at, landlord_id, available_rooms, location_id)
          VALUES (6, 'Annonce test e2e en attente', 'Annonce pour tests automatisés', 'SINGLE_ROOM', 35000, 'PENDING_REVIEW', NOW(), NOW(), landlord_id, 1, loc_id);
        ELSE
          UPDATE listing SET status = 'PENDING_REVIEW' WHERE id = 6;
        END IF;
        
        IF NOT EXISTS (SELECT 1 FROM photo p WHERE p.listing_id = 6) THEN
          INSERT INTO photo (url, listing_id, display_order) VALUES
            ('https://example.com/photo1.jpg', 6, 1),
            ('https://example.com/photo2.jpg', 6, 2),
            ('https://example.com/photo3.jpg', 6, 3);
        END IF;
      END \\\\\\$\\\\\\$;
    "`, { stdio: 'pipe' });
    console.log('Pending listing ensured for e2e tests');
  } catch (e) {
    console.warn('Could not setup pending listing:', e);
  }
}

export default async function globalSetup(_config: FullConfig) {
  fs.mkdirSync(AUTH_DIR, { recursive: true });

  await loginAndSave('essono@test.ndobaak.com', 'ndobaak123', 'student');
  await loginAndSave('mbarga@test.ndobaak.com', 'ndobaak123', 'landlord');
  await loginAndSave('admin@ndobaak.com', 'Admin@ndobaak2026', 'admin');

  await createPendingListing();
}
