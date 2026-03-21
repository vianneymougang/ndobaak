-- =============================================
-- Ndobaâk — Sprint 1 Test Data Seed
-- =============================================
-- Purpose: populate dev DB with realistic data
--          covering the full Sprint 1 user journey:
--          register → create listing → admin review → publish → search
--
-- Usage:
--   psql -U postgres -d ndobaak -f seed_sprint1.sql
--   OR via pgAdmin: run as a script
--
-- Password for all test accounts: Ndobaak2024
--
-- Existing admin account:
--   admin@ndobaak.com / (see V4 migration)
--
-- CAUTION: deletes all existing non-admin users and listings.
-- Safe to run multiple times (idempotent via DELETE + re-insert).
-- =============================================

BEGIN;

-- -----------------------------------------------
-- 0. Clean existing seed data (non-admin)
-- -----------------------------------------------
DELETE FROM listing_amenity WHERE listing_id IN (SELECT id FROM listing);
DELETE FROM photo WHERE listing_id IN (SELECT id FROM listing);
DELETE FROM listing WHERE landlord_id IN (SELECT id FROM users WHERE user_type IN ('LANDLORD', 'STUDENT'));
DELETE FROM location WHERE id NOT IN (SELECT location_id FROM listing WHERE location_id IS NOT NULL);
DELETE FROM users WHERE user_type IN ('LANDLORD', 'STUDENT');

-- -----------------------------------------------
-- 1. Landlords
--    password: Ndobaak2024
--    hash:     $2b$10$jOnEmuOxuCk9zlsc.9kafeXGjnSGKDVRSVbrFp0bnV8OR.Lu3v4bC
-- -----------------------------------------------
INSERT INTO users (user_type, first_name, last_name, email, phone, password, registration_date, status, id_card_number, tax_id)
VALUES
    ('LANDLORD', 'Jean-Pierre', 'Mballa',  'jp.mballa@test.com',   '+237690100101',
     '$2b$10$jOnEmuOxuCk9zlsc.9kafeXGjnSGKDVRSVbrFp0bnV8OR.Lu3v4bC',
     NOW() - INTERVAL '30 days', 'ACTIVE', 'CNI237001001', 'Résidence Obili'),

    ('LANDLORD', 'Marie',       'Ngo Bami', 'marie.ngobami@test.com', '+237690100102',
     '$2b$10$jOnEmuOxuCk9zlsc.9kafeXGjnSGKDVRSVbrFp0bnV8OR.Lu3v4bC',
     NOW() - INTERVAL '25 days', 'ACTIVE', 'CNI237001002', 'Foyer Melen'),

    ('LANDLORD', 'Paul',        'Essono',  'paul.essono@test.com',  '+237690100103',
     '$2b$10$jOnEmuOxuCk9zlsc.9kafeXGjnSGKDVRSVbrFp0bnV8OR.Lu3v4bC',
     NOW() - INTERVAL '20 days', 'ACTIVE', 'CNI237001003', 'Villa Tsinga');

-- -----------------------------------------------
-- 2. Students
-- -----------------------------------------------
INSERT INTO users (user_type, first_name, last_name, email, phone, password, registration_date, status, matricule, field_of_study, year_of_study)
VALUES
    ('STUDENT', 'Alice',     'Beng',   'alice.beng@test.com',   '+237690200201',
     '$2b$10$jOnEmuOxuCk9zlsc.9kafeXGjnSGKDVRSVbrFp0bnV8OR.Lu3v4bC',
     NOW() - INTERVAL '15 days', 'ACTIVE', 'IUSJC2024001', 'Informatique', 3),

    ('STUDENT', 'David',     'Kamga',  'david.kamga@test.com',  '+237690200202',
     '$2b$10$jOnEmuOxuCk9zlsc.9kafeXGjnSGKDVRSVbrFp0bnV8OR.Lu3v4bC',
     NOW() - INTERVAL '12 days', 'ACTIVE', 'IUSJC2024002', 'Économie', 2),

    ('STUDENT', 'Fatima',    'Aliou',  'fatima.aliou@test.com', '+237690200203',
     '$2b$10$jOnEmuOxuCk9zlsc.9kafeXGjnSGKDVRSVbrFp0bnV8OR.Lu3v4bC',
     NOW() - INTERVAL '8 days',  'ACTIVE', 'IUSJC2024003', 'Droit', 1);

