DO $$
DECLARE
  test_user_ids   BIGINT[];
  test_booking_ids BIGINT[];
  test_listing_ids BIGINT[];
  test_location_ids BIGINT[];
BEGIN

  SELECT ARRAY_AGG(id) INTO test_user_ids
  FROM users
  WHERE email LIKE '%@test.ndobaak.com';

  IF test_user_ids IS NULL THEN
    RETURN;
  END IF;

  SELECT ARRAY_AGG(id) INTO test_listing_ids
  FROM listing
  WHERE landlord_id = ANY(test_user_ids);

  SELECT ARRAY_AGG(id) INTO test_booking_ids
  FROM booking
  WHERE student_id = ANY(test_user_ids)
     OR listing_id = ANY(test_listing_ids);

  SELECT ARRAY_AGG(location_id) INTO test_location_ids
  FROM listing
  WHERE id = ANY(test_listing_ids);

  IF test_booking_ids IS NOT NULL THEN
    DELETE FROM inspection_photo
    WHERE inspection_id IN (
      SELECT id FROM property_inspection WHERE booking_id = ANY(test_booking_ids)
    );

    DELETE FROM inspection_item
    WHERE inspection_id IN (
      SELECT id FROM property_inspection WHERE booking_id = ANY(test_booking_ids)
    );

    DELETE FROM key_handover WHERE booking_id = ANY(test_booking_ids);
    DELETE FROM property_inspection WHERE booking_id = ANY(test_booking_ids);

    DELETE FROM lease_contract WHERE booking_id = ANY(test_booking_ids);

    DELETE FROM tenant_document
    WHERE application_id IN (
      SELECT id FROM tenant_application WHERE booking_id = ANY(test_booking_ids)
    );
    DELETE FROM guarantor
    WHERE application_id IN (
      SELECT id FROM tenant_application WHERE booking_id = ANY(test_booking_ids)
    );
    DELETE FROM tenant_application WHERE booking_id = ANY(test_booking_ids);

    DELETE FROM installment
    WHERE payment_schedule_id IN (
      SELECT id FROM payment_schedule WHERE booking_id = ANY(test_booking_ids)
    );
    DELETE FROM payment_schedule WHERE booking_id = ANY(test_booking_ids);
    DELETE FROM payment WHERE booking_id = ANY(test_booking_ids);

    DELETE FROM review WHERE booking_id = ANY(test_booking_ids);

    DELETE FROM booking WHERE id = ANY(test_booking_ids);
  END IF;

  IF test_listing_ids IS NOT NULL THEN
    DELETE FROM listing_amenity WHERE listing_id = ANY(test_listing_ids);
    DELETE FROM listing WHERE id = ANY(test_listing_ids);
  END IF;

  IF test_location_ids IS NOT NULL THEN
    DELETE FROM location
    WHERE id = ANY(test_location_ids)
      AND id NOT IN (SELECT location_id FROM listing WHERE location_id IS NOT NULL);
  END IF;

  DELETE FROM notification WHERE recipient_id = ANY(test_user_ids);
  DELETE FROM users WHERE id = ANY(test_user_ids);

END $$;
