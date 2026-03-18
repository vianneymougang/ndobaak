-- Fix IUSJC campus coordinates: actual position measured on-site
-- Old: 3.8480, 11.5021 (wrong — ~12 km off)
-- New: 3.8844536562668015, 11.391160302004234

-- Relocate V10 test locations near the real IUSJC campus
UPDATE location SET latitude = 3.8862, longitude = 11.3925
WHERE address = '7 Rue des Manguiers, Eyang';

UPDATE location SET latitude = 3.8910, longitude = 11.3960
WHERE address = '12 Avenue de la Paix, Nkoldongo';

UPDATE location SET latitude = 3.8750, longitude = 11.3830
WHERE address = '34 Rue du Marché Central, Obili';

UPDATE location SET latitude = 3.8920, longitude = 11.3850
WHERE address = '5 Impasse de la Source, Melen';

UPDATE location SET latitude = 3.8730, longitude = 11.3800
WHERE address = '18 Boulevard de la Liberté, Ngousso';

-- Recalculate distance_to_campus for ALL locations using Haversine formula
UPDATE location
SET distance_to_campus = ROUND(
    CAST(
        2 * 6371.0 * ASIN(SQRT(
            POWER(SIN(RADIANS((3.8844536562668015 - latitude) / 2)), 2) +
            COS(RADIANS(latitude)) * COS(RADIANS(3.8844536562668015)) *
            POWER(SIN(RADIANS((11.391160302004234 - longitude) / 2)), 2)
        ))
    AS NUMERIC), 2);