-- -----------------------------------------------
-- 3. Locations (near IUSJC Yaoundé — 3.8845°N, 11.3912°E)
-- -----------------------------------------------
INSERT INTO location (address, latitude, longitude, distance_to_campus, neighborhood, city)
VALUES
    -- L1 : Obili, very close
    ('Rue Obili, face école primaire, Yaoundé', 3.8870, 11.3880, 0.44, 'Obili', 'Yaoundé'),
    -- L2 : Obili, slightly further
    ('Carrefour Obili, 2ème rue à gauche, Yaoundé', 3.8825, 11.3870, 0.52, 'Obili', 'Yaoundé'),
    -- L3 : Melen
    ('Avenue Melen, derrière le marché, Yaoundé', 3.8780, 11.3960, 0.89, 'Melen', 'Yaoundé'),
    -- L4 : Melen, draft listing
    ('Quartier Melen, rue des étudiants, Yaoundé', 3.8760, 11.3980, 1.18, 'Melen', 'Yaoundé'),
    -- L5 : Tsinga
    ('Résidence Tsinga, bloc C, Yaoundé', 3.8950, 11.3810, 1.54, 'Tsinga', 'Yaoundé'),
    -- L6 : Tsinga, rejected listing
    ('Tsinga, derrière la station Total, Yaoundé', 3.8970, 11.3790, 1.91, 'Tsinga', 'Yaoundé');

-- -----------------------------------------------
-- 4. Listings
-- -----------------------------------------------

-- 4a. Published listings (visible in search)
INSERT INTO listing (title, description, room_type, monthly_price, available_rooms, available_date, status, created_at, updated_at, landlord_id, location_id)
VALUES
    (
        'Studio meublé Obili — proche IUSJC',
        'Beau studio entièrement meublé à 450 m de l''IUSJC. Cuisine équipée, salle de bain privative, eau et électricité incluses. Cadre calme et sécurisé, idéal pour étudiant sérieux.',
        'STUDIO',
        35000,
        2,
        CURRENT_DATE + INTERVAL '7 days',
        'PUBLISHED',
        NOW() - INTERVAL '28 days',
        NOW() - INTERVAL '26 days',
        (SELECT id FROM users WHERE email = 'jp.mballa@test.com'),
        (SELECT id FROM location WHERE neighborhood = 'Obili' AND distance_to_campus = 0.45)
    ),
    (
        'Chambre simple Melen — 25 000 FCFA',
        'Chambre simple lumineuse dans une résidence étudiante bien tenue. Cuisine et toilettes partagées avec 2 autres étudiants seulement. Accès direct au marché Melen. Proche transports.',
        'SINGLE_ROOM',
        25000,
        1,
        CURRENT_DATE + INTERVAL '3 days',
        'PUBLISHED',
        NOW() - INTERVAL '22 days',
        NOW() - INTERVAL '20 days',
        (SELECT id FROM users WHERE email = 'marie.ngobami@test.com'),
        (SELECT id FROM location WHERE neighborhood = 'Melen' AND distance_to_campus = 0.92)
    ),
    (
        'Appartement 2 pièces Tsinga — tout confort',
        'Appartement moderne de 2 pièces dans une résidence sécurisée à Tsinga. Climatisation, parking privé, gardien 24h/24. Idéal pour un couple d''étudiants ou un étudiant souhaitant plus d''espace.',
        'APARTMENT',
        80000,
        1,
        CURRENT_DATE,
        'PUBLISHED',
        NOW() - INTERVAL '18 days',
        NOW() - INTERVAL '16 days',
        (SELECT id FROM users WHERE email = 'paul.essono@test.com'),
        (SELECT id FROM location WHERE neighborhood = 'Tsinga' AND distance_to_campus = 1.35)
    ),

    -- 4b. Pending review (waiting for admin action)
    (
        'Chambre double Obili — 2 places',
        'Grande chambre pouvant accueillir deux étudiants (lits séparés). Quartier Obili, à 650 m de l''IUSJC. Eau courante, électricité, WiFi partagé dans la maison.',
        'DOUBLE_ROOM',
        45000,
        1,
        CURRENT_DATE + INTERVAL '14 days',
        'PENDING_REVIEW',
        NOW() - INTERVAL '5 days',
        NOW() - INTERVAL '4 days',
        (SELECT id FROM users WHERE email = 'jp.mballa@test.com'),
        (SELECT id FROM location WHERE neighborhood = 'Obili' AND distance_to_campus = 0.65)
    ),

    -- 4c. Draft (not submitted yet, only visible to landlord)
    (
        'Studio Melen — brouillon',
        'Studio en cours de description. Photos à ajouter.',
        'STUDIO',
        32000,
        1,
        NULL,
        'DRAFT',
        NOW() - INTERVAL '3 days',
        NOW() - INTERVAL '3 days',
        (SELECT id FROM users WHERE email = 'marie.ngobami@test.com'),
        (SELECT id FROM location WHERE neighborhood = 'Melen' AND distance_to_campus = 1.05)
    ),

    -- 4d. Rejected (with rejection reason)
    (
        'Chambre Tsinga pas chère',
        'chambre libre',
        'SINGLE_ROOM',
        18000,
        1,
        CURRENT_DATE,
        'REJECTED',
        NOW() - INTERVAL '10 days',
        NOW() - INTERVAL '9 days',
        (SELECT id FROM users WHERE email = 'paul.essono@test.com'),
        (SELECT id FROM location WHERE neighborhood = 'Tsinga' AND distance_to_campus = 1.55)
    );

