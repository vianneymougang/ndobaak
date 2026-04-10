ALTER TABLE users
    ADD COLUMN mobile_money_phone    VARCHAR(20)  NULL,
    ADD COLUMN notch_pay_account_id  VARCHAR(100) NULL;

COMMENT ON COLUMN users.mobile_money_phone   IS 'Numéro Mobile Money pour virements — collecté à l''onboarding';
COMMENT ON COLUMN users.notch_pay_account_id IS 'ID compte connecté NotchPay Sync — Phase 2 uniquement';
