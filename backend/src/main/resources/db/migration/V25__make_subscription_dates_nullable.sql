ALTER TABLE subscription
    ALTER COLUMN start_date DROP NOT NULL,
    ALTER COLUMN end_date   DROP NOT NULL;
