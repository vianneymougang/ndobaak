CREATE TYPE payment_channel AS ENUM ('ORANGE_MONEY', 'MTN_MOMO', 'CARD', 'USSD', 'QR_CODE');

ALTER TABLE payment
    ALTER COLUMN operator TYPE payment_channel
    USING operator::text::payment_channel;

ALTER TABLE payment RENAME COLUMN operator TO channel;

ALTER TYPE mobile_money_operator RENAME TO mobile_money_operator_deprecated;