UPDATE listing
SET rejection_reason = 'Description trop courte et photos manquantes. Veuillez fournir au minimum 3 photos et une description détaillée de 100 caractères minimum.'
WHERE title = 'Chambre Tsinga pas chère';

-- -----------------------------------------------
-- 5. Photos (picsum.photos as placeholder)
--    Thumbnails: 200×150, Medium: 400×300, Original: 800×600
-- -----------------------------------------------

-- Studio Obili (listing 1) — 3 photos
INSERT INTO photo (url, thumbnail_url, medium_url, description, file_size, display_order, created_at, listing_id)
SELECT
    'https://picsum.photos/seed/ndob11/800/600',
    'https://picsum.photos/seed/ndob11/200/150',
    'https://picsum.photos/seed/ndob11/400/300',
    'Vue principale de la chambre',
    245760,
    0,
    NOW() - INTERVAL '26 days',
    l.id
FROM listing l WHERE l.title = 'Studio meublé Obili — proche IUSJC';

INSERT INTO photo (url, thumbnail_url, medium_url, description, file_size, display_order, created_at, listing_id)
SELECT
    'https://picsum.photos/seed/ndob12/800/600',
    'https://picsum.photos/seed/ndob12/200/150',
    'https://picsum.photos/seed/ndob12/400/300',
    'Cuisine équipée',
    198432,
    1,
    NOW() - INTERVAL '26 days',
    l.id
FROM listing l WHERE l.title = 'Studio meublé Obili — proche IUSJC';

INSERT INTO photo (url, thumbnail_url, medium_url, description, file_size, display_order, created_at, listing_id)
SELECT
    'https://picsum.photos/seed/ndob13/800/600',
    'https://picsum.photos/seed/ndob13/200/150',
    'https://picsum.photos/seed/ndob13/400/300',
    'Salle de bain',
    178560,
    2,
    NOW() - INTERVAL '26 days',
    l.id
FROM listing l WHERE l.title = 'Studio meublé Obili — proche IUSJC';

-- Chambre Melen (listing 2) — 2 photos
INSERT INTO photo (url, thumbnail_url, medium_url, description, file_size, display_order, created_at, listing_id)
SELECT
    'https://picsum.photos/seed/ndob21/800/600',
    'https://picsum.photos/seed/ndob21/200/150',
    'https://picsum.photos/seed/ndob21/400/300',
    'La chambre',
    212000,
    0,
    NOW() - INTERVAL '20 days',
    l.id
FROM listing l WHERE l.title = 'Chambre simple Melen — 25 000 FCFA';

INSERT INTO photo (url, thumbnail_url, medium_url, description, file_size, display_order, created_at, listing_id)
SELECT
    'https://picsum.photos/seed/ndob22/800/600',
    'https://picsum.photos/seed/ndob22/200/150',
    'https://picsum.photos/seed/ndob22/400/300',
    'Espace commun',
    189000,
    1,
    NOW() - INTERVAL '20 days',
    l.id
FROM listing l WHERE l.title = 'Chambre simple Melen — 25 000 FCFA';

-- Appartement Tsinga (listing 3) — 4 photos
INSERT INTO photo (url, thumbnail_url, medium_url, description, file_size, display_order, created_at, listing_id)
SELECT
    'https://picsum.photos/seed/ndob31/800/600',
    'https://picsum.photos/seed/ndob31/200/150',
    'https://picsum.photos/seed/ndob31/400/300',
    'Salon',
    290000,
    0,
    NOW() - INTERVAL '16 days',
    l.id
FROM listing l WHERE l.title = 'Appartement 2 pièces Tsinga — tout confort';

INSERT INTO photo (url, thumbnail_url, medium_url, description, file_size, display_order, created_at, listing_id)
SELECT
    'https://picsum.photos/seed/ndob32/800/600',
    'https://picsum.photos/seed/ndob32/200/150',
    'https://picsum.photos/seed/ndob32/400/300',
    'Chambre principale',
    275000,
    1,
    NOW() - INTERVAL '16 days',
    l.id
