-- Migration: Notification module (read APIs + device token registration)
-- Run this script manually on Supabase (psql / SQL editor) before
-- deploying the backend code that consumes these tables.
-- Safe to re-run: each statement is guarded with IF NOT EXISTS where possible.

-- =========================================================================
-- 1) notifications
--    Broadcast notice shown to BOTH anonymous visitors (welcome page) and
--    authenticated users (personal page). No per-user recipient list.
-- =========================================================================
CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL    PRIMARY KEY,
    category        VARCHAR(50)  NOT NULL,
    title           VARCHAR(255) NOT NULL,
    summary         VARCHAR(500),
    content         TEXT,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    published_at    TIMESTAMP    NOT NULL,
    created_by      BIGINT       NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notifications_created_by
        FOREIGN KEY (created_by) REFERENCES users (user_id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_notifications_active_published_at
    ON notifications (is_active, published_at DESC);

COMMENT ON TABLE  notifications                       IS 'Broadcast notices shown to all users (and guests on welcome page).';
COMMENT ON COLUMN notifications.category              IS 'Enum: THONG_TIN | CHINH_SACH | CANH_BAO | BAO_TRI';
COMMENT ON COLUMN notifications.summary               IS 'Short description shown in the list view.';
COMMENT ON COLUMN notifications.content               IS 'Full body shown on the detail page ("Read more").';
COMMENT ON COLUMN notifications.is_active            IS 'Soft visibility flag (admin can hide without deleting).';
COMMENT ON COLUMN notifications.published_at          IS 'When the notice was made public to users.';
COMMENT ON COLUMN notifications.created_by            IS 'users.user_id of the admin/staff who authored the notice. Nullable so the table can be seeded before the create API exists.';

-- =========================================================================
-- 2) device_tokens
--    FCM / APNS / Web push tokens. Persisted now so we can wire push
--    delivery later without a second schema change. Not yet consumed by
--    any service.
-- =========================================================================
CREATE TABLE IF NOT EXISTS device_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(500) NOT NULL UNIQUE,
    platform    VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_device_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_device_tokens_user_id
    ON device_tokens (user_id);

COMMENT ON TABLE  device_tokens            IS 'Push notification tokens per user/device. Not yet used for sending.';
COMMENT ON COLUMN device_tokens.platform   IS 'Enum: WEB | ANDROID | IOS';
COMMENT ON COLUMN device_tokens.token      IS 'FCM/APNS/Web push token. Unique so a re-register from the same device updates the row in place.';
