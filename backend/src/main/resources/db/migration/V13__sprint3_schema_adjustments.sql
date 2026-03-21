ALTER TYPE subscription_status ADD VALUE IF NOT EXISTS 'PENDING_PAYMENT';

ALTER TYPE notification_type ADD VALUE IF NOT EXISTS 'SUBSCRIPTION_ACTIVATED';
ALTER TYPE notification_type ADD VALUE IF NOT EXISTS 'SUBSCRIPTION_EXPIRING_SOON';
ALTER TYPE notification_type ADD VALUE IF NOT EXISTS 'SUBSCRIPTION_EXPIRED';
ALTER TYPE notification_type ADD VALUE IF NOT EXISTS 'REVIEW_FLAGGED';
ALTER TYPE notification_type ADD VALUE IF NOT EXISTS 'ACCOUNT_DEACTIVATED';
ALTER TYPE notification_type ADD VALUE IF NOT EXISTS 'USER_VERIFIED';
ALTER TYPE notification_type ADD VALUE IF NOT EXISTS 'USER_REJECTED';

ALTER TABLE subscription ADD COLUMN IF NOT EXISTS auto_renew BOOLEAN DEFAULT FALSE;
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS max_listings INTEGER DEFAULT 20;
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE payment ALTER COLUMN booking_id DROP NOT NULL;
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS last_payment_id BIGINT REFERENCES payment(id);
UPDATE subscription SET created_at = NOW() WHERE created_at IS NULL;
ALTER TABLE subscription ALTER COLUMN created_at SET DEFAULT NOW();

ALTER TABLE users ADD COLUMN IF NOT EXISTS deactivated BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deactivation_reason VARCHAR(500);

ALTER TABLE report ADD COLUMN IF NOT EXISTS resolved_by_id BIGINT REFERENCES users(id);
ALTER TABLE report ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS notification_type_preference (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type notification_type NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(user_id, notification_type)
);
CREATE INDEX IF NOT EXISTS idx_notif_type_pref_user ON notification_type_preference(user_id);

CREATE TABLE IF NOT EXISTS listing_view (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES listing(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id),
    viewer_ip VARCHAR(45),
    viewed_at DATE NOT NULL DEFAULT CURRENT_DATE
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_listing_view_user_day
    ON listing_view(listing_id, user_id, viewed_at)
    WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_listing_view_ip_day
    ON listing_view(listing_id, viewer_ip, viewed_at)
    WHERE viewer_ip IS NOT NULL AND user_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_listing_view_listing ON listing_view(listing_id);
CREATE INDEX IF NOT EXISTS idx_listing_view_date ON listing_view(viewed_at);