FROM listing l WHERE l.title = 'Appartement 2 pièces Tsinga — tout confort';

INSERT INTO photo (url, thumbnail_url, medium_url, description, file_size, display_order, created_at, listing_id)
SELECT
    'https://picsum.photos/seed/ndob33/800/600',
    'https://picsum.photos/seed/ndob33/200/150',
    'https://picsum.photos/seed/ndob33/400/300',
    'Cuisine',
    255000,
    2,
    NOW() - INTERVAL '16 days',
    l.id
FROM listing l WHERE l.title = 'Appartement 2 pièces Tsinga — tout confort';

INSERT INTO photo (url, thumbnail_url, medium_url, description, file_size, display_order, created_at, listing_id)
SELECT
    'https://picsum.photos/seed/ndob34/800/600',
    'https://picsum.photos/seed/ndob34/200/150',
    'https://picsum.photos/seed/ndob34/400/300',
    'Parking',
    230000,
    3,
    NOW() - INTERVAL '16 days',
    l.id
FROM listing l WHERE l.title = 'Appartement 2 pièces Tsinga — tout confort';

-- Chambre double Obili en attente — 2 photos
INSERT INTO photo (url, thumbnail_url, medium_url, description, file_size, display_order, created_at, listing_id)
SELECT
    'https://picsum.photos/seed/ndob41/800/600',
    'https://picsum.photos/seed/ndob41/200/150',
    'https://picsum.photos/seed/ndob41/400/300',
    'Les deux lits',
    220000,
    0,
    NOW() - INTERVAL '4 days',
    l.id
FROM listing l WHERE l.title = 'Chambre double Obili — 2 places';

INSERT INTO photo (url, thumbnail_url, medium_url, description, file_size, display_order, created_at, listing_id)
SELECT
    'https://picsum.photos/seed/ndob42/800/600',
    'https://picsum.photos/seed/ndob42/200/150',
    'https://picsum.photos/seed/ndob42/400/300',
    'Vue depuis la fenêtre',
    195000,
    1,
    NOW() - INTERVAL '4 days',
    l.id
FROM listing l WHERE l.title = 'Chambre double Obili — 2 places';

-- -----------------------------------------------
-- 6. Amenities associations
-- -----------------------------------------------

-- Studio Obili : WiFi, Running Water, Electricity, Private Shower, Private Toilet, Furnished
INSERT INTO listing_amenity (listing_id, amenity_id)
SELECT l.id, a.id
FROM listing l, amenity a
WHERE l.title = 'Studio meublé Obili — proche IUSJC'
  AND a.name IN ('WiFi', 'Running Water', 'Electricity', 'Private Shower', 'Private Toilet', 'Furnished');

-- Chambre Melen : Running Water, Electricity, Shared Kitchen, Fan
INSERT INTO listing_amenity (listing_id, amenity_id)
SELECT l.id, a.id
FROM listing l, amenity a
WHERE l.title = 'Chambre simple Melen — 25 000 FCFA'
  AND a.name IN ('Running Water', 'Electricity', 'Shared Kitchen', 'Fan');

-- Appartement Tsinga : WiFi, Running Water, Electricity, Private Shower, Private Toilet, Air Conditioning, Parking, Security Guard, Furnished
INSERT INTO listing_amenity (listing_id, amenity_id)
SELECT l.id, a.id
FROM listing l, amenity a
WHERE l.title = 'Appartement 2 pièces Tsinga — tout confort'
  AND a.name IN ('WiFi', 'Running Water', 'Electricity', 'Private Shower', 'Private Toilet',
                 'Air Conditioning', 'Parking', 'Security Guard', 'Furnished');

-- Chambre double Obili (pending) : Running Water, Electricity, WiFi
INSERT INTO listing_amenity (listing_id, amenity_id)
SELECT l.id, a.id
FROM listing l, amenity a
WHERE l.title = 'Chambre double Obili — 2 places'
  AND a.name IN ('Running Water', 'Electricity', 'WiFi');

COMMIT;

-- -----------------------------------------------
-- Summary
-- -----------------------------------------------
SELECT '=== SEED SUMMARY ===' AS info;

SELECT user_type, email, first_name || ' ' || last_name AS name
FROM users
WHERE user_type IN ('LANDLORD', 'STUDENT')
ORDER BY user_type, email;

SELECT l.status, l.title, l.monthly_price, u.email AS landlord
FROM listing l
JOIN users u ON u.id = l.landlord_id
ORDER BY l.status, l.created_at;

SELECT '=== password for all test accounts: Ndobaak2024 ===' AS reminder;
