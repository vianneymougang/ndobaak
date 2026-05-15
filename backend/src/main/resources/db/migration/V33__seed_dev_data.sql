DO $$
DECLARE
  landlord1_id  BIGINT;
  landlord2_id  BIGINT;
  student1_id   BIGINT;
  student2_id   BIGINT;
  loc1_id       BIGINT;
  loc2_id       BIGINT;
  loc3_id       BIGINT;
  loc4_id       BIGINT;
  loc5_id       BIGINT;
  listing1_id   BIGINT;
  listing2_id   BIGINT;
  listing3_id   BIGINT;
  listing4_id   BIGINT;
  listing5_id   BIGINT;
BEGIN

  INSERT INTO users (user_type, first_name, last_name, email, phone, password, registration_date, status)
  VALUES ('LANDLORD', 'Jean-Baptiste', 'Mbarga', 'mbarga@test.ndobaak.com', '237691234567',
          '$2b$10$LmL6q0PitaXgjJzycEAsduObLgwfjlKAZzVLaIE8ZUsaxLaL8McsW', NOW(), 'ACTIVE')
  RETURNING id INTO landlord1_id;

  INSERT INTO users (user_type, first_name, last_name, email, phone, password, registration_date, status)
  VALUES ('LANDLORD', 'Marie-Claire', 'Ondoua', 'ondoua@test.ndobaak.com', '237677654321',
          '$2b$10$LmL6q0PitaXgjJzycEAsduObLgwfjlKAZzVLaIE8ZUsaxLaL8McsW', NOW(), 'ACTIVE')
  RETURNING id INTO landlord2_id;

  INSERT INTO users (user_type, first_name, last_name, email, phone, password, registration_date, status,
                     field_of_study, year_of_study)
  VALUES ('STUDENT', 'Paul', 'Essono', 'essono@test.ndobaak.com', '237655112233',
          '$2b$10$LmL6q0PitaXgjJzycEAsduObLgwfjlKAZzVLaIE8ZUsaxLaL8McsW', NOW(), 'ACTIVE',
          'Informatique', 2)
  RETURNING id INTO student1_id;

  INSERT INTO users (user_type, first_name, last_name, email, phone, password, registration_date, status,
                     field_of_study, year_of_study)
  VALUES ('STUDENT', 'Aline', 'Nguema', 'nguema@test.ndobaak.com', '237699887766',
          '$2b$10$LmL6q0PitaXgjJzycEAsduObLgwfjlKAZzVLaIE8ZUsaxLaL8McsW', NOW(), 'ACTIVE',
          'Droit', 1)
  RETURNING id INTO student2_id;

  INSERT INTO location (address, latitude, longitude, distance_to_campus, neighborhood, city)
  VALUES ('7 Rue des Manguiers, Eyang', 3.8718, 11.5148, 0.2, 'Eyang', 'Yaoundé')
  RETURNING id INTO loc1_id;

  INSERT INTO location (address, latitude, longitude, distance_to_campus, neighborhood, city)
  VALUES ('12 Avenue de la Paix, Nkoldongo', 3.8652, 11.5201, 0.8, 'Nkoldongo', 'Yaoundé')
  RETURNING id INTO loc2_id;

  INSERT INTO location (address, latitude, longitude, distance_to_campus, neighborhood, city)
  VALUES ('34 Rue du Marché Central, Obili', 3.8589, 11.5118, 1.5, 'Obili', 'Yaoundé')
  RETURNING id INTO loc3_id;

  INSERT INTO location (address, latitude, longitude, distance_to_campus, neighborhood, city)
  VALUES ('5 Impasse de la Source, Melen', 3.8740, 11.5234, 1.0, 'Melen', 'Yaoundé')
  RETURNING id INTO loc4_id;

  INSERT INTO location (address, latitude, longitude, distance_to_campus, neighborhood, city)
  VALUES ('18 Boulevard de la Liberté, Ngousso', 3.8614, 11.5267, 1.8, 'Ngousso', 'Yaoundé')
  RETURNING id INTO loc5_id;

  INSERT INTO listing (title, description, room_type, monthly_price, available_rooms,
                       available_date, status, landlord_id, location_id, created_at)
  VALUES (
    'Studio meublé lumineux — Quartier Eyang',
    'Beau studio entièrement meublé au cœur du quartier Eyang, à 200 m de l''IUSJC. '
    'Lumineux et sécurisé, idéalement situé pour les étudiants. Douche et WC privatifs, '
    'cuisine équipée. WiFi haut débit inclus dans le loyer. Disponible dès juin 2026.',
    'STUDIO', 65000, 1, '2026-06-01', 'PUBLISHED', landlord1_id, loc1_id, NOW()
  ) RETURNING id INTO listing1_id;

  INSERT INTO listing (title, description, room_type, monthly_price, available_rooms,
                       available_date, status, landlord_id, location_id, created_at)
  VALUES (
    'Chambre individuelle calme — Nkoldongo',
    'Chambre individuelle dans une concession sécurisée et propre à Nkoldongo. '
    'Environnement paisible, idéal pour se concentrer sur les études. Cuisine et sanitaires '
    'partagés en bon état. Gardien présent 24h/24. Bus IUSJC à 5 min à pied.',
    'SINGLE_ROOM', 40000, 1, '2026-06-15', 'PUBLISHED', landlord1_id, loc2_id, NOW()
  ) RETURNING id INTO listing2_id;

  INSERT INTO listing (title, description, room_type, monthly_price, available_rooms,
                       available_date, status, landlord_id, location_id, created_at)
  VALUES (
    'Appartement F3 spacieux et rénové — Obili',
    'Grand appartement F3 entièrement rénové, idéal pour 2 ou 3 étudiants souhaitant '
    'partager les charges. Deux chambres spacieuses, salon, cuisine équipée, douche et WC '
    'privatifs. Climatisation dans le salon. Parking sécurisé dans la résidence.',
    'APARTMENT', 120000, 2, '2026-07-01', 'PUBLISHED', landlord2_id, loc3_id, NOW()
  ) RETURNING id INTO listing3_id;

  INSERT INTO listing (title, description, room_type, monthly_price, available_rooms,
                       available_date, status, landlord_id, location_id, created_at)
  VALUES (
    'Chambre double sécurisée — Melen',
    'Chambre double dans une résidence surveillée au quartier Melen. Convient à deux '
    'colocataires. Literie fournie, grande cour commune, cuisine partagée bien équipée. '
    'Cadre verdoyant et tranquille, à 1 km de l''IUSJC.',
    'DOUBLE_ROOM', 50000, 2, '2026-06-01', 'PUBLISHED', landlord2_id, loc4_id, NOW()
  ) RETURNING id INTO listing4_id;

  INSERT INTO listing (title, description, room_type, monthly_price, available_rooms,
                       available_date, status, landlord_id, location_id, created_at)
  VALUES (
    'Studio moderne avec WiFi — Ngousso',
    'Studio moderne et bien entretenu dans un quartier résidentiel calme à Ngousso. '
    'Connexion internet haut débit, eau courante 24h/24, électricité stable sur groupe '
    'électrogène en cas de coupure. Idéal pour un étudiant sérieux recherchant confort '
    'et tranquillité.',
    'STUDIO', 58000, 1, '2026-07-15', 'PUBLISHED', landlord1_id, loc5_id, NOW()
  ) RETURNING id INTO listing5_id;

  INSERT INTO listing_amenity (listing_id, amenity_id)
  SELECT listing1_id, id FROM amenity
  WHERE name IN ('WiFi', 'Electricity', 'Running Water', 'Private Shower',
                 'Private Toilet', 'Furnished', 'Fan');

  INSERT INTO listing_amenity (listing_id, amenity_id)
  SELECT listing2_id, id FROM amenity
  WHERE name IN ('Electricity', 'Running Water', 'Shared Kitchen', 'Fan', 'Security Guard');

  INSERT INTO listing_amenity (listing_id, amenity_id)
  SELECT listing3_id, id FROM amenity
  WHERE name IN ('WiFi', 'Electricity', 'Running Water', 'Private Shower', 'Private Toilet',
                 'Shared Kitchen', 'Parking', 'Security Guard', 'Furnished', 'Air Conditioning');

  INSERT INTO listing_amenity (listing_id, amenity_id)
  SELECT listing4_id, id FROM amenity
  WHERE name IN ('Electricity', 'Running Water', 'Shared Kitchen', 'Fan',
                 'Security Guard', 'Furnished');

  INSERT INTO listing_amenity (listing_id, amenity_id)
  SELECT listing5_id, id FROM amenity
  WHERE name IN ('WiFi', 'Electricity', 'Running Water', 'Private Shower', 'Fan');

  INSERT INTO booking (listing_id, student_id, start_date, end_date, contract_duration,
                       monthly_rent, total_amount, status, landlord_response_date, created_at)
  VALUES (listing1_id, student1_id, '2026-06-01', '2027-04-01', 10,
          65000, 650000, 'CONFIRMED', NOW() - INTERVAL '2 days', NOW() - INTERVAL '3 days');

  INSERT INTO booking (listing_id, student_id, start_date, end_date, contract_duration,
                       monthly_rent, total_amount, status, created_at)
  VALUES (listing2_id, student2_id, '2026-07-01', '2026-10-01', 3,
          40000, 120000, 'PENDING', NOW() - INTERVAL '1 day');

END $$;
